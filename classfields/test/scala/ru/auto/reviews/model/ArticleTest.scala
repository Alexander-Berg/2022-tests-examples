package ru.auto.reviews.model

import com.google.protobuf.util.Timestamps
import org.scalatest.FunSuite
import play.api.libs.json.Json
import ru.auto.api.CatalogModel.{Mark, Model, SuperGeneration, TechInfo}
import ru.auto.api.CommonModel.Photo
import ru.auto.api.magazine.MagazineModel.MagazineArticleSnippet

import scala.collection.JavaConverters._

/**
  * Created by Karpenko Maksim (knkmx@yandex-team.ru) on 2019-07-11.
  */
class ArticleTest extends FunSuite {

  test("Article to snippet") {
    val theme1 = Theme(Some("themeTitle1"))
    val theme2 = Theme(Some("themeTitle2"))
    val themes = Seq(theme1, theme2)

    val car1 = Car("", "", "BMW X5 123")
    val car2 = Car("", "", "BMW X5")
    val car3 = Car("", "", "BMW")

    val car4 = Car("", "", "AUDI Q5")
    val car5 = Car("", "", "AUDI Q5")
    val car6 = Car("", "", "AUDI")

    val car7 = Car("", "", "BMW X5 456")

    val cars = Seq(car1, car2, car3, car4, car5, car6, car7)
    val images = Map(
      "size1" -> "url1",
      "size2" -> "url2"
    )

    val article = Article(
      "title",
      "text",
      "url",
      images,
      cars,
      themes,
      1562128200
    )

    val image = Photo
      .newBuilder()
      .putAllSizes(images.asJava)

    val info1 = TechInfo
      .newBuilder()
      .setMarkInfo(Mark.newBuilder().setCode("BMW"))
      .setModelInfo(Model.newBuilder().setCode("X5"))
      .setSuperGen(SuperGeneration.newBuilder().setId(123))
      .build()

    val info2 = TechInfo
      .newBuilder()
      .setMarkInfo(Mark.newBuilder().setCode("BMW"))
      .setModelInfo(Model.newBuilder().setCode("X5"))
      .setSuperGen(SuperGeneration.newBuilder().setId(456))
      .build()

    val info3 = TechInfo
      .newBuilder()
      .setMarkInfo(Mark.newBuilder().setCode("AUDI"))
      .setModelInfo(Model.newBuilder().setCode("Q5"))
      .build()

    val autoInfos = Seq(info1, info2, info3)
    val categories = themes.filter(_.title.isDefined).map(_.title.get).asJavaCollection
    val publishTime = Timestamps.fromSeconds(1562128200)

    val snippet = MagazineArticleSnippet
      .newBuilder()
      .setArticleUrl("url")
      .setText("text")
      .setMainPhoto(image)
      .addAllAutoInfo(autoInfos.asJava)
      .addAllCategories(categories)
      .setPublishTime(publishTime)
      .build()

    assert(article.toSnippet == snippet)

  }

  test("parse json") {
    val json =
      "{\"id\":\"g63cayennesvrdrag\",\"title\":\"Дрэг по полю: «Гелик» против Porsche Cayenne Turbo и Range Rover Sport SVR\",\"url\":\"https://mag.auto.ru/article/g63cayennesvrdrag/\",\"images\":{\"desktop\":\"https://autoru-mag.s3.yandex.net/2019/01/21/bd7086a5a8f946eb94f8558e10063727.gif/desktop\",\"mobile\":\"https://autoru-mag.s3.yandex.net/2019/01/21/be39aa2d1dd6428d80ca29bb94709499.jpeg/mobile\",\"wide\":\"https://autoru-mag.s3.yandex.net/2019/01/21/be39aa2d1dd6428d80ca29bb94709499.jpeg/wide\",\"wide@1.5\":\"https://autoru-mag.s3.yandex.net/2019/01/21/be39aa2d1dd6428d80ca29bb94709499.jpeg/wide@1.5\",\"wide@2\":\"https://autoru-mag.s3.yandex.net/2019/01/21/be39aa2d1dd6428d80ca29bb94709499.jpeg/wide@2\",\"wide@3\":\"https://autoru-mag.s3.yandex.net/2019/01/21/be39aa2d1dd6428d80ca29bb94709499.jpeg/wide@3\",\"4x3\":\"https://autoru-mag.s3.yandex.net/2019/01/21/be39aa2d1dd6428d80ca29bb94709499.jpeg/4x3\",\"4x3@1.5\":\"https://autoru-mag.s3.yandex.net/2019/01/21/be39aa2d1dd6428d80ca29bb94709499.jpeg/4x3@1.5\",\"4x3@2\":\"https://autoru-mag.s3.yandex.net/2019/01/21/be39aa2d1dd6428d80ca29bb94709499.jpeg/4x3@2\",\"4x3@3\":\"https://autoru-mag.s3.yandex.net/2019/01/21/be39aa2d1dd6428d80ca29bb94709499.jpeg/4x3@3\"},\"lead\":\"«Заряженные» внедорожники сразились друг с другом и с необычным для дрэга покрытием\",\"themes\":[{\"title\":\"Видео\",\"url\":\"https://mag.auto.ru/theme/video/\"},{\"title\":\"Дрэг\",\"url\":\"https://mag.auto.ru/theme/drag/\"},{\"title\":null,\"url\":\"https://mag.auto.ru/theme//\"},{\"title\":null,\"url\":\"https://mag.auto.ru/theme//\"}],\"cars\":[{\"title\":\"Mercedes-Benz\",\"alias\":\"MERCEDES\",\"url\":\"https://mag.auto.ru/theme/mercedes/\"},{\"title\":\"Land Rover\",\"alias\":\"LAND_ROVER\",\"url\":\"https://mag.auto.ru/theme/landrover/\"},{\"title\":\"Porsche\",\"alias\":\"PORSCHE\",\"url\":\"https://mag.auto.ru/theme/porsche/\"},{\"title\":\"G-klasse AMG\",\"alias\":\"MERCEDES G_KLASSE_AMG\",\"url\":\"https://mag.auto.ru/theme/mercedes-gklasseamg/\"},{\"title\":\"Mercedes-Benz G-klasse AMG II (W\",\"alias\":\"MERCEDES G_KLASSE_AMG 21203440\",\"url\":\"https://mag.auto.ru/theme/mercedes-gklasseamg-21203440/\"},{\"title\":\"Range Rover Sport\",\"alias\":\"LAND_ROVER RANGE_ROVER_SPORT\",\"url\":\"https://mag.auto.ru/theme/landrover-rangeroversport/\"},{\"title\":\"Cayenne\",\"alias\":\"PORSCHE CAYENNE\",\"url\":\"https://mag.auto.ru/theme/porsche-cayenne/\"},{\"title\":\"Porsche Cayenne III\",\"alias\":\"PORSCHE CAYENNE 21080912\",\"url\":\"https://mag.auto.ru/theme/porsche-cayenne-21080912/\"}],\"date\":1548075162,\"type\":\"video\",\"text\":\"YouTube-канал Carwow сравнил Porsche Cayenne Turbo (550 л.с.), Range Rover Sport SVR (575 л.с.) и Mercedes-AMG G63 (585 л.с.) в дрэге по полю. Но если вы не хотите ждать, то сразу перематывайте на 4:05. До этого машины гоняются по асфальту, да еще и не в полном составе.      Какая из этих машин круче,\"}"

    val js = Json.parse(json)
    val res = Json.fromJson[Article](js)
    assert(res.isSuccess)
  }
}
