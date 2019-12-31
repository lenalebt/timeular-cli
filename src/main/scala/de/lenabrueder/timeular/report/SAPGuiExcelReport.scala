package de.lenabrueder.timeular.report

import java.io.ByteArrayOutputStream
import java.time.DayOfWeek
import java.time.LocalTime

import com.typesafe.scalalogging.StrictLogging
import de.lenabrueder.timeular.api.TimeEntry
import de.lenabrueder.timeular.filters.Filters
import org.apache.poi.hssf.usermodel.HSSFWorkbook

import scala.concurrent.duration._

/**
  * Exports to Excel in the format the SAP GUI wants to see
  */
class SAPGuiExcelReport(val filters: Filters) extends Report[Array[Byte]] with StrictLogging {
  import filters._

  def lunchBreakTimeForWorkTime(workTime: Duration): Duration = workTime match {
    case _ if workTime < 6.hours  => 0.minutes
    case _ if workTime < 9.hours  => 30.minutes
    case _ if workTime < 10.hours => 45.minutes
    case _                        => 0.minutes //in this case you need to handle it manually, more than 10 hours are not allowed :)
  }

  def roundWorkingTime(time: Duration): Duration = {
    //+7 is for rounding because it'd round down in all cases without it
    (((time.toMinutes + 7) / 15) * 15).minutes
  }

  val days = DayOfWeek.values().toSeq.map(_.toString)
  def formatDuration(d: Duration): String = {
    val hours   = d.toHours
    val minutes = (d - hours.hours).toMinutes
    f"$hours:$minutes%02d"
  }

  def create(entries: Seq[TimeEntry]) = {
    val workbook = new HSSFWorkbook()
    val groups   = groupedByWeek(entries).view.mapValues(groupedByDay).toMap
    for { (week, groupedByWeek) <- groups } {
      val sortedDaysInWeek = groupedByWeek.keySet.toSeq.sorted
      val firstDayOfWeek   = sortedDaysInWeek.head.minusDays(sortedDaysInWeek.head.getDayOfWeek.ordinal())
      val sheet            = workbook.createSheet(week.toString.replace('/', '|'))

      //set titles
      val titleRow       = sheet.createRow(0)
      val descriptionRow = sheet.createRow(1)
      for { (day, dayNumber) <- days.zipWithIndex } {
        val anchor = dayNumber * 3
        titleRow.createCell(anchor).setCellValue(s"$day, ${firstDayOfWeek.plusDays(dayNumber)}")
        descriptionRow.createCell(anchor).setCellValue("duration")
        descriptionRow.createCell(anchor + 1).setCellValue("from")
        descriptionRow.createCell(anchor + 2).setCellValue("until")
      }

      val attendanceTimeRow = sheet.createRow(2)
      val workTimeRow       = sheet.createRow(3)
      for { day <- sortedDaysInWeek } {
        val entries        = groupedByWeek(day)
        val dayOfWeek      = day.getDayOfWeek
        val workTime       = roundWorkingTime(timeWorked(entries))
        val lunchBreakTime = lunchBreakTimeForWorkTime(workTime)
        val attendanceTime = workTime + lunchBreakTime

        val topLeftCellXCoord = dayOfWeek.ordinal() * 3
        val startTime         = LocalTime.of(8, 0)
        val endTime           = startTime.plus(java.time.Duration.ofMinutes(attendanceTime.toMinutes))

        attendanceTimeRow.createCell(topLeftCellXCoord).setCellValue(formatDuration(attendanceTime))
        attendanceTimeRow.createCell(topLeftCellXCoord + 1).setCellValue(startTime.toString)
        attendanceTimeRow.createCell(topLeftCellXCoord + 2).setCellValue(endTime.toString)

        workTimeRow.createCell(topLeftCellXCoord).setCellValue(formatDuration(workTime))
      }
    }
    val output = new ByteArrayOutputStream()
    workbook.write(output)
    output.toByteArray
  }
}
