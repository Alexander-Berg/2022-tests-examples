package ru.yandex.vertis.shark.converter.protobuf

import io.circe.parser._
import ru.yandex.vertis.zio_baker.scalapb_utils.ProtoJson._
import ru.yandex.vertis.zio_baker.scalapb_utils.Validation.ValidationException
import ru.yandex.vertis.shark.converter.protobuf.Implicits._
import ru.yandex.vertis.shark.model.{ConsumerCreditProduct, CreditProduct, DealerCreditProduct, DealerCreditProductStub}
import ru.yandex.vertis.zio_baker.scalapb_utils.Validation.ValidationException.FieldError
import zio.Task
import zio.test._
import zio.test.Assertion.{anything, equalTo, fails, isSubtype}

object DealerCreditProductSpec extends DefaultRunnableSpec {

  private val dealerFullCreditProduct: String =
    """
      |{
      |  "id": "dealer-1",
      |  "productType": "DEALER",
      |  "domain": "DOMAIN_AUTO",
      |  "amountRange": {
      |    "from": "50000",
      |    "to": "5000000"
      |  },
      |  "interestRateRange": {
      |    "from": 6.5,
      |    "to": 20.99
      |  },
      |  "termMonthsRange": {
      |    "from": 12,
      |    "to": 60
      |  },
      |  "minInitialFeeRate": 0,
      |  "geobaseIds": [
      |    "225"
      |  ],
      |  "creditProductPayload": {
      |    "dealer": {}
      |  },
      |  "creditApplicationInfoBlocks": [
      |    {
      |      "blockType": "CONTROL_WORD",
      |      "requiredCondition": {
      |        "always": {}
      |      }
      |    },
      |    {
      |      "blockType": "OKB_STATEMENT_AGREEMENT",
      |      "requiredCondition": {
      |        "always": {}
      |      }
      |    },
      |    {
      |      "blockType": "ADVERT_STATEMENT_AGREEMENT",
      |      "requiredCondition": {
      |        "always": {}
      |      }
      |    }
      |  ],
      |  "borrowerPersonProfileBlocks": [
      |    {
      |      "blockType": "NAME",
      |      "requiredCondition": {
      |        "always": {}
      |      }
      |    },
      |    {
      |      "blockType": "OLD_NAME",
      |      "requiredCondition": {
      |        "always": {}
      |      }
      |    },
      |    {
      |      "blockType": "GENDER",
      |      "allowedCondition": {
      |        "always": {}
      |      }
      |    },
      |    {
      |      "blockType": "PASSPORT_RF",
      |      "requiredCondition": {
      |        "always": {}
      |      }
      |    },
      |    {
      |      "blockType": "OLD_PASSPORT_RF",
      |      "requiredCondition": {
      |        "always": {}
      |      }
      |    },
      |    {
      |      "blockType": "FOREIGN_PASSPORT",
      |      "allowedCondition": {
      |        "always": {}
      |      }
      |    },
      |    {
      |      "blockType": "INSURANCE_NUMBER",
      |      "allowedCondition": {
      |        "always": {}
      |      }
      |    },
      |    {
      |      "blockType": "DRIVER_LICENSE",
      |      "allowedCondition": {
      |        "always": {}
      |      }
      |    },
      |    {
      |      "blockType": "BIRTH_DATE",
      |      "requiredCondition": {
      |        "always": {}
      |      }
      |    },
      |    {
      |      "blockType": "BIRTH_PLACE",
      |      "requiredCondition": {
      |        "always": {}
      |      }
      |    },
      |    {
      |      "blockType": "RESIDENCE_ADDRESS",
      |      "requiredCondition": {
      |        "always": {}
      |      }
      |    },
      |    {
      |      "blockType": "REGISTRATION_ADDRESS",
      |      "requiredCondition": {
      |        "always": {}
      |      }
      |    },
      |    {
      |      "blockType": "EDUCATION",
      |      "requiredCondition": {
      |        "always": {}
      |      }
      |    },
      |    {
      |      "blockType": "MARITAL_STATUS",
      |      "requiredCondition": {
      |        "always": {}
      |      }
      |    },
      |    {
      |      "blockType": "DEPENDENTS",
      |      "requiredCondition": {
      |        "always": {}
      |      }
      |    },
      |    {
      |      "blockType": "INCOME",
      |      "requiredCondition": {
      |        "always": {}
      |      }
      |    },
      |    {
      |      "blockType": "EXPENSES",
      |      "allowedCondition": {
      |        "always": {}
      |      }
      |    },
      |    {
      |      "blockType": "PROPERTY_OWNERSHIP",
      |      "requiredCondition": {
      |        "always": {}
      |      }
      |    },
      |    {
      |      "blockType": "VEHICLE_OWNERSHIP",
      |      "allowedCondition": {
      |        "always": {}
      |      }
      |    },
      |    {
      |      "blockType": "EMPLOYMENT",
      |      "requiredCondition": {
      |        "always": {}
      |      }
      |    },
      |    {
      |      "blockType": "RELATED_PERSONS",
      |      "allowedCondition": {
      |        "always": {}
      |      }
      |    },
      |    {
      |      "blockType": "PHONES",
      |      "requiredCondition": {
      |        "always": {}
      |      }
      |    },
      |    {
      |      "blockType": "EMAILS",
      |      "requiredCondition": {
      |        "always": {}
      |      }
      |    }
      |  ],
      |  "isActive": true,
      |  "borrowerConditions": {
      |    "employmentTypes": [
      |      "EMPLOYEE"
      |    ],
      |    "ageRange": {
      |      "from": 21,
      |      "to": 70
      |    },
      |    "minLastExperienceMonths": 3,
      |    "allExperienceMonths": 0,
      |    "incomeAfterExpenses": "0",
      |    "proofs": [],
      |    "incomeWithoutExpense": "10000",
      |    "checkAgeWithoutTerm": false,
      |    "exactlyRequirements": false,
      |    "geobaseIds": []
      |  },
      |  "bankId": "dealer",
      |  "clientFeatures": [
      |    "SHARK_BANK_ID_SUPPORT"
      |  ],
      |  "priority": 1,
      |  "excludedGeobaseIds": [
      |    "977"
      |  ],
      |  "specificBorrowerConditions": [],
      |  "priorityTags": [
      |    "AMOUNT_BELOW_1M"
      |  ]
      |}
      |""".stripMargin

