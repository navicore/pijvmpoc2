package onextent.iot.pijvmpoc2.models

sealed trait Command

case class ReadCommand() extends Command

case class TempReading(humidity: Option[Double] = None, temperature: Option[Double] = None)

case class TempReport(DevAddr: Option[String] = Some("unknown"), data: TempReading)

case class Error()

