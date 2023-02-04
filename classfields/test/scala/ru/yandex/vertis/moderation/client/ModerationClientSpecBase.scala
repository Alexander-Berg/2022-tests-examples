package ru.yandex.vertis.moderation.client

import ru.yandex.vertis.moderation.client.Generators._
import ru.yandex.vertis.moderation.client.Producer.generatorAsProducer
import ru.yandex.vertis.moderation.proto.Model._
import ru.yandex.vertis.moderation.proto.ModelFactory

import scala.collection.JavaConverters._

/**
  * Base specs on [[ModerationClient]]
  *
  * @author semkagtn
  */
trait ModerationClientSpecBase
  extends SpecBase
    with TestParams {

  def moderationClientFactory: ModerationClientFactory
  def service: Service
  def instanceFilter: InstanceSource => Boolean = {
    is => is.getSignalsList.asScala.forall(s => {
        if(s.hasBanSignal) {
          checkNoMarker(s.getBanSignal.getSource)
        } else if (s.hasUnbanSignal) {
          checkNoMarker(s.getUnbanSignal.getSource)
        } else if(s.hasHoboSignal) {
          checkNoMarker(s.getHoboSignal.getSource)
        } else if(s.hasIndexErrorSignal) {
          checkNoMarker(s.getIndexErrorSignal.getSource)
        } else if (s.hasTagSignal) {
          checkNoMarker(s.getTagSignal.getSource)
        } else if (s.hasWarnSignal) {
          checkNoMarker(s.getWarnSignal.getSource)
        } else {
          true
        }
      }
    )
  }

  def checkNoMarker(source: Source): Boolean = {
    if(source.hasAutomaticSource) {
      val automaticSource = source.getAutomaticSource
      if(automaticSource.hasSourceMarker) {
        automaticSource.getSourceMarker.getType == SourceMarker.Type.NO_MARKER
      } else {
        true
      }
    } else {
      true
    }
  }

  lazy val moderationClient: ModerationClient = moderationClientFactory.client(host, port, service)

  "push" should {

    "correctly work with one source" in {
      val instanceSource = InstanceSourceGen.suchThat(instanceFilter).next
      val unit = moderationClient.push(Iterable(instanceSource)).futureValue
      unit should be(())
    }

    "correctly work with two sources" in {
      val instanceSources = InstanceSourceGen.suchThat(instanceFilter).next(2)
      val unit = moderationClient.push(instanceSources).futureValue
      unit should be(())
    }

    "correctly work with one source with useBatch = false" in {
      val instanceSource = InstanceSourceGen.suchThat(instanceFilter).next
      val unit = moderationClient.push(Iterable(instanceSource), useBatch = false).futureValue
      unit should be(())
    }

    "correctly work with two sources with useBatch = flase" in {
      val instanceSources = InstanceSourceGen.suchThat(instanceFilter).next(2)
      val unit = moderationClient.push(instanceSources, useBatch = false).futureValue
      unit should be(())
    }
  }

  "opinions" should {

    "correctly work for one unknown external id" in {
      val externalId = ExternalIdGen.next
      val opinions = moderationClient.opinions(Seq(externalId)).futureValue
      opinions.map(_.getType) should be(Seq(Opinion.Type.UNKNOWN))
    }

    "correctly work for two external id" in {
      val instanceSource = ModelFactory.newInstanceSourceBuilder.
        mergeFrom(InstanceSourceGen.suchThat(instanceFilter).next).
        addSignals(ModelFactory.newSignalSourceBuilder.setBanSignal(BanSignalSourceGen.next)).
        build

      val externalId = instanceSource.getExternalId
      val unknownExternalId = ExternalIdGen.next
      moderationClient.push(Iterable(instanceSource)).futureValue

      Thread.sleep(500)
      val opinions = moderationClient.opinions(Seq(unknownExternalId, externalId)).futureValue

      opinions.map(_.getType) should be(Seq(Opinion.Type.UNKNOWN, Opinion.Type.FAILED))
    }
  }

  "instance" should {
    "correctly get last instance" in {
      val instanceSource1 = InstanceSourceGen.suchThat(instanceFilter).next
      val unit1 = moderationClient.push(Iterable(instanceSource1)).futureValue
      unit1 should be(())

      val externalId = instanceSource1.getExternalId
      val instanceSource2 = instanceSourceGen(instanceSource1.getExternalId).
        suchThat(instanceFilter).
        suchThat(_.getContext.getVisibility != instanceSource1.getContext.getVisibility).next
      val unit2 = moderationClient.push(Iterable(instanceSource2)).futureValue
      unit2 should be(())

      val result = moderationClient.getAllCurrent(Set(externalId)).futureValue

      result.size should be(1)
      result.head.getEssentials should be(instanceSource2.getEssentials)
    }
  }

  "update context" should {
    "correctly update" in {
      val instanceSource =
        InstanceSourceGen.suchThat(instanceFilter).next
      val externalId = instanceSource.getExternalId
      val newContextSource = ContextSourceGen.suchThat(s => s != instanceSource.getContext).next
      val idAndSource = ModelFactory.newExternalIdAndContextSourceBuilder().
        setExternalId(externalId).
        setContextSource(newContextSource).
        build()

      moderationClient.push(Iterable(instanceSource)).futureValue should be(())

      val cs = moderationClient.getAllCurrent(Set(externalId)).futureValue.head.getContext
      cs.getVisibility should be (instanceSource.getContext.getVisibility)
      Option(cs.getTag) should be (Option(instanceSource.getContext.getTag))

      moderationClient.setContext(idAndSource).futureValue

      val cs2 = moderationClient.getAllCurrent(Set(externalId)).futureValue.head.getContext
      cs2.getVisibility should be (newContextSource.getVisibility)
      Option(cs2.getTag) should be (Option(newContextSource.getTag))
    }
  }

  private def getUser(user: User) = {
    if (user.hasYandexUser) {
      s"Yandex(${user.getYandexUser})"
    } else if (user.hasPartnerUser) {
      s"Partner(${user.getPartnerUser})"
    } else if (user.hasDealerUser) {
      s"Dealer(${user.getDealerUser})"
    } else if (user.hasAutoruUser) {
      s"Autoru(${user.getAutoruUser})"
    } else {
      throw new IllegalArgumentException(s"user with unknown type")
    }
  }
}