  private val dealerShortCreditProduct: String =
    """
      |{
      |  "id": "dealer-1",
      |  "productType": "DEALER",
      |  "domain": "DOMAIN_AUTO",
      |  "creditProductPayload": {
      |    "dealer": {}
      |  },
      |  "creditApplicationInfoBlocks": [
      |    {
      |      "blockType": "CONTROL_WORD",
      |      "requiredCondition": {
      |        "always": {}
      |      }
      |    },
      |    {
      |      "blockType": "OKB_STATEMENT_AGREEMENT",
      |      "requiredCondition": {
      |        "always": {}
      |      }
      |    },
      |    {
      |      "blockType": "ADVERT_STATEMENT_AGREEMENT",
      |      "requiredCondition": {
      |        "always": {}
      |      }
      |    }
      |  ],
      |  "borrowerPersonProfileBlocks": [
      |    {
      |      "blockType": "NAME",
      |      "requiredCondition": {
      |        "always": {}
      |      }
      |    },
      |    {
      |      "blockType": "OLD_NAME",
      |      "requiredCondition": {
      |        "always": {}
      |      }
      |    },
      |    {
      |      "blockType": "GENDER",
      |      "allowedCondition": {
      |        "always": {}
      |      }
      |    },
      |    {
      |      "blockType": "PASSPORT_RF",
      |      "requiredCondition": {
      |        "always": {}
      |      }
      |    },
      |    {
      |      "blockType": "OLD_PASSPORT_RF",
      |      "requiredCondition": {
      |        "always": {}
      |      }
      |    },
      |    {
      |      "blockType": "FOREIGN_PASSPORT",
      |      "allowedCondition": {
      |        "always": {}
      |      }
      |    },
      |    {
      |      "blockType": "INSURANCE_NUMBER",
      |      "allowedCondition": {
      |        "always": {}
      |      }
      |    },
      |    {
      |      "blockType": "DRIVER_LICENSE",
      |      "allowedCondition": {
      |        "always": {}
      |      }
      |    },
      |    {
      |      "blockType": "BIRTH_DATE",
      |      "requiredCondition": {
      |        "always": {}
      |      }
      |    },
      |    {
      |      "blockType": "BIRTH_PLACE",
      |      "requiredCondition": {
      |        "always": {}
      |      }
      |    },
      |    {
      |      "blockType": "RESIDENCE_ADDRESS",
      |      "requiredCondition": {
      |        "always": {}
      |      }
      |    },
      |    {
      |      "blockType": "REGISTRATION_ADDRESS",
      |      "requiredCondition": {
      |        "always": {}
      |      }
      |    },
      |    {
      |      "blockType": "EDUCATION",
      |      "requiredCondition": {
      |        "always": {}
      |      }
      |    },
      |    {
      |      "blockType": "MARITAL_STATUS",
      |      "requiredCondition": {
      |        "always": {}
      |      }
      |    },
      |    {
      |      "blockType": "DEPENDENTS",
      |      "requiredCondition": {
      |        "always": {}
      |      }
      |    },
      |    {
      |      "blockType": "INCOME",
      |      "requiredCondition": {
      |        "always": {}
      |      }
      |    },
      |    {
      |      "blockType": "EXPENSES",
      |      "allowedCondition": {
      |        "always": {}
      |      }
      |    },
      |    {
      |      "blockType": "PROPERTY_OWNERSHIP",
      |      "requiredCondition": {
      |        "always": {}
      |      }
      |    },
      |    {
      |      "blockType": "VEHICLE_OWNERSHIP",
      |      "allowedCondition": {
      |        "always": {}
      |      }
      |    },
      |    {
      |      "blockType": "EMPLOYMENT",
      |      "requiredCondition": {
      |        "always": {}
      |      }
      |    },
      |    {
      |      "blockType": "RELATED_PERSONS",
      |      "allowedCondition": {
      |        "always": {}
      |      }
      |    },
      |    {
      |      "blockType": "PHONES",
      |      "requiredCondition": {
      |        "always": {}
      |      }
      |    },
      |    {
      |      "blockType": "EMAILS",
      |      "requiredCondition": {
      |        "always": {}
      |      }
      |    }
      |  ],
      |  "isActive": true,
      |  "borrowerConditions": {
      |    "employmentTypes": [
      |      "EMPLOYEE"
      |    ],
      |    "ageRange": {
      |      "from": 21,
      |      "to": 70
      |    },
      |    "minLastExperienceMonths": 3,
      |    "allExperienceMonths": 0,
      |    "incomeAfterExpenses": "0",
      |    "proofs": [],
      |    "incomeWithoutExpense": "10000",
      |    "checkAgeWithoutTerm": false,
      |    "exactlyRequirements": false,
      |    "geobaseIds": []
      |  },
      |  "bankId": "dealer",
      |  "clientFeatures": [
      |    "SHARK_BANK_ID_SUPPORT"
      |  ],
      |  "priority": 1,
      |  "excludedGeobaseIds": [
      |    "977"
      |  ],
      |  "specificBorrowerConditions": [],
      |  "priorityTags": [
      |    "AMOUNT_BELOW_1M"
      |  ]
      |}
      |""".stripMargin

