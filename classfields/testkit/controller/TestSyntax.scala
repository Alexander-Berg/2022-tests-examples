package ru.yandex.vertis.shark.controller

import ru.yandex.vertis.shark.model._

object TestSyntax {

  implicit class RichCheckRequirementsCompanion(val self: CheckRequirements.type) extends AnyVal {
    import ru.yandex.vertis.shark.model.CheckRequirements._

    def forTest(matching: Matching = Matched, term: Boolean = true, geo: Boolean = true) =
      self(matching = matching, term = term, geo = geo)
  }

  implicit class RichCheckBorrowerCompanion(val self: CheckBorrower.type) extends AnyVal {

    def forTest(
        employment: Boolean = true,
        age: Boolean = true,
        minLastExperienceMonths: Boolean = true,
        allExperienceMonths: Boolean = true,
        incomeAfterExpenses: Boolean = true,
        incomeWithoutExpenses: Boolean = true,
        proofs: Boolean = true,
        score: Option[CheckBorrower.CheckScore] = None) =
      self(
        employment = employment,
        age = age,
        minLastExperienceMonths = minLastExperienceMonths,
        allExperienceMonths = allExperienceMonths,
        incomeAfterExpenses = incomeAfterExpenses,
        incomeWithoutExpenses = incomeWithoutExpenses,
        proofs = proofs,
        score = score
      )
  }

  implicit class RichCheckRateLimitCompanion(val self: CheckRateLimit.type) extends AnyVal {

    def forTest(maxClaimsPer1d: Boolean = true) =
      self(maxClaimsPer1d = maxClaimsPer1d)
  }
}
