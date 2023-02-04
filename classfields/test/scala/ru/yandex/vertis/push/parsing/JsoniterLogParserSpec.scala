package ru.yandex.vertis.push.parsing

/**
  * Runnable spec on [[JsoniterLogParser]]
  *
  * @author dimas
  */
class JsoniterLogParserSpec
  extends LogParserSpecBase {
  def parser: LogParser = JsoniterLogParser
}
