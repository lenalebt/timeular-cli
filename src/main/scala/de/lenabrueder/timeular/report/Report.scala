package de.lenabrueder.timeular.report

import de.lenabrueder.timeular.api.TimeEntry

/**
  * Describes a report format. Can transform time entries to whatever you are interested in.
  */
trait Report[ExportFormat] {
  def create(entries: Seq[TimeEntry]): ExportFormat
}
