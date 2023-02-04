package vertis.yt.test

import org.scalacheck.Gen
import ru.yandex.inside.yt.kosher.cypress.YPath
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTree
import ru.yandex.vertis.generators.ProducerProvider._
import vertis.yt.model.{YtSchema, YtTable}
import vertis.yt.model.attributes.YtAttribute

import java.nio.charset.StandardCharsets.UTF_8

/** @author kusaeva
  */
object Generators {

  val StrGen: Gen[String] = for {
    length <- Gen.choose(5, 10)
  } yield {
    val unicodeStr = Gen.alphaNumChar.next(length).mkString
    new String(unicodeStr.getBytes(UTF_8), UTF_8)
  }

  def ytTableGen(basePath: YPath): Gen[YtTable] = for {
    name <- StrGen
    path = basePath.child(name)
  } yield YtTable(name, path, YtSchema.Empty, None, Seq())

  val YtAttributeGen: Gen[YtAttribute] = for {
    key <- StrGen
    value <- StrGen
  } yield key -> YTree.stringNode(value)
}