  private val alfaBankFullCreditProduct: String =
    """
      |{
      |  "id": "dealer-1",
      |  "bankType": "ALFABANK",
      |  "productType": "CONSUMER",
      |  "domain": "DOMAIN_AUTO",
      |  "amountRange": {
      |    "from": "50000",
      |    "to": "5000000"
      |  },
      |  "interestRateRange": {
      |    "from": 6.5,
      |    "to": 20.99
      |  },
      |  "termMonthsRange": {
      |    "from": 12,
      |    "to": 60
      |  },
      |  "minInitialFeeRate": 0,
      |  "geobaseIds": [
      |    "225"
      |  ],
      |  "creditProductPayload": {
      |    "consumer": {}
      |  },
      |  "creditApplicationInfoBlocks": [
      |    {
      |      "blockType": "CONTROL_WORD",
      |      "requiredCondition": {
      |        "always": {}
      |      }
      |    },
      |    {
      |      "blockType": "OKB_STATEMENT_AGREEMENT",
      |      "requiredCondition": {
      |        "always": {}
      |      }
      |    },
      |    {
      |      "blockType": "ADVERT_STATEMENT_AGREEMENT",
      |      "requiredCondition": {
      |        "always": {}
      |      }
      |    }
      |  ],
      |  "borrowerPersonProfileBlocks": [
      |    {
      |      "blockType": "NAME",
      |      "requiredCondition": {
      |        "always": {}
      |      }
      |    },
      |    {
      |      "blockType": "OLD_NAME",
      |      "requiredCondition": {
      |        "always": {}
      |      }
      |    },
      |    {
      |      "blockType": "GENDER",
      |      "allowedCondition": {
      |        "always": {}
      |      }
      |    },
      |    {
      |      "blockType": "PASSPORT_RF",
      |      "requiredCondition": {
      |        "always": {}
      |      }
      |    },
      |    {
      |      "blockType": "OLD_PASSPORT_RF",
      |      "requiredCondition": {
      |        "always": {}
      |      }
      |    },
      |    {
      |      "blockType": "FOREIGN_PASSPORT",
      |      "allowedCondition": {
      |        "always": {}
      |      }
      |    },
      |    {
      |      "blockType": "INSURANCE_NUMBER",
      |      "allowedCondition": {
      |        "always": {}
      |      }
      |    },
      |    {
      |      "blockType": "DRIVER_LICENSE",
      |      "allowedCondition": {
      |        "always": {}
      |      }
      |    },
      |    {
      |      "blockType": "BIRTH_DATE",
      |      "requiredCondition": {
      |        "always": {}
      |      }
      |    },
      |    {
      |      "blockType": "BIRTH_PLACE",
      |      "requiredCondition": {
      |        "always": {}
      |      }
      |    },
      |    {
      |      "blockType": "RESIDENCE_ADDRESS",
      |      "requiredCondition": {
      |        "always": {}
      |      }
      |    },
      |    {
      |      "blockType": "REGISTRATION_ADDRESS",
      |      "requiredCondition": {
      |        "always": {}
      |      }
      |    },
      |    {
      |      "blockType": "EDUCATION",
      |      "requiredCondition": {
      |        "always": {}
      |      }
      |    },
      |    {
      |      "blockType": "MARITAL_STATUS",
      |      "requiredCondition": {
      |        "always": {}
      |      }
      |    },
      |    {
      |      "blockType": "DEPENDENTS",
      |      "requiredCondition": {
      |        "always": {}
      |      }
      |    },
      |    {
      |      "blockType": "INCOME",
      |      "requiredCondition": {
      |        "always": {}
      |      }
      |    },
      |    {
      |      "blockType": "EXPENSES",
      |      "allowedCondition": {
      |        "always": {}
      |      }
      |    },
      |    {
      |      "blockType": "PROPERTY_OWNERSHIP",
      |      "requiredCondition": {
      |        "always": {}
      |      }
      |    },
      |    {
      |      "blockType": "VEHICLE_OWNERSHIP",
      |      "allowedCondition": {
      |        "always": {}
      |      }
      |    },
      |    {
      |      "blockType": "EMPLOYMENT",
      |      "requiredCondition": {
      |        "always": {}
      |      }
      |    },
      |    {
      |      "blockType": "RELATED_PERSONS",
      |      "allowedCondition": {
      |        "always": {}
      |      }
      |    },
      |    {
      |      "blockType": "PHONES",
      |      "requiredCondition": {
      |        "always": {}
      |      }
      |    },
      |    {
      |      "blockType": "EMAILS",
      |      "requiredCondition": {
      |        "always": {}
      |      }
      |    }
      |  ],
      |  "isActive": true,
      |  "borrowerConditions": {
      |    "employmentTypes": [
      |      "EMPLOYEE"
      |    ],
      |    "ageRange": {
      |      "from": 21,
      |      "to": 70
      |    },
      |    "minLastExperienceMonths": 3,
      |    "allExperienceMonths": 0,
      |    "incomeAfterExpenses": "0",
      |    "proofs": [],
      |    "incomeWithoutExpense": "10000",
      |    "checkAgeWithoutTerm": false,
      |    "exactlyRequirements": false,
      |    "geobaseIds": []
      |  },
      |  "bankId": "alfabank",
      |  "clientFeatures": [
      |    "SHARK_BANK_ID_SUPPORT"
      |  ],
      |  "priority": 1,
      |  "excludedGeobaseIds": [
      |    "977"
      |  ],
      |  "specificBorrowerConditions": [],
      |  "priorityTags": [
      |    "AMOUNT_BELOW_1M"
      |  ]
      |}
      |""".stripMargin

