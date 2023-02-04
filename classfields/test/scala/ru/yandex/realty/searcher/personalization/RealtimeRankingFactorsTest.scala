package ru.yandex.realty.searcher.personalization

import com.google.protobuf.ByteString
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FlatSpec, Matchers}
import ru.yandex.proto.crypta.Profile
import ru.yandex.proto.crypta.EKeywordId._
import ru.yandex.realty.search.common.request.domain.SearchQuery

class RealtimeRankingFactorsTest extends FlatSpec with Matchers with ScalaFutures {

  "testExtractBigBDSSMVector" should "work correctly with null SearchQuery" in {
    Array.empty[Float] sameElements RealtimeRankingFactors.extractBigBDSSMVector(null)
  }

  "testExtractBigBDSSMVector" should "work correctly with empty SearchQuery" in {
    val sq = new SearchQuery

    Array.empty[Float] sameElements RealtimeRankingFactors.extractBigBDSSMVector(sq)
  }

  "testExtractBigBDSSMVector" should "work correctly with broken vector" in {

    val item = Profile.ProfileItem
      .newBuilder()
      .setStringValue(ByteString.copyFromUtf8("asdasdsa"))
      .setKeywordId(KI_REALTY_VECTOR_VALUE)
      .build()
    val profile = Profile
      .newBuilder()
      .addItems(item)
      .build()

    val sq = new SearchQuery
    sq.setBigbResponse(profile)

    Array.empty[Float] sameElements RealtimeRankingFactors.extractBigBDSSMVector(sq)
  }

  "testExtractBigBDSSMVector" should "extract correct embedding" in {

    val embedding = Array(4.737635695282926e-30, 2.0566220601536678e-11, 2.2212994969734043e-26,
      -1.3525592972019052e-11, 7.362552073108433e-27, -9.043182869206134e-13, 4.040623968974149e-30,
      -2.2472436801490197e-23, 1.0159852057989912e-35, 0.1292603611946106, -0.21023112535476685, -0.21821841597557068,
      -1.7511501141483424e-37, 9.477285792917059e-29, 4.933576117083227e-11, 8.851876627502674e-22,
      1.1571365512593986e-25, 0.23874402046203613, 7.548162615778065e-12, -4.652925797239054e-27, -0.47411590814590454,
      4.493781648878814e-13, 0.10707609355449677, -2.0001306084665075e-29, 1.3888936018197867e-21, 0.05095847323536873,
      -0.09314817190170288, -1.1021195346323852e-20, -0.256246954202652, 0.4253777265548706, -1.7228325274383959e-25,
      6.818653833669769e-25, 0.10168307274580002, -0.11640229821205139, -1.3853040893227475e-12,
      -1.0670650145613933e-37, 2.6732395051916324e-13, -0.12491969764232635, 4.263347798850438e-31, 0.5534074306488037,
      -0.24134795367717743, -1.0486112710572301e-13, -0.10117550939321518, 0.06525719165802002, 1.50101682878975e-17,
      -1.0408315420057007e-19, 4.075067540354471e-12, 1.8990486808957707e-13)

    val item = Profile.ProfileItem
      .newBuilder()
//      .setStringValue(ByteString.copyFromUtf8(embedding.map(_.toString).mkString(";")))
//      .setKeywordId(KI_REALTY_VECTOR_VALUE)
      .build()
    val profile = Profile
      .newBuilder()
      .addItems(item)
      .build()

    val sq = new SearchQuery
    sq.setBigbResponse(profile)

    embedding sameElements RealtimeRankingFactors.extractBigBDSSMVector(sq)
  }

}
