package ru.yandex.vertis.punisher

import cats.syntax.applicative._
import ru.yandex.vertis.Domain
import ru.yandex.vertis.clustering.proto.Model.ClusteringFormula
import ru.yandex.vertis.moderation.proto.Model.{Essentials, Instance, Opinion, Reason}
import ru.yandex.vertis.moderation.proto.RealtyLight.UserRealtyEssentials
import ru.yandex.vertis.moderation.proto.RealtyLight.UserRealtyEssentials.UserType
import ru.yandex.vertis.punisher.model._
import ru.yandex.vertis.punisher.services.ModerationService.{SendResult, SwitchOffs}
import ru.yandex.vertis.punisher.services._
import ru.yandex.vertis.punisher.stages.Clusterizer
import ru.yandex.vertis.punisher.stages.impl.ClusterizerImpl

import scala.annotation.nowarn
import scala.jdk.CollectionConverters._

object MockRealtyServicesBuilder extends BaseSpec {

  private val ModerationInstanceFile: String = "/csv/realty-moderation-instance.csv"

  private val userClusters: Set[Set[UserId]] =
    resourceLines("/csv/realty-user-clusters.csv").map { line =>
      line.split(';').map(_.trim).filter(_.nonEmpty).toSet
    }.toSet

  private val userClusteringService: UserClusteringService[F] =
    new UserClusteringService[F] {

      override protected def domain: Domain = Domain.DOMAIN_REALTY

      override def getCluster(clusteringFormula: ClusteringFormula)(userId: UserId, kafkaOffset: Option[KafkaOffset]) =
        clusteringFormula match {
          case ClusteringFormula.L1_STRICT =>
            UserIdCluster(userClusters.find(_.contains(userId)).getOrElse(Set(userId)), userId).pure[F]
          case _ => ???
        }
    }

  val clusterizer: Clusterizer[F] = new ClusterizerImpl(userClusteringService, ClusteringFormula.L1_STRICT)

  val moderationService: ModerationService[F] =
    new ModerationService[F] {

      private case class ModerationInstance(userType: UserType, reasons: Seq[Reason])

      @nowarn("cat=other-match-analysis")
      private val instanceMap: Map[UserId, ModerationInstance] =
        resourceLines(ModerationInstanceFile).map { line =>
          val List(userId, userType, reasonsStr) = line.split(";").toList
          userId -> ModerationInstance(UserType.valueOf(userType), reasonsStr.split(",").map(Reason.valueOf).toSeq)
        }.toMap

      override def instanceOpt(user: ModerationService.ExternalId): F[Option[Instance]] = {
        val version = 1
        instanceMap
          .get(user.userId)
          .map { instance =>
            val opinion =
              Opinion.newBuilder
                .addAllReasons(instance.reasons.asJava)
                .setVersion(version)
            val userRealtyEssentials =
              UserRealtyEssentials.newBuilder
                .setUserType(instance.userType)
                .setVersion(version)
            val essentials =
              Essentials.newBuilder
                .setUserRealty(userRealtyEssentials)
                .setVersion(version)
            Instance.newBuilder
              .setOpinion(opinion)
              .setEssentials(essentials)
              .setVersion(version)
              .setHashVersion(version)
              .build
          }
          .pure[F]
      }

      override def send(signal: ModerationService.Signal): F[SendResult] = ???

      override def send(switchOffs: SwitchOffs): F[SendResult] = ???
    }
}