  private val alfaBankShortCreditProduct: String =
    """
      |{
      |  "id": "dealer-1",
      |  "bankType": "ALFABANK",
      |  "productType": "CONSUMER",
      |  "domain": "DOMAIN_AUTO",
      |  "geobaseIds": [
      |    "225"
      |  ],
      |  "creditProductPayload": {
      |    "consumer": {}
      |  },
      |  "creditApplicationInfoBlocks": [
      |    {
      |      "blockType": "CONTROL_WORD",
      |      "requiredCondition": {
      |        "always": {}
      |      }
      |    },
      |    {
      |      "blockType": "OKB_STATEMENT_AGREEMENT",
      |      "requiredCondition": {
      |        "always": {}
      |      }
      |    },
      |    {
      |      "blockType": "ADVERT_STATEMENT_AGREEMENT",
      |      "requiredCondition": {
      |        "always": {}
      |      }
      |    }
      |  ],
      |  "borrowerPersonProfileBlocks": [
      |    {
      |      "blockType": "NAME",
      |      "requiredCondition": {
      |        "always": {}
      |      }
      |    },
      |    {
      |      "blockType": "OLD_NAME",
      |      "requiredCondition": {
      |        "always": {}
      |      }
      |    },
      |    {
      |      "blockType": "GENDER",
      |      "allowedCondition": {
      |        "always": {}
      |      }
      |    },
      |    {
      |      "blockType": "PASSPORT_RF",
      |      "requiredCondition": {
      |        "always": {}
      |      }
      |    },
      |    {
      |      "blockType": "OLD_PASSPORT_RF",
      |      "requiredCondition": {
      |        "always": {}
      |      }
      |    },
      |    {
      |      "blockType": "FOREIGN_PASSPORT",
      |      "allowedCondition": {
      |        "always": {}
      |      }
      |    },
      |    {
      |      "blockType": "INSURANCE_NUMBER",
      |      "allowedCondition": {
      |        "always": {}
      |      }
      |    },
      |    {
      |      "blockType": "DRIVER_LICENSE",
      |      "allowedCondition": {
      |        "always": {}
      |      }
      |    },
      |    {
      |      "blockType": "BIRTH_DATE",
      |      "requiredCondition": {
      |        "always": {}
      |      }
      |    },
      |    {
      |      "blockType": "BIRTH_PLACE",
      |      "requiredCondition": {
      |        "always": {}
      |      }
      |    },
      |    {
      |      "blockType": "RESIDENCE_ADDRESS",
      |      "requiredCondition": {
      |        "always": {}
      |      }
      |    },
      |    {
      |      "blockType": "REGISTRATION_ADDRESS",
      |      "requiredCondition": {
      |        "always": {}
      |      }
      |    },
      |    {
      |      "blockType": "EDUCATION",
      |      "requiredCondition": {
      |        "always": {}
      |      }
      |    },
      |    {
      |      "blockType": "MARITAL_STATUS",
      |      "requiredCondition": {
      |        "always": {}
      |      }
      |    },
      |    {
      |      "blockType": "DEPENDENTS",
      |      "requiredCondition": {
      |        "always": {}
      |      }
      |    },
      |    {
      |      "blockType": "INCOME",
      |      "requiredCondition": {
      |        "always": {}
      |      }
      |    },
      |    {
      |      "blockType": "EXPENSES",
      |      "allowedCondition": {
      |        "always": {}
      |      }
      |    },
      |    {
      |      "blockType": "PROPERTY_OWNERSHIP",
      |      "requiredCondition": {
      |        "always": {}
      |      }
      |    },
      |    {
      |      "blockType": "VEHICLE_OWNERSHIP",
      |      "allowedCondition": {
      |        "always": {}
      |      }
      |    },
      |    {
      |      "blockType": "EMPLOYMENT",
      |      "requiredCondition": {
      |        "always": {}
      |      }
      |    },
      |    {
      |      "blockType": "RELATED_PERSONS",
      |      "allowedCondition": {
      |        "always": {}
      |      }
      |    },
      |    {
      |      "blockType": "PHONES",
      |      "requiredCondition": {
      |        "always": {}
      |      }
      |    },
      |    {
      |      "blockType": "EMAILS",
      |      "requiredCondition": {
      |        "always": {}
      |      }
      |    }
      |  ],
      |  "isActive": true,
      |  "borrowerConditions": {
      |    "employmentTypes": [
      |      "EMPLOYEE"
      |    ],
      |    "ageRange": {
      |      "from": 21,
      |      "to": 70
      |    },
      |    "minLastExperienceMonths": 3,
      |    "allExperienceMonths": 0,
      |    "incomeAfterExpenses": "0",
      |    "proofs": [],
      |    "incomeWithoutExpense": "10000",
      |    "checkAgeWithoutTerm": false,
      |    "exactlyRequirements": false,
      |    "geobaseIds": []
      |  },
      |  "bankId": "alfabank",
      |  "clientFeatures": [
      |    "SHARK_BANK_ID_SUPPORT"
      |  ],
      |  "priority": 1,
      |  "excludedGeobaseIds": [
      |    "977"
      |  ],
      |  "specificBorrowerConditions": [],
      |  "priorityTags": [
      |    "AMOUNT_BELOW_1M"
      |  ]
      |}
      |""".stripMargin

