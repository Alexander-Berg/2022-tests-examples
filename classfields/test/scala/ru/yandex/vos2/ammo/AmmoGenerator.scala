package ru.yandex.vos2.ammo

import org.scalacheck.Gen
import play.api.libs.json.{JsArray, Json}
import ru.yandex.vos2.UserModel.User
import ru.yandex.vos2.ammo.DefaultPaths.{file, host}
import ru.yandex.vos2.model.user.UserGenerator
import ru.yandex.vos2.model.{AmmoGenBase, UserRef, UserRefUID}
import ru.yandex.vos2.realty.api.rendering.UserRenderer
import ru.yandex.vos2.realty.model.offer.RealtyOfferGenerator

import scala.collection.mutable

object DefaultPaths {
  //    val file = s"/Users/747mmhg/job/vos/misc/generated/vos2-$numberOfUsers-users-$numberOfOffersPerUser-offers-per-user.ammo.gz"
  val file: String = ???
  val host = "vos2-backend.srv01ht.vertis.yandex.net"
}

/**
  * Generates lunapark ammos in request-style format.
  * https://yandextank.readthedocs.org/en/latest/tutorial.html#first-steps
  *
  * @author Leonid Nalchadzhi (nalchadzhi@yandex-team.ru)
  */
object AmmoGenerator extends AmmoGenBase(file, host) with App {

  val numberOfUsers = 2110
  val numberOfUserUpdatesPerUser = 5
  val updateNPercentOfUsers = 5
  val numberOfOffersPerUser = 50
  val numberOfUserPerOffer = 3
  val bufferSize = 1000
  val percent = numberOfUsers / 100

  generate()

  override protected def doGenerate(): Unit = {
    val users = mutable.ArrayBuffer[User]()
    val buffer = mutable.ArrayBuffer[String]()
    for (a ← 1 to numberOfUsers) {
      for (u ← UserGenerator.NewUserGen.sample) {
        users += u
        val userJsonString: String = (request ++ UserRenderer.render(u)).toString()
        write(req("/api/realty/user/create", "create_user", "POST", userJsonString))

        if (a % (updateNPercentOfUsers * percent) == 0) {
          val uid = UserRef.from(u.getUserRef).asInstanceOf[UserRefUID].uid
          for (update ← 1 to numberOfUserUpdatesPerUser) {
            buffer += req(s"/api/realty/user/update/$uid", "update_user", "PUT", userJsonString)
          }
        }

        for {
          a ← 1 until numberOfOffersPerUser
          user ← Gen.oneOf(users).sample
          o ← RealtyOfferGenerator.offerGen(user).sample
        } yield {
          val offer = RealtyOfferAmmoRenderer.render(o)
          val offerJsonString = (request ++ Json.obj("offers" → JsArray(Seq(offer)))).toString()
          val sampleUid = user.getAlternativeIds.getExternal
          for (i ← 1 until numberOfUserPerOffer) {
            buffer += req(s"/api/realty/user/$sampleUid", "get_user")
          }
          buffer += req(s"/api/realty/offer/create/$sampleUid", "create_offer", "POST", offerJsonString)
          buffer += req(s"/api/realty/offer/create/$sampleUid", "create_offer", "POST", offerJsonString)
          buffer += req(
            s"/api/realty/user_offers/$sampleUid?offerType=SELL&category=APARTMENT&showStatus=published&sort=endOfShow",
            "user_offers"
          )
          buffer += req(s"/api/realty/user_offers/$sampleUid/statistics", "statistics")
        }

        // val offerJsonString = (request ++ Json.obj("offers" -> JsArray(offerJsons))).toString()
        // fileWriter.write(req(s"/api/realty/offer/create/${u.getRefYaUID.getRef}", "POST", offerJsonString))

        if (buffer.size >= bufferSize) {
          dump(buffer)
          buffer.clear()
        }
        if (a % percent == 0) {
          println(s"${a / percent}% generated")
        }
      }
      dump(buffer)
    }
  }
}
