package de.lenabrueder.timeular.report
import de.lenabrueder.timeular.api.TimeEntry
import de.lenabrueder.timeular.filters.Filters

import scala.concurrent.duration._

class TextReport(val filters: Filters) extends Report[String] {
  import filters._

  def humanReadable(duration: Duration) = {
    val days      = (duration / filters.dailyWorkTarget).toLong
    var remaining = duration.minus(days * filters.dailyWorkTarget)
    val hours     = remaining.toHours
    remaining = remaining.minus(hours.hours)
    val minutes = remaining.toMinutes

    s"${days}d ${hours}h ${minutes}min"
  }

  override def create(entries: Seq[TimeEntry]): String = {
    s"""overtime today:      ${humanReadable(overtime(today(entries)))}
       |overtime this week:  ${humanReadable(overtime(thisWeek(entries)))}
       |overtime this month: ${humanReadable(overtime(thisMonth(entries)))}
       |overtime last month: ${humanReadable(overtime(lastMonth(entries)))}
       |overtime total:      ${humanReadable(overtime(entries))}""".stripMargin
  }
}
