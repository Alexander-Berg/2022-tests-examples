package amogus.webhooks

import amogus.model.amo.CustomFieldTextValue
import amogus.model.company.CompanyChangedModel
import amogus.model.company.CompanyChangedModel._
import amogus.webhooks.Decoders.CompanyChangedModelDecoders.CompanyChangedModelDecoder
import io.circe.parser._
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

class DecodersSpec extends AnyWordSpecLike with Matchers {

  "Decoders" should {
    "Decode CompanyChangedModel" in {
      val rawJson = """
          |{
          |  "contacts": {
          |    "add": [
          |      {
          |        "modified_user_id": "6507739",
          |        "responsible_user_id": "6507739",
          |        "last_modified": "1635538854",
          |        "created_at": "1631695638",
          |        "updated_at": "1635538854",
          |        "name": "Тестовый клиент",
          |        "custom_fields": [
          |          {
          |            "name": "Тип компании",
          |            "id": "293097",
          |            "values": [
          |              {
          |                "enum": "395207",
          |                "value": "ДЦ"
          |              }
          |            ],
          |            "code": "company"
          |          },
          |          {
          |            "name": "Размер склада",
          |            "values": [
          |              {
          |                "value": "2119"
          |              }
          |            ],
          |            "id": "853667"
          |          }
          |        ],
          |        "created_user_id": "6507739",
          |        "id": "27182017",
          |        "linked_leads_id": [
          |          {
          |            "ID": "15332657"
          |          },
          |          {
          |            "ID": "15332743"
          |          }
          |        ],
          |        "type": "company",
          |        "old_responsible_user_id": "6101560",
          |        "date_create": "1631695638"
          |      }
          |    ],
          |    "update": [
          |      {
          |        "modified_user_id": "6507739",
          |        "responsible_user_id": "6507739",
          |        "last_modified": "1635538854",
          |        "created_at": "1631695638",
          |        "updated_at": "1635538854",
          |        "name": "Тестовый клиент",
          |        "custom_fields": [
          |          {
          |            "name": "Тип компании",
          |            "id": "293097",
          |            "values": [
          |              {
          |                "enum": "395207",
          |                "value": "ДЦ"
          |              }
          |            ],
          |            "code": "company"
          |          },
          |          {
          |            "name": "Размер склада",
          |            "values": [
          |              {
          |                "value": "2119"
          |              }
          |            ],
          |            "id": "853667"
          |          }
          |        ],
          |        "created_user_id": "6507739",
          |        "id": "27182017",
          |        "linked_leads_id": [
          |          {
          |            "ID": "15332657"
          |          },
          |          {
          |            "ID": "15332743"
          |          }
          |        ],
          |        "type": "company",
          |        "old_responsible_user_id": "6101560",
          |        "date_create": "1631695638"
          |      }
          |    ]
          |  },
          |  "account": {
          |    "id": "29138932",
          |    "subdomain": "autorutesting",
          |    "_links": {
          |      "self": "https://autorutesting.amocrm.ru"
          |    }
          |  }
          |}""".stripMargin

      val extractedModel = CompanyChangedModel(
        contacts = Contact(
          add = List(
            Add(
              id = 27182017L,
              responsibleUserId = "6507739",
              lastModified = "1635538854",
              createdAt = "1631695638",
              updatedAt = "1635538854",
              name = "Тестовый клиент",
              dateCreate = "1631695638",
              customFields = List(
                CustomField(293097L, "Тип компании", Some("company"), List(CustomFieldTextValue("ДЦ"))),
                CustomField(853667L, "Размер склада", None, List(CustomFieldTextValue("2119")))
              )
            )
          ),
          update = List(
            Update(
              id = 27182017L,
              responsibleUserId = "6507739",
              lastModified = "1635538854",
              modifiedUserId = Some("6507739"),
              createdAt = "1631695638",
              updatedAt = "1635538854",
              name = "Тестовый клиент",
              oldResponsibleUserId = Some("6101560"),
              dateCreate = "1631695638",
              customFields = List(
                CustomField(293097L, "Тип компании", Some("company"), List(CustomFieldTextValue("ДЦ"))),
                CustomField(853667L, "Размер склада", None, List(CustomFieldTextValue("2119")))
              )
            )
          )
        ),
        account = Account("29138932", "autorutesting")
      )

      decode[CompanyChangedModel](rawJson) shouldBe Right(extractedModel)
    }
  }
}