  private def parse(str: String): Task[CreditProduct] = Task.effect {
    decode[CreditProduct](str).getOrElse(throw new RuntimeException("Parse error!"))
  }

  override def spec: ZSpec[Environment, Failure] =
    suite("DealerCreditProduct")(
      testM("Full dealers credit product is success") {
        assertM(parse(dealerFullCreditProduct))(isSubtype[DealerCreditProduct](anything))
      },
      testM("Stub dealers credit product is success") {
        assertM(parse(dealerShortCreditProduct))(isSubtype[DealerCreditProductStub](anything))
      },
      testM("Full alfabank credit product is success") {
        assertM(parse(alfaBankFullCreditProduct))(isSubtype[ConsumerCreditProduct](anything))
      },
      testM("Short alfabank credit product is validation exception") {
        val expected = ValidationException(
          Seq(
            FieldError("amount_range.amount_range", "MissingValue(amount_range.amount_range)"),
            FieldError(
              "interest_rate_range.interest_rate_range",
              "MissingValue(interest_rate_range.interest_rate_range)"
            ),
            FieldError("term_months_range.term_months_range", "MissingValue(term_months_range.term_months_range)")
          )
        )
        assertM(parse(alfaBankShortCreditProduct).run)(fails(equalTo(expected)))
      }
    )
}
