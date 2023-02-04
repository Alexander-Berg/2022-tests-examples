package ru.yandex.vertis.push.model

import org.scalacheck.Gen

/**
  * @author @logab
  */
object ModelGenerators {

  val LogMessageGen: Gen[LogMessage] = for {
    service <- Gen.choose(0, 10).map(_.toString).map("testing/" + _)
    instance <- Gen.const("id")
    tag <- Gen.option(Gen.oneOf("main", "unparsed", "http-access", "access", "foobar"))
    message <- Gen.alphaStr.filter(_.nonEmpty).map(_.take(10000))
  } yield LogMessage(service, instance, tag, message)

}
