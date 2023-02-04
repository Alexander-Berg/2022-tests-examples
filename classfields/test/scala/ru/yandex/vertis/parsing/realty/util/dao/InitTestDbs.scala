package ru.yandex.vertis.parsing.realty.util.dao

import ru.yandex.vertis.parsing.realty.components.TestDockerParsingComponents
import ru.yandex.vertis.parsing.util.logging.Logging

/**
  * Created by andrey on 11/8/17.
  */
trait InitTestDbs extends Logging {
  val components: TestDockerParsingComponents.type = TestDockerParsingComponents

  def initDb(): Unit = {
    val schemaFilename = "/schema_base.sql"
    log.info(s"Using schema from $schemaFilename")
    val autoruSchemaFile = readSqlFile(schemaFilename)
    components.parsingRealtyShard.master.jdbc.update("SET FOREIGN_KEY_CHECKS=0;")
    autoruSchemaFile.foreach(sql => {
      //log.info(sql)
      components.parsingRealtyShard.master.jdbc.update(sql)
    })
    components.parsingRealtyShard.master.jdbc.update("SET FOREIGN_KEY_CHECKS=1;")
  }

  private def readSqlFile(name: String) = {
    scala.io.Source
      .fromURL(getClass.getResource(name))("UTF-8")
      .getLines
      .filter(s => s.trim.nonEmpty && !s.trim.startsWith("--"))
      .map(s => s.split("--").head)
      .mkString("\n")
      .split(";")
  }
}
