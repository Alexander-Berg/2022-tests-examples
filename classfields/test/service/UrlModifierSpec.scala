package ru.vertistraf.cost_plus.builder.auto.service

import ru.vertistraf.cost_plus.model.auto.Section
import zio.ZLayer
import zio.test.Assertion._
import zio.test._
import zio.test.environment.TestEnvironment

object UrlModifierSpec extends DefaultRunnableSpec {

  private def makeGen[A](elems: A*): Gen[Any, A] =
    Gen.fromIterable(elems)

  override def spec: ZSpec[TestEnvironment, Any] =
    suite("UrlModifier")(
      testM("should correctly add mark to listing") {
        val cases =
          makeGen(
            "/cars/all/color-red/" -> "/cars/audi/all/color-red/",
            "/moskva/cars/all/color-red/" -> "/moskva/cars/audi/all/color-red/",
            "/abakan/cars/1997-year/used/" -> "/abakan/cars/audi/1997-year/used/"
          )

        checkAllM(cases) { case (input, expected) =>
          UrlModifier
            .addMarkToListing(input, "AUDI")
            .map(actual => assertTrue(actual == expected))
        }
      },
      testM("should correctly add model to listing") {

        val cases = makeGen(
          ("/cars/audi/all/color-red/", "/cars/audi/a7/all/color-red/"),
          ("/moskva/cars/audi/all/color-red/", "/moskva/cars/audi/a7/all/color-red/"),
          ("/cars/all/color-red/", "/cars/audi/a7/all/color-red/"),
          ("/moskva/cars/all/color-red/", "/moskva/cars/audi/a7/all/color-red/"),
          ("/abakan/cars/1997-year/used/", "/abakan/cars/audi/a7/1997-year/used/")
        )

        checkAllM(cases) { case (input, expected) =>
          UrlModifier
            .addModelToListing(input, "AUDI", "A7", Option.when(input.contains("audi"))("AUDI"))
            .map(actual => assertTrue(actual == expected))
        }
      },
      testM("should fail on bad url when adding model") {
        val urls = Iterable(
          "/cars/audi/all/color-red/" -> Some("audi") // another mark
        )

        checkAllM(Gen.fromIterable(urls)) { case (u, m) =>
          UrlModifier
            .addModelToListing(u, "SSANG_YONG", "H7", m)
            .run
            .map(res => assert(res)(fails(anything)))
        }
      },
      testM("should correctly build listing from tags url") {
        UrlModifier
          .buildListingWithState(Some("moskva"), "AUDI", "A7", Section.All)
          .map(actual => assertTrue(actual == "/moskva/cars/audi/a7/all/"))
      },
      testM("should correctly replace vendors") {
        UrlModifier
          .replaceVendorWithMark(
            "/cars/vendor-european/used/drive-forward_wheel/",
            vendorCode = "european",
            markCode = "AUDI"
          )
          .map(actual => assertTrue(actual == "/cars/audi/used/drive-forward_wheel/"))
      },
      testM("should correctly fail on replace vendors") {
        UrlModifier
          .replaceVendorWithMark(
            "/cars/vendor-european/used/drive-forward_wheel/",
            vendorCode = "foreign",
            markCode = "AUDI"
          )
          .run
          .map(res => assert(res)(fails(equalTo(UrlModifier.UrlModifierException.MissingVendor("foreign")))))
      },
      testM("should correctly add complectation parameters to listing") {
        val cases = makeGen(
          "/cars/porsche/panamera/new/" -> "/cars/new/group/porsche/panamera/22481288-22481337/",
          "/mineralnye_vody/cars/porsche/panamera/new/" -> "/mineralnye_vody/cars/new/group/porsche/panamera/22481288-22481337/"
        )

        checkAllM(cases) { case (input, expected) =>
          UrlModifier
            .addComplectationToListing(input, 22481288L, 22481337L)
            .map(actual => assertTrue(actual == expected))
        }
      }
    ).provideLayer(UrlModifier.live ++ ZLayer.requires[TestEnvironment])
}
