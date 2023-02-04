package ru.yandex.vertis.push.parsing

import org.scalatest.{Matchers, WordSpecLike}
import org.slf4j.LoggerFactory
import ru.yandex.vertis.push.model.LogMessage

/**
  * @author @logab
  */
trait LogParserSpecBase
  extends WordSpecLike
    with Matchers {

  def parser: LogParser

  val log = LoggerFactory.getLogger(classOf[LogParserSpecBase])

  val container = "e4f582db-6d11-11cb-cb9b-3887ff34776a"

  "Stdout LogMessageReader" should {

    val service = "test-fluentd"
    val message = "2017-06-06T12:36:48.191+0300: 57840.746: Total time for which application threads were stopped: 0.0002158 seconds, Stopping threads took: 0.0000305 seconds"

    "mark unparsed" in {
      val json =
        s"""
           |{
           | "type": "log",
           | "source": "/var/lib/nomad/alloc/$container/alloc/logs/$service.stdout.89",
           | "offset": 7290404,
           | "message": "$message",
           | "hostname": "docker-exp-15-sas.test.vertis.yandex.net",
           | "env": "test",
           | "@timestamp": "2017-07-21T09:10:48.353Z"
           | }""".stripMargin
      val logMessage = parser.parseMessage(json).get
      logMessage shouldEqual LogMessage("test/" + service, container, None, message)
    }

    "mark parsed" in {
      val json =
        s"""
           |{
           | "type": "log",
           | "source": "/var/lib/nomad/alloc/$container/alloc/logs/$service.stdout.89",
           | "offset": 7290404,
           | "message": "@@ main $message",
           | "hostname": "docker-exp-15-sas.test.vertis.yandex.net",
           | "env": "test",
           | "@timestamp": "2017-07-21T09:10:48.353Z"
           | }""".stripMargin
      val logMessage = parser.parseMessage(json).get
      logMessage shouldEqual LogMessage("test/" + service, container, Some("main.log"), message)
    }
  }

  "File LogMessageReader" should {

    val service = "realty3"
    val logFile = "realty3-api-http-access"
    val message = "[Thu Nov 09 11:55:21 2017]      /offerSearch?category=APARTMENT&remoteIp=127.0.0.1&selectedRegionId=213&type=SELL       19      OK      34995   127.0.0.1               0       213     SELL    587795"

    "read record" in {
      val json =
        s"""
           |{
           | "type": "log",
           | "source": "/var/lib/nomad/alloc/$container/alloc/logs/$service/$logFile.log.shell",
           | "offset": 7290404,
           | "message": "$message",
           | "hostname": "docker-exp-15-sas.test.vertis.yandex.net",
           | "env": "test",
           | "@timestamp": "2017-07-21T09:10:48.353Z"
           | }""".stripMargin
      val logMessage = parser.parseMessage(json).get
      logMessage shouldEqual LogMessage("test/" + service, container, Some(s"$logFile.log.shell"), message)
    }

    "read new format" in {
      val json = """{
        "@timestamp": "2021-07-07T11:48:35.463Z",
        "@metadata": {
          "beat": "filebeat",
          "type": "_doc",
          "version": "7.13.2"
        },
        "hostname": "docker-02-sas.test.vertis.yandex.net",
        "log": {
          "file": {
          "path": "/var/lib/nomad/alloc/2cfe6c64-2d63-bc70-d581-0758db80ea15/alloc/logs/chat/chat-api.log"
        },
          "offset": 8183742
        },
        "message": "[2021-07-07 14:48:34,255] INFO  [pool-4-thread-1] r.y.v.chat.util.http.ApacheHttpClient    f8de51a8e670bc0313ba980429408b76 Completed request chat/invalidate_cache: HTTP/1.1 200 OK in 1 ms.",
        "env": "test"
      }"""
      val logMessage = parser.parseMessage(json).get
      logMessage shouldEqual LogMessage("test/chat", "2cfe6c64-2d63-bc70-d581-0758db80ea15", Some("chat-api.log"),
        "[2021-07-07 14:48:34,255] INFO  [pool-4-thread-1] r.y.v.chat.util.http.ApacheHttpClient    " +
          "f8de51a8e670bc0313ba980429408b76 Completed request chat/invalidate_cache: HTTP/1.1 200 OK in 1 ms.")
    }
  }
}
