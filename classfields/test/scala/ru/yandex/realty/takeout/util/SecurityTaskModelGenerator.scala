package ru.yandex.realty.takeout.util

import org.scalacheck.Gen
import ru.yandex.realty.model.gen.RealtyGenerators
import ru.yandex.realty.takeout.SecurityTaskStatus.SecurityCategory
import ru.yandex.realty.takeout.model.security.SecurityTask
import ru.yandex.vertis.util.time.DateTimeUtil

trait SecurityTaskModelGenerator extends RealtyGenerators {

  def securityTaskGen: Gen[SecurityTask] = {
    for {
      uid <- posNum[Long]
      categoryId <- protoEnumWithUnknown(SecurityCategory.values())
        .filter(c => c != SecurityCategory.CATEGORY_UNKNOWN && c != SecurityCategory.UNRECOGNIZED)
        .map(_.getNumber)
      shardKey <- Gen.chooseNum(0, 1)
      createTime <- Gen.const(DateTimeUtil.now())
      visitTime <- Gen.const(Some(DateTimeUtil.now()))
      endTime <- Gen.const(None)
    } yield SecurityTask(
      uid = uid,
      categoryId = categoryId,
      shardKey = shardKey,
      createTime = createTime,
      visitTime = visitTime,
      endTime = endTime
    )
  }

  def securityTaskGen(categoryId: Integer, uid: Long): Gen[SecurityTask] = {
    for {
      shardKey <- Gen.chooseNum(0, 1)
      createTime <- Gen.const(DateTimeUtil.now())
      visitTime <- Gen.const(Some(DateTimeUtil.now()))
      endTime <- Gen.const(None)
    } yield SecurityTask(
      uid = uid,
      categoryId = categoryId,
      shardKey = shardKey,
      createTime = createTime,
      visitTime = visitTime,
      endTime = endTime
    )
  }
}
