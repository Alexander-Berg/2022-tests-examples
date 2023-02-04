package ru.yandex.vos2.autoru.model.extdata

import java.io.ByteArrayInputStream

import com.google.common.base.Charsets
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.OptionValues
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import ru.yandex.vertis.moderation.proto.Model.Reason

/**
  * Author: Vladislav Dolbilov (darl@yandex-team.ru)
  * Created: 13.02.18
  */
@RunWith(classOf[JUnitRunner])
class BanReasonsSpec extends AnyFunSuite with Matchers with OptionValues {
  test("parse BanReasons json") {
    val data =
      """{
        |   "no_docs":{
        |      "content":{
        |         "title":"Продажа ТС без документов",
        |         "title_lk":"Продажа ТС без документов",
        |         "comment":"Баним, когда осуществляется продажа ТС без документов или без переоформления",
        |         "agreement":"11.2.1 11.2.5",
        |         "agreement_links":[
        |            "https://auto.ru/pages/terms_of_service/#rule-11.2.1",
        |            "https://auto.ru/pages/terms_of_service/#rule-11.2.5"
        |         ],
        |         "text":"<b>Продажа ТС без документов</b><br>Вы пытались продать транспортное средство без документов, имеющее запрет на переоформление или утилизированное по данным ГИБДД. Это запрещено <a target='_blank' href='https://yandex.ru/legal/autoru_terms_of_service/'> правилами Авто.ру</a>.",
        |         "text_app":"Продажа ТС без документов. Вы пытались продать транспортное средство без документов, имеющее запрет на переоформление или утилизированное по данным ГИБДД. Это запрещено правилами Авто.ру",
        |         "text_app_html":"Вы пытались продать транспортное средство без документов, имеющее запрет на переоформление или утилизированное по данным ГИБДД. Это запрещено <a target='_blank' href='https://yandex.ru/legal/autoru_terms_of_service/'> правилами Авто.ру</a>.",
        |         "text_sms":"Мы проверили и удалили ваше объявление о продаже МАРКА / МОДЕЛЬ, потому что нашли в нем ошибки. Чтобы узнать причину, войдите в личный кабинет auth.auto.ru/login",
        |         "text_user_ban":"111",
        |         "text_user_ban_sms":"Мы заблокировали ваш аккаунт, все текущие объявления сняты с публикации и неактивны. Скорее всего, вы серьезно нарушили правила Пользовательского соглашения сайта. Подробности — https://auto.ru/my/.",
        |         "text_user_ban_app":"Паспорт, переоформление, регистрация. На auto.ru нельзя размещать объявления о продаже утилизированных или залоговых автомобилей и мотоциклов, транспортных средств без паспорта, без документов на переоформление или с действующим запретом на регистрацию.",
        |         "push_name":"Объявление заблокировано",
        |         "push_title":"Объявление заблокировано 🔒",
        |         "push_text":"Мы заблокировали объявление о продаже {mark} {model}, потому что нашли ошибки. Подробности в личном кабинете.",
        |         "deeplink":"autoru://app/users.auto.ru/sales",
        |         "vos_sender_template":"moderation.block_ad",
        |         "passport_sender_template":"moderation.block_cheat",
        |         "show_only_one_vos":"true",
        |         "show_only_one_passport":true,
        |         "offer_editable":true,
        |         "text_lk_dealer":"222"
        |      }
        |   },
        |   "commercial":{
        |      "content":{
        |         "title":"Коммерческая деятельность",
        |         "title_lk":"Коммерческая деятельность",
        |         "comment":"Баним, когда под видом частников размещаются юзеры, осуществляющие коммерческую деятельность",
        |         "agreement":"1.1.7 3.2 3.3 10.4 11.4 11.5",
        |         "agreement_links":[
        |            "https://auto.ru/pages/terms_of_service/#rule-1.1.7",
        |            "https://auto.ru/pages/terms_of_service/#rule-3.2",
        |            "https://auto.ru/pages/terms_of_service/#rule-3.3",
        |            "https://auto.ru/pages/terms_of_service/#rule-10.4",
        |            "https://auto.ru/pages/terms_of_service/#rule-11.4",
        |            "https://auto.ru/pages/terms_of_service/#rule-11.5"
        |         ],
        |         "text":"<b>Коммерческая деятельность</b><br>Мы полагаем, что вы занимаетесь на Авто.ру коммерческой деятельностью и разместили это объявление для получение прибыли. Если вы представляете салон, то <a target='_blank' href='https://auto.ru/dealer/'>подключитесь как партнер</a> и получите выгодные условия размещения.",
        |         "text_app":"Коммерческая деятельность. Мы полагаем, что вы занимаетесь на Авто.ру коммерческой деятельностью и разместили это объявление для получение прибыли. Если вы представляете салон, то подключитесь как партнер и получите выгодные условия размещения.",
        |         "text_app_html":"Мы полагаем, что вы занимаетесь на Авто.ру коммерческой деятельностью и разместили это объявление для получение прибыли. Если вы представляете салон, то <a target='_blank' href='https://auto.ru/dealer/'>подключитесь как партнер</a> и получите выгодные условия размещения.",
        |         "text_user_ban":"<b>Коммерческая деятельность</b><br>Вы разместили много объявлений с одного или нескольких кабинетов с целью получения прибыли, что похоже на <b>коммерческую деятельность</b>.  Сейчас ваш аккаунт и все объявления заблокированы.<br/>Чтобы вернуться на Авто.ру, <a target='_blank' href='https://auto.ru/dealer/#client-form'>подключитесь как дилер</a> и торгуйте честно.",
        |         "text_user_ban_sms":"Мы заблокировали ваш аккаунт и все объявления. Хотите продавать через сайт на постоянной основе? Подробнее — https://auto.ru/dealer",
        |         "text_user_ban_app":"Коммерческая деятельность. Эксперты auto.ru проанализировали вашу активность на сайте и обнаружили, что вы разместили несколько бесплатных объявлений через вымышленные аккаунты и / или публиковали их с очевидной регулярностью.</br>Ваши действия попадают под определение «коммерческая деятельность» — и это серьёзное нарушение правил сайта. Снять блокировку мы сможем только в том случае, если вы официально станете нашим клиентом.",
        |         "push_name":"Объявление заблокировано",
        |         "push_title":"Объявление заблокировано 🔒",
        |         "push_text":"Мы заблокировали объявление о продаже {mark} {model}, потому что нашли ошибки. Подробности в личном кабинете.",
        |         "deeplink":"autoru://app/users.auto.ru/sales",
        |         "passport_sender_template":"moderation.block_commercial",
        |         "offer_editable":false,
        |         "show_only_one_passport":false
        |      }
        |   }
        |}""".stripMargin
    val reasons = BanReasons.parse(new ByteArrayInputStream(data.getBytes(Charsets.UTF_8)))

    val map = reasons.getByKeys(Seq("1", "commercial", "no_docs"))
    map should have size 2
    val noDocs = map(Reason.NO_DOCS)

    noDocs.text should startWith("<b>Продажа ТС без документов</b>")
    noDocs.textSms.value should startWith("Мы проверили и удалили ваше объявление о продаже МАРКА / МОДЕЛЬ")
    noDocs.offerEditable should equal(true)
    noDocs.links shouldBe Set(
      "https://auto.ru/pages/terms_of_service/#rule-11.2.1",
      "https://auto.ru/pages/terms_of_service/#rule-11.2.5"
    )
    noDocs.sendOnlyOne should equal(true)
    noDocs.senderTemplate shouldBe Some("moderation.block_ad")

    val commercial = map(Reason.COMMERCIAL)
    commercial.text should startWith("<b>Коммерческая деятельность</b>")
    commercial.sendOnlyOne should equal(false)
    commercial.senderTemplate shouldBe None
  }
}
