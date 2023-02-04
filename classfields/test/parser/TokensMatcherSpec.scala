package ru.yandex.vertis.general.wizard.meta.parser

import zio.test.Assertion.equalTo
import zio.test._
import common.text.query.QueryString
import ru.yandex.vertis.general.wizard.meta.utils.Parser

object TokensMatcherSpec extends DefaultRunnableSpec {

  sealed trait Entry
  case object Commander extends Entry
  case object CommanderRegiment extends Entry
  case object RegimentCommander extends Entry
  case object GeneralRegiment extends Entry
  case object Regiment extends Entry
  case object ElderlyCommander extends Entry
  case object Other extends Entry
  case object Was extends Entry
  case object Pine extends Entry
  case object Buy extends Entry

  private val queryStringOne = QueryString("The commander regiment of the was an elderly")
  private val commanderSynonym = Seq("general" -> 1)

  private val userDictOne = Parser.userRequestDictionary(queryStringOne.tokens.toSeq.zipWithIndex, commanderSynonym)

  private val queryStringSome = QueryString("The commander of the regiment elderly was a bustard")
  private val userDictSome = Parser.userRequestDictionary(queryStringSome.tokens.toSeq.zipWithIndex, Seq.empty)

  private val dictOne: Map[String, Seq[Entry]] = Map(
    "commander" -> Seq(Commander),
    "elderly commander" -> Seq(ElderlyCommander),
    "commander of the regiment advanced" -> Seq(CommanderRegiment, RegimentCommander),
    "commander of the regiment" -> Seq(CommanderRegiment, RegimentCommander),
    "general of the regiment" -> Seq(GeneralRegiment),
    "regiment" -> Seq(Regiment),
    "other" -> Seq(Other),
    "was" -> Seq(Was)
  )

  private val dictSome: Map[String, Seq[Entry]] = Map(
    "elderly" -> Seq(ElderlyCommander),
    "commander of the regiment" -> Seq(RegimentCommander),
    "regiment" -> Seq(Regiment),
    "was" -> Seq(Was)
  )

  private val dictWithIo: Map[String, Seq[Entry]] = Map(
    "ёлка" -> Seq(Pine),
    "купить" -> Seq(Buy)
  )

  private val pDictOne = TokensMatcher.transformToTokensDictionary(dictOne)
  private val pDictSome = TokensMatcher.transformToTokensDictionary(dictSome)

  private val expectedOne: Seq[TokensMatch[Entry]] = Seq(
    TokensMatch(Set(1), Commander, usedTokenSynonyms = false),
    TokensMatch(Set(1, 7), ElderlyCommander, usedTokenSynonyms = false),
    TokensMatch(Set(5), Was, usedTokenSynonyms = false),
    TokensMatch(Set(2), Regiment, usedTokenSynonyms = false),
    TokensMatch(Set(0, 1, 2, 3), CommanderRegiment, usedTokenSynonyms = false),
    TokensMatch(Set(0, 1, 2, 3), RegimentCommander, usedTokenSynonyms = false),
    TokensMatch(Set(0, 1, 2, 3), GeneralRegiment, usedTokenSynonyms = true),
    TokensMatch(Set(1, 2, 3, 4), CommanderRegiment, usedTokenSynonyms = false),
    TokensMatch(Set(1, 2, 3, 4), RegimentCommander, usedTokenSynonyms = false),
    TokensMatch(Set(1, 2, 3, 4), GeneralRegiment, usedTokenSynonyms = true),
    TokensMatch(Set(5), Was, usedTokenSynonyms = false)
  )

  private val variant1: TokensMatches[Entry] =
    TokensMatches(
      Set(
        TokensMatch(Set(0, 1, 2, 4), RegimentCommander, usedTokenSynonyms = false),
        TokensMatch(Set(5), ElderlyCommander, usedTokenSynonyms = false),
        TokensMatch(Set(6), Was, usedTokenSynonyms = false)
      )
    )

  private val variant2: TokensMatches[Entry] =
    TokensMatches(
      Set(
        TokensMatch(Set(4), Regiment, usedTokenSynonyms = false),
        TokensMatch(Set(6), Was, usedTokenSynonyms = false),
        TokensMatch(Set(5), ElderlyCommander, usedTokenSynonyms = false)
      )
    )

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] =
    suite("TokensMatcher")(
      test("parse variants of one") {
        assert(TokensMatcher.parseOne(userDictOne, pDictOne).toSet)(equalTo(expectedOne.toSet))
      },
      test("parse variant1 of some") {
        assert(TokensMatcher.parseSome[Entry](userDictSome, pDictSome, _ => true).toSet.contains(variant1))(
          equalTo(true)
        )
      },
      test("parse variant2 of some") {
        assert(TokensMatcher.parseSome[Entry](userDictSome, pDictSome, _ => true).toSet.contains(variant2))(
          equalTo(true)
        )
      },
      test("ignore io symbols, replacing them with e") {
        assert(
          TokensMatcher
            .transformToTokensDictionary(dictWithIo)
            .toSet
            .contains("елка" -> List(TokenOfAlias(Pine, "ёлка", 1, Set())))
        )(equalTo(true))
      }
    )
}
