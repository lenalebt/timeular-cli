package de.lenabrueder.timeular.filters

import java.time.temporal.IsoFields
import java.time.temporal.WeekFields
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime

import de.lenabrueder.timeular.api.TimeEntry

import scala.concurrent.duration._

/**
  * Contains filters that can be applied to lists of entries.
  *
  * The config parameters should go to a config file some time to allow for very generic reports.
  */
class Filters(val dailyWorkTarget: Duration) {
  val weekDays = Seq(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY)
  val workDays = weekDays

  def isWeekday(localDate: LocalDate): Boolean =
    weekDays.contains(localDate.getDayOfWeek)

  //TODO: this function should be injected, since it can be quite complicated (e.g.: holidays or work shifts)
  def isWorkday(localDate: LocalDate): Boolean =
    weekDays.contains(localDate.getDayOfWeek)

  def thisWeek(localDate: LocalDate): Boolean = {
    val now = LocalDate.now
    (now.get(IsoFields.WEEK_BASED_YEAR) == localDate.get(IsoFields.WEEK_BASED_YEAR)) &&
    (now.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR) == localDate.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR))
  }

  def thisWeek(entries: Seq[TimeEntry]): Seq[TimeEntry] =
    entries.filter(el => thisWeek(el.duration.startedAt.toLocalDate))

  def thisMonth(localDate: LocalDate): Boolean =
    LocalDate.now.withDayOfMonth(1).isBefore(localDate) &&
      LocalDate.now.withDayOfMonth(1).plusMonths(1).isAfter(localDate)

  def thisMonth(entries: Seq[TimeEntry]): Seq[TimeEntry] =
    entries.filter(el => thisMonth(el.duration.startedAt.toLocalDate))

  def lastMonth(localDate: LocalDate): Boolean =
    LocalDate.now.withDayOfMonth(1).minusMonths(1).isBefore(localDate) &&
      LocalDate.now.withDayOfMonth(1).isAfter(localDate)

  def lastMonth(entries: Seq[TimeEntry]): Seq[TimeEntry] =
    entries.filter(el => lastMonth(el.duration.startedAt.toLocalDate))

  def today(localDate: LocalDate): Boolean = LocalDate.now.equals(localDate)

  def today(entries: Seq[TimeEntry]): Seq[TimeEntry] =
    entries.filter(el => today(el.duration.startedAt.toLocalDate))

  def daysAgo(days: Int, localDate: LocalDate): Boolean =
    LocalDate.now.minusDays(days).equals(localDate)

  def daysAgo(days: Int)(entries: Seq[TimeEntry]): Seq[TimeEntry] =
    entries.filter(el => daysAgo(days, el.duration.startedAt.toLocalDate))

  def overtime(entries: Seq[TimeEntry]): Duration = {
    val daysWorked = entries.map(_.duration.startedAt.toLocalDate).distinct

    //This one accommodates for days where you maybe work an hour, but it is e.g. a weekend.
    //This hour will not be forgotten, but it will not count that day as a regular work day either.
    //This does not work for holidays, but it does for an occasional hour of work on the weekend.
    val targetTime = daysWorked.count(isWorkday) * dailyWorkTarget
    val workedTime = timeWorked(entries)
    workedTime - targetTime
  }

  def daysWorked(entries: Seq[TimeEntry]): Int =
    entries.map(_.duration.startedAt.toLocalDate).distinct.size

  def weekDaysWorked(entries: Seq[TimeEntry]): Int =
    entries.map(_.duration.startedAt.toLocalDate).distinct.count(isWeekday)

  def timeWorked(entries: Seq[TimeEntry]): FiniteDuration =
    entries
      .map(_.duration.asJavaDuration)
      .fold(java.time.Duration.ZERO) {
        _.plus(_)
      }
      .toMinutes
      .minutes

  implicit val localDateOrdering: Ordering[LocalDate]         = _.compareTo(_)
  implicit val localDateTimeOrdering: Ordering[LocalDateTime] = _.compareTo(_)
  def groupedByDay(entries: Seq[TimeEntry]): Map[LocalDate, Seq[TimeEntry]] =
    entries.groupBy(_.duration.startedAt.toLocalDate).view.mapValues(_.sortBy(_.duration.startedAt)).toMap

  case class Week(year: Int, week: Int) {
    override def toString: String = s"$year/$week"
  }
  object Week {
    def apply(date: LocalDate): Week = Week(date.getYear, date.get(WeekFields.ISO.weekOfWeekBasedYear()))
  }
  def groupedByWeek(entries: Seq[TimeEntry]): Map[Week, Seq[TimeEntry]] =
    entries
      .groupBy(key => Week(key.duration.startedAt.toLocalDate))
      .view
      .mapValues(_.sortBy(_.duration.startedAt))
      .toMap
}
