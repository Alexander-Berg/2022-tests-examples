package vertis.palma.gen

import java.nio.charset.StandardCharsets.UTF_8

import io.circe.Encoder.encodeString
import io.circe.syntax._
import org.scalacheck.Gen
import ru.yandex.vertis.generators.ProducerProvider
import vertis.palma.dao.model.DictionaryItem.Id
import vertis.palma.dao.model.{DictionaryItem, Index, Relation, UpdateInfo}

/** @author kusaeva
  */
object ModelGenerators extends ProducerProvider {

  val StrGen: Gen[String] =
    for {
      length <- Gen.choose(5, 10)
    } yield {
      val unicodeStr = Gen.alphaNumChar.next(length).mkString
      new String(unicodeStr.getBytes(UTF_8), UTF_8)
    }

  val IdGen: Gen[Id] =
    for {
      name <- StrGen
      key <- StrGen
    } yield Id(name, key)

  val RelationGen: Gen[Relation] =
    for {
      to <- IdGen
      field <- StrGen
    } yield Relation(to, field)

  val IndexGen: Gen[Index] =
    for {
      field <- StrGen
      value <- StrGen
    } yield Index(field, value)

  val UpdateInfoGen: Gen[UpdateInfo] =
    for {
      user <- Gen.option(StrGen)
      timestamp <- Gen.calendar.map(_.toInstant)
      schemaVersion <- Gen.option(StrGen)
      version <- StrGen
    } yield UpdateInfo(user, schemaVersion, timestamp, version)

  val PlainContentGen: Gen[DictionaryItem.PlainPayload] = for {
    content <- StrGen
    jsonView <- StrGen
  } yield DictionaryItem.PlainPayload(content.getBytes, jsonView.asJson.toString)

  val EncryptedPayloadGen: Gen[DictionaryItem.EncryptedPayload] = for {
    content <- StrGen
    secret <- StrGen
  } yield DictionaryItem.EncryptedPayload(content.getBytes, secret)

  val DictionaryPayloadGen: Gen[DictionaryItem.Payload] = for {
    payload <- Gen.oneOf(EncryptedPayloadGen, PlainContentGen)
  } yield payload

  val DictionaryItemGen: Gen[DictionaryItem] =
    for {
      id <- IdGen
      payload <- DictionaryPayloadGen
      nRelation <- Gen.choose(0, 5)
      relations <- Gen.listOfN(nRelation, RelationGen)
      nIndexes <- Gen.choose(0, 5)
      indexes <- Gen.listOfN(nIndexes, IndexGen)
      lastUpdateInfo <- UpdateInfoGen
    } yield DictionaryItem(
      id,
      payload,
      relations,
      indexes,
      lastUpdateInfo
    )

}
