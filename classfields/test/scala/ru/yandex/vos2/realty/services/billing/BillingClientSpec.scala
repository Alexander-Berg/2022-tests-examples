package ru.yandex.vos2.realty.services.billing

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.{Matchers, WordSpec}
import play.api.libs.json.Json

/**
  * Created by Vsevolod Levin on 01.06.2018.
  */
@RunWith(classOf[JUnitRunner])
class BillingClientSpec extends WordSpec with Matchers {

  "BillingClient" should {
    "detect balance agency" in {
      val json = Json.parse(
        """{
          |  "user": 326478913,
          |  "role": "RegularUser",
          |  "client": {
          |    "city": "",
          |    "name": "ООО 2P-PИЭЛТИ",
          |    "email": "",
          |    "url": "",
          |    "agency": true,
          |    "id": 7507065,
          |    "fax": "",
          |    "agencyId": 7507065,
          |    "type": "IndividualPerson",
          |    "phone": ""
          |  },
          |  "customers": []
          |}""".stripMargin
      )

      BillingClient.isAgency(json) should be(true)

    }

    "detect balance under agency" in {
      val json = Json.parse(
        """{
          |  "user": 326478913,
          |  "role": "RegularUser",
          |  "client": {
          |    "city": "",
          |    "name": "ООО 2P-PИЭЛТИ",
          |    "email": "",
          |    "url": "",
          |    "agency": false,
          |    "id": 7507065,
          |    "fax": "",
          |    "agencyId": 7507065,
          |    "type": "IndividualPerson",
          |    "phone": ""
          |  },
          |  "customers": []
          |}""".stripMargin
      )

      BillingClient.isAgency(json) should be(true)

    }

    "detect balance non agency" in {
      val json = Json.parse(
        """{
          |  "user": 326478913,
          |  "role": "RegularUser",
          |  "client": {
          |    "city": "",
          |    "name": "ООО 2P-PИЭЛТИ",
          |    "email": "",
          |    "url": "",
          |    "agency": false,
          |    "id": 7507065,
          |    "fax": "",
          |    "type": "IndividualPerson",
          |    "phone": ""
          |  },
          |  "customers": []
          |}""".stripMargin
      )

      BillingClient.isAgency(json) should be(false)
    }

    "detect billing non agency" in {
      val json = Json.parse(
        """{
          |  "user": 3470308,
          |  "role": "RegularUser",
          |  "customers": [
          |    {
          |      "clientId": 322438,
          |      "client": {
          |        "city": "",
          |        "name": "Кальницкий Андрей Станиславович",
          |        "email": "Kalnitskiyas@best-realty.ru",
          |        "url": "",
          |        "agency": false,
          |        "id": 322438,
          |        "fax": "",
          |        "type": "IndividualPerson",
          |        "regionId": 225,
          |        "phone": "8-926-754-24-30"
          |      },
          |      "resourceRefs": [
          |        {
          |          "capaPartnerId": "1001606094"
          |        }
          |      ],
          |      "resources": []
          |    }
          |  ]
          |}""".stripMargin
      )

      BillingClient.isAgency(json) should be(false)
    }

    "detect billing under agency" in {
      val json = Json.parse(
        """{
          |  "user": 51697174,
          |  "role": "RegularUser",
          |  "customers": [
          |    {
          |      "clientId": 1020459,
          |      "client": {
          |        "city": "Владимир",
          |        "name": "Апекс-реалти",
          |        "email": "apex-realt@yandex.ru",
          |        "url": "-",
          |        "agency": false,
          |        "id": 1020459,
          |        "fax": "-",
          |        "agencyId": 469552,
          |        "type": "IndividualPerson",
          |        "regionId": 225,
          |        "phone": "84955180447"
          |      },
          |      "resourceRefs": [
          |        {
          |          "capaPartnerId": "5613635"
          |        }
          |      ],
          |      "resources": []
          |    }
          |  ]
          |}
          |""".stripMargin
      )

      BillingClient.isAgency(json) should be(true)
    }

    "detect billing agency" in {
      val json = Json.parse(
        """{
          |  "user": 51697174,
          |  "role": "RegularUser",
          |  "customers": [
          |    {
          |      "clientId": 1020459,
          |      "client": {
          |        "city": "Владимир",
          |        "name": "Апекс-реалти",
          |        "email": "apex-realt@yandex.ru",
          |        "url": "-",
          |        "agency": true,
          |        "id": 1020459,
          |        "fax": "-",
          |        "type": "IndividualPerson",
          |        "regionId": 225,
          |        "phone": "84955180447"
          |      },
          |      "resourceRefs": [
          |        {
          |          "capaPartnerId": "5613635"
          |        }
          |      ],
          |      "resources": []
          |    }
          |  ]
          |}
          |""".stripMargin
      )

      BillingClient.isAgency(json) should be(true)
    }

    "detect billing agency with multiple customers" in {
      val json = Json.parse(
        """{
          |  "user": 51697174,
          |  "role": "RegularUser",
          |  "customers": [
          |    {
          |      "clientId": 1020459,
          |      "client": {
          |        "city": "Владимир",
          |        "name": "Апекс-реалти",
          |        "email": "apex-realt@yandex.ru",
          |        "url": "-",
          |        "agency": false,
          |        "id": 1020459,
          |        "fax": "-",
          |        "type": "IndividualPerson",
          |        "regionId": 225,
          |        "phone": "84955180447"
          |      },
          |      "resourceRefs": [
          |        {
          |          "capaPartnerId": "5613635"
          |        }
          |      ],
          |      "resources": []
          |    },
          |     {
          |      "clientId": 1020459,
          |      "client": {
          |        "city": "Владимир",
          |        "name": "Апекс-реалти",
          |        "email": "apex-realt@yandex.ru",
          |        "url": "-",
          |        "agency": true,
          |        "id": 1020459,
          |        "fax": "-",
          |        "type": "IndividualPerson",
          |        "regionId": 225,
          |        "phone": "84955180447"
          |      },
          |      "resourceRefs": [
          |        {
          |          "capaPartnerId": "5613635"
          |        }
          |      ],
          |      "resources": []
          |    }
          |  ]
          |}
          |""".stripMargin
      )

      BillingClient.isAgency(json) should be(true)
    }
  }

}
