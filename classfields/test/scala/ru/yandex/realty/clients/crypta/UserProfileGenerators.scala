package ru.yandex.realty.clients.crypta

import com.google.protobuf.ByteString
import org.scalacheck.Gen
import ru.yandex.vertis.generators.BasicGenerators
import ru.yandex.proto.crypta.Profile
import ru.yandex.proto.crypta.Profile.ProfileItem

import scala.collection.JavaConverters._

/**
  * @author azakharov
  */
trait UserProfileGenerators {

  def profileItemGen: Gen[ProfileItem] = {
    for {
      keywordId: Int <- BasicGenerators.posNum[Int]
      updateTime: Int <- BasicGenerators.posNum[Int]
      stringValue <- BasicGenerators.readableString
    } yield {
      ProfileItem
        .newBuilder()
        .setKeywordId(keywordId)
        .setUpdateTime(updateTime)
        .setStringValue(ByteString.copyFrom(stringValue.getBytes))
        .build()
    }
  }

  def userProfileGen: Gen[Profile] = {
    for {
      profileItems <- BasicGenerators.list(1, 3, profileItemGen)
    } yield Profile.newBuilder().addAllItems(profileItems.asJava).build()
  }
}
