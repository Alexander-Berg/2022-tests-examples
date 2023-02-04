import org.scalatest.{Matchers, WordSpec}
import ru.yandex.auto.extdata.jobs.YaMapsDealersProducer

class UnitSpecs extends WordSpec with Matchers {

  "test original phone format parsing" in {
    def parsePhone(phone: String) = YaMapsDealersProducer.originalPhoneFormat(phone).toOption

    // positive tests
    parsePhone("+7 958 100-36-75") shouldEqual Some("79581003675")
    parsePhone("+7 812 244-77-88") shouldEqual Some("78122447788")
    parsePhone("+7 495 788-56-65") shouldEqual Some("74957885665")

    // negative tests
    parsePhone("") shouldEqual None
    parsePhone("+") shouldEqual None
    parsePhone("+7 (812) 244-77-88") shouldEqual None
    parsePhone("+7 244-77-88") shouldEqual None
    parsePhone("+72447788") shouldEqual None
  }

  "test only-numbers phone format" in {
    def parsePhone(phone: String) = YaMapsDealersProducer.ensureOnlyNumbersPhoneFormat(phone).toOption

    // positive tests
    parsePhone("79581003675") shouldEqual Some("79581003675")

    // negative tests
    parsePhone("") shouldEqual None
    parsePhone("+") shouldEqual None
    parsePhone("+79581003675") shouldEqual None
  }

  "test phone format parsers combination" in {
    def parsePhone(phone: String) =
      YaMapsDealersProducer
        .originalPhoneFormat(phone)
        .orElse(YaMapsDealersProducer.ensureOnlyNumbersPhoneFormat(phone))
        .toOption

    // positive tests
    parsePhone("79581003675") shouldEqual Some("79581003675")
    parsePhone("+7 958 100-36-75") shouldEqual Some("79581003675")
    parsePhone("+7 812 244-77-88") shouldEqual Some("78122447788")

    // negative tests
    parsePhone("") shouldEqual None
    parsePhone("+") shouldEqual None
    parsePhone("+79581003675") shouldEqual None
  }

  "test replace image size" in {
    YaMapsDealersProducer.replaceImageSize(
      "https://images.mds-proxy.test.avto.ru/get-autoru-vos/1936147/c315966c04a2af38a58888f3380477c6/624x832",
      "360x360"
    ) shouldEqual "https://images.mds-proxy.test.avto.ru/get-autoru-vos/1936147/c315966c04a2af38a58888f3380477c6/360x360"

    YaMapsDealersProducer.replaceImageSize(
      "https://images.mds-proxy.test.avto.ru/get-autoru-vos/1936147/c315966c04a2af38a58888f3380477c6/624x832/",
      ""
    ) shouldEqual "https://images.mds-proxy.test.avto.ru/get-autoru-vos/1936147/c315966c04a2af38a58888f3380477c6/"

    YaMapsDealersProducer.replaceImageSize(
      "https://images.mds-proxy.test.avto.ru/get-autoru-vos/1936147/c315966c04a2af38a58888f3380477c6/624x",
      ""
    ) shouldEqual "https://images.mds-proxy.test.avto.ru/get-autoru-vos/1936147/c315966c04a2af38a58888f3380477c6/"

    YaMapsDealersProducer.replaceImageSize(
      "https://images.mds-proxy.test.avto.ru/get-autoru-vos/1936147/c315966c04a2af38a58888f3380477c6/",
      ""
    ) shouldEqual "https://images.mds-proxy.test.avto.ru/get-autoru-vos/1936147/"

    YaMapsDealersProducer.replaceImageSize(
      "https://",
      ""
    ) shouldEqual "/"

    YaMapsDealersProducer.replaceImageSize(
      "",
      "360x360"
    ) shouldEqual "/360x360"
  }
}
