package ru.vertistraf.notification_center

import ru.vertistraf.notification_center.mindbox_api.model.mindbox.MindboxBody
import ru.vertistraf.notification_center.mindbox_api.model.mindbox.PushData.FlagPushData
import ru.vertistraf.notification_center.model.ExpFlag
import zio.random.Random
import zio.test.magnolia.DeriveGen
import zio.test.{Gen, Sized}

package object mindbox_api {
  val mindboxBodyGen: Gen[Random with Sized, MindboxBody] = DeriveGen[MindboxBody]

  val flagPushDataGen: Gen[Random with Sized, FlagPushData] = DeriveGen[FlagPushData]

  val flagNameGen: Gen[Random, ExpFlag] = Gen.stringBounded(0, 100)(Gen.anyASCIIChar)

  val flagsListGen: Gen[Random with Sized, List[(ExpFlag, FlagPushData)]] =
    Gen.sized(n => Gen.listOfBounded(1, n)(Gen.zipN(flagNameGen, flagPushDataGen)(_ -> _)))
}
