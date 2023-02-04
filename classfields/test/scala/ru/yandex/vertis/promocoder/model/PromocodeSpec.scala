package ru.yandex.vertis.promocoder.model

import ru.yandex.vertis.promocoder.WordSpecBase
import ru.yandex.vertis.promocoder.model.gens.ModelGenerators
import ru.yandex.vertis.promocoder.util.Null
import ru.yandex.vertis.util.time.DateTimeUtil

import scala.annotation.nowarn

/** Specs on [[Promocode]] invariants
  *
  * @author alex-kovalenko
  */
class PromocodeSpec extends WordSpecBase with ModelGenerators {

  @nowarn("msg=discarded non-Unit value")
  def shouldFail(t: => Any): Unit = {
    intercept[IllegalArgumentException](t)
  }

  "Promocode" should {
    val code = "code"
    val constraints = Constraints(DateTimeUtil.now(), Int.MaxValue, Int.MaxValue)
    val features = Iterable(FeatureGen.next.copy(referring = None))

    "not accept invalid code" in {
      shouldFail(Promocode(Null, None, features, constraints))
      shouldFail(Promocode("", None, features, constraints))
    }

    "not accept empty features" in {
      shouldFail(Promocode(code, None, Iterable.empty[Feature], constraints))
    }

    "not accept features with duplicate tags" in {
      val tag = "tag"
      val features = FeatureGen.next(2).map(_.copy(tag = tag))
      shouldFail(Promocode(code, None, features, constraints))
    }

    "not accept null constraints" in {
      shouldFail(Promocode(code, None, features, Null))
    }

    "not accept referring" when {
      "features with same tag are not equal for one user" in {
        val user = "user"
        val refs = ReferringGen
          .next(2)
          .map(r => r.copy(user = user, feature = r.feature.copy(tag = s"tag")))
        val fs = FeatureGen.next(2).toList.zip(refs).map { case (f, r) =>
          f.copy(referring = Some(r))
        }
        shouldFail(Promocode(code, None, fs, constraints))
      }
    }

    "accept referring" when {
      "features with same tag are equal for one user" in {
        val user = "user"
        val refFeature = FeatureGen.next
        val refs = ReferringGen
          .next(2)
          .map(r => r.copy(user = user, feature = refFeature))
        val fs = FeatureGen.next(2).toList.zip(refs).map { case (f, r) =>
          f.copy(referring = Some(r))
        }
        info(s"features: $fs")
        Promocode(code, None, fs, constraints)
      }

      "features with same tag are not equal for different users" in {
        val user = "user"
        val refs = ReferringGen
          .next(2)
          .zipWithIndex
          .map { case (r, i) =>
            r.copy(user = s"${user}_$i", feature = r.feature.copy(tag = s"tag_$i"))
          }
        val fs = FeatureGen.next(2).toList.zip(refs).map { case (f, r) =>
          f.copy(referring = Some(r))
        }
        Promocode(code, None, fs, constraints)
      }
    }
  }
}
