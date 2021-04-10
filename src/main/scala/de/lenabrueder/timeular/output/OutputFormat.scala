package de.lenabrueder.timeular.output

import cats.Show
import cats.implicits._
import de.lenabrueder.timeular.report.SAPGuiExcelReport
import de.lenabrueder.timeular.report.TextReport
import de.lenabrueder.timeular.api.TimeEntry
import de.lenabrueder.timeular.cli.Config
import de.lenabrueder.timeular.filters.Filters
import de.lenabrueder.timeular.output.CsvFormat.PreparedCsv
import de.lenabrueder.timeular.output.CsvFormat.prepareCsv
import io.circe.Encoder
import io.circe.Json
import io.circe.yaml._
import org.apache.poi.hssf.usermodel.HSSFWorkbook
import scala.concurrent.duration._

sealed trait OutputFormat[OutputFormat, EncoderFormat[_]] {
  def apply[T](t: T)(implicit encoderFormat: EncoderFormat[T]): OutputFormat
}
object OutputFormat {
  def apply[T](
      data: T
  )(
      implicit config: Config,
      encoder: Encoder[T],
      show: Show[T]
  ): Either[Array[Byte], String] =
    config.outputOptions.`type`.toLowerCase match {
      case "json"         => Right(JsonFormat.apply(data))
      case "yaml" | "yml" => Right(YamlFormat.apply(data))
      case "xls"          => Left(XlsFormat(config.outputOptions.options).apply(data))
      case "csv"          => Right(CsvFormat.apply(data))
      case "text"         => Right(TextFormat(config.outputOptions.options).apply(data))
    }
}

object XlsSapCatsReportFormat extends OutputFormat[Array[Byte], Encoder] {
  override def apply[T](t: T)(implicit identity: Encoder[T]): Array[Byte] = t match {
    case seq: Seq[Any] =>
      seq.headOption match {
        case Some(_: TimeEntry) =>
          new SAPGuiExcelReport(new Filters(8.hours)).create(seq.asInstanceOf[Seq[TimeEntry]])
        case _ => throw new IllegalArgumentException("cannot process this with SAP-CATS-Export!")
      }
    case _ => throw new IllegalArgumentException("cannot process this with SAP-CATS-Export!")
  }
}

class XlsFormat extends OutputFormat[Array[Byte], Encoder] {
  override def apply[T](t: T)(implicit encoderFormat: Encoder[T]): Array[Byte] = {
    val PreparedCsv(keys, data) = prepareCsv(t, encoderFormat)

    val workbook = new HSSFWorkbook()
    val sheet    = workbook.createSheet("data")
    val titleRow = sheet.createRow(0)
    for { (key, index) <- keys.zipWithIndex } {
      titleRow.createCell(index).setCellValue(key)
    }
    for {
      (dataLine, lineIndex)   <- data.zipWithIndex
      dataRow                 = sheet.createRow(lineIndex + 1)
      (element, elementIndex) <- dataLine.zipWithIndex
    } {
      val cell = dataRow.createCell(elementIndex)
      element.asBoolean.foreach(cell.setCellValue)
      element.asNumber.foreach(it => cell.setCellValue(it.toDouble))
      element.asString.foreach(cell.setCellValue)
    }
    keys.indices.foreach(sheet.autoSizeColumn)
    workbook.getBytes
  }
}
object XlsFormat {
  def apply(options: Map[String, String]): OutputFormat[Array[Byte], Encoder] = {
    options.get("report") match {
      case Some("sap" | "sap-cats" | "true") => XlsSapCatsReportFormat
      case None | _                          => new XlsFormat
    }
  }
}

object CsvFormat extends OutputFormat[String, Encoder] {
  override def apply[T](t: T)(implicit encoder: Encoder[T]): String = {
    val PreparedCsv(keys, data) = prepareCsv(t, encoder)
    val sb                      = new StringBuilder()
    sb.append(keys.mkString(","))
    sb.append('\n')
    for (element <- data) {
      sb.append(element.map(json => if (json.isNull) "" else json.toString()).mkString(","))
      sb.append('\n')
    }
    sb.toString()
  }

  case class PreparedCsv(keys: Seq[String], data: Seq[Seq[Json]])
  def prepareCsv[T](t: T, encoder: Encoder[T]): PreparedCsv = {
    def flattenObject(keyName: Option[String], json: Json): Seq[(String, Json)] = {
      def optKey(str: String) = keyName.map(x => s"$x.$str").getOrElse(str).some

      if (json.isObject) {
        val result = json.asObject.get.toMap.flatMap { case (key, value) => flattenObject(optKey(key), value) }
        result.toSeq
      } else if (json.isArray) {
        json.asArray.get.zipWithIndex.flatMap { case (value, index) => flattenObject(optKey(s"$index"), value) }
      } else {
        Seq(keyName.getOrElse("none") -> json)
      }
    }

    val traversedJson: Seq[Map[String, Json]] = encoder.apply(t) match {
      case array if array.isArray => array.asArray.get.map(flattenObject(None, _).toMap)
      case obj if obj.isObject =>
        Seq(obj.asObject.get.toMap.flatMap { case (key, value) => flattenObject(key.some, value) })
    }

    val keys = traversedJson.flatMap(_.keySet).toSet.toList.sorted
    val data = traversedJson.map(oneMap => keys.map(key => oneMap.getOrElse(key, Json.Null)))
    PreparedCsv(keys, data)
  }
}
object JsonFormat extends OutputFormat[String, Encoder] {
  override def apply[T](t: T)(implicit encoder: Encoder[T]): String = encoder.apply(t).toString()
}
object YamlFormat extends OutputFormat[String, Encoder] {
  override def apply[T](t: T)(implicit encoder: Encoder[T]): String =
    io.circe.yaml.Printer(dropNullKeys = true, mappingStyle = Printer.FlowStyle.Block).pretty(encoder.apply(t))
}

class TextFormat extends OutputFormat[String, Show] {
  override def apply[T](t: T)(implicit show: Show[T]): String = show.show(t)
}
object TextFormatReport extends OutputFormat[String, Show] {
  override def apply[T](t: T)(implicit encoderFormat: Show[T]): String = t match {
    case seq: Seq[Any] =>
      seq.headOption match {
        case Some(_: TimeEntry) => new TextReport(new Filters(8.hours)).create(seq.asInstanceOf[Seq[TimeEntry]])
        case _                  => throw new IllegalArgumentException("cannot process this with text report!")
      }
    case _ => throw new IllegalArgumentException("cannot process this with text report!")
  }
}
object TextFormat {
  def apply(options: Map[String, String]): OutputFormat[String, Show] = {
    options.get("report") match {
      case Some("true") => TextFormatReport
      case None | _     => new TextFormat
    }
  }
}
