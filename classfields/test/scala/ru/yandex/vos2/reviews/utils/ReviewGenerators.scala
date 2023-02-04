package ru.yandex.vos2.reviews.utils

import java.lang.System.currentTimeMillis

import com.google.protobuf.Timestamp
import org.scalacheck.Gen
import ru.auto.api.ApiOfferModel.Category
import ru.auto.api.CarsModel.CarInfo
import ru.auto.api.reviews.ReviewModel.Review
import ru.auto.api.reviews.ReviewModel.Review.{Item, ReviewUser, Status}

/**
  * Created by Karpenko Maksim (knkmx@yandex-team.ru) on 24/11/2017.
  */
object ReviewGenerators {

  val UserGen: Gen[ReviewUser] = for {
    id <- Gen.posNum[Int]
  } yield ReviewUser.newBuilder().setId(id.toString).build()


  val PastProtoTimeGen: Gen[Timestamp] = for {
    timestamp <- Gen.choose(1000000000000L, currentTimeMillis())
  } yield {
    Timestamp.newBuilder()
      .setSeconds(timestamp / 1000)
      .build()
  }

  val ReviewGen: Gen[Review] = for {
    user <- UserGen
    car <- CarInfoGen
  } yield {
    val auto = Item.Auto.newBuilder()
      .setCategory(Category.CARS)
      .setMark(car.getMark)
      .setModel(car.getModel)
      .setSuperGenId(car.getSuperGenId)
      .setTechParamId(car.getTechParamId)
      .build()

    Review.newBuilder()
      .setReviewer(user)
      .setItem(Item.newBuilder().setAuto(auto))
      .setStatus(Status.ENABLED)
      .build()
  }

  val CarInfoGen: Gen[CarInfo] = for {
    (mark, model, superGen, configuration, techParam) ← Gen.oneOf(
      ("AUDI", "A4", 20637504, 20637561, 20693205),
      ("FORD", "FOCUS", 20243246, 20243254, 20537272),
      ("BMW", "3ER", 20548423, 20548432, 20549159),
      ("VAZ", "2170", 20067929, 20067939, 20400410)
    )
  } yield {
    CarInfo.newBuilder()
      .setMark(mark)
      .setModel(model)
      .setSuperGenId(superGen)
      .setConfigurationId(configuration)
      .setTechParamId(techParam)
      .build()
  }

  val CarRandomInfoGen: Gen[CarInfo] = for {
    mark <- Gen.alphaStr.filter(_.nonEmpty)
    model <- Gen.alphaStr.filter(_.nonEmpty)
    superGen <- Gen.numStr.filter(_.nonEmpty)
    configuration <- Gen.numStr.filter(_.nonEmpty)
    techParam <- Gen.numStr.filter(_.nonEmpty)
  } yield {
    CarInfo.newBuilder()
      .setMark(mark.take(10).toUpperCase)
      .setModel(model.take(10).toUpperCase)
      .setSuperGenId(superGen.take(5).toInt)
      .setConfigurationId(configuration.take(5).toInt)
      .setTechParamId(techParam.take(5).toInt)
      .build()
  }

}
