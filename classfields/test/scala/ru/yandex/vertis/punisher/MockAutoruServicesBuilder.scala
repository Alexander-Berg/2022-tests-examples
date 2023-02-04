package ru.yandex.vertis.punisher

import java.time.ZonedDateTime
import cats.syntax.applicative._
import ru.yandex.vertis.Domain
import ru.yandex.vertis.clustering.proto.Model.ClusteringFormula
import ru.yandex.vertis.moderation.proto.Model.Domain.UsersAutoru
import ru.yandex.vertis.moderation.proto.Model.{Opinion, Opinions, Reason}
import ru.yandex.vertis.moderation.proto.{Autoru, Model}
import ru.yandex.vertis.punisher.model._
import ru.yandex.vertis.punisher.services._
import ru.yandex.vertis.punisher.stages.Clusterizer
import ru.yandex.vertis.punisher.stages.impl.ClusterizerImpl

import scala.jdk.CollectionConverters._
import scala.collection.mutable

object MockAutoruServicesBuilder extends BaseSpec {

  private val userClusters: Set[Set[UserId]] =
    resourceLines("/csv/autoru-user-clusters.csv").map { line =>
      line.split(';').map(_.trim).filter(_.nonEmpty).toSet
    }.toSet

  private val userClusteringService: UserClusteringService[F] =
    new UserClusteringService[F] {

      override protected def domain: Domain = Domain.DOMAIN_AUTO

      override def getCluster(
          clusteringFormula: ClusteringFormula
      )(userId: UserId, kafkaOffset: Option[KafkaOffset]): F[UserIdCluster] =
        clusteringFormula match {
          case ClusteringFormula.L1_STRICT =>
            UserIdCluster(userClusters.find(_.contains(userId)).getOrElse(Set(userId)), userId).pure[F]
          case _ => ???
        }
    }

  val clusterizer: Clusterizer[F] = new ClusterizerImpl(userClusteringService, ClusteringFormula.L1_STRICT)

  val moderationService: ModerationService[F] =
    new ModerationService[F] {

      private val No: String = "0"
      private val Comma: String = ","
      private val Semicolon = ";"
      private val Hash = "#"

      val userInstanceMap = mutable.Map.empty[UserId, Model.Instance.Builder]

      resourceLines("/csv/autoru-vertis-passport-get-user.csv").foreach { list =>
        val splitted = list.split(Semicolon)
        val userId = splitted(0)
        val email = splitted(1)

        val extId =
          Model.ExternalId
            .newBuilder()
            .setVersion(1)
            .setUser(Model.User.newBuilder().setVersion(1).setAutoruUser(userId))
            .build()

        val autoruEssBuilder = Autoru.UserAutoruEssentials.newBuilder()
        autoruEssBuilder.setVersion(1).setEmail(email)
        if (!splitted(2).equals(No)) autoruEssBuilder.setClientId(splitted(2))

        val essentials =
          Model.Essentials
            .newBuilder()
            .setVersion(1)
            .setUserAutoru(autoruEssBuilder.build())

        userInstanceMap.update(
          userId,
          Model.Instance
            .newBuilder()
            .setHashVersion(1)
            .setVersion(1)
            .setExternalId(extId)
            .setEssentials(essentials)
        )
      }

      resourceLines("/csv/autoru-vertis-passport-moderation.csv").foreach { list =>
        val splitted = list.split(Semicolon)
        val userId = splitted(0)
        userInstanceMap.get(userId).foreach { builder =>
          // Ban reasons
          if (!splitted(1).equals(No)) {
            val banReasonsByCategories: Map[UsersAutoru, Set[Reason]] =
              splitted(1)
                .split(Comma)
                .map { item =>
                  val s = item.split(Hash, 2)
                  UsersAutoru.valueOf(s(0)) -> Reason.valueOf(s(1))
                }
                .groupBy(_._1)
                .map { case (k, v) => k -> v.map(_._2).toSet }

            val opinions = Opinions.newBuilder().setVersion(1)
            banReasonsByCategories.foreach { case (category, banReasons) =>
              val domain = Model.Domain.newBuilder().setVersion(1).setUsersAutoru(category)
              val opinion =
                Model.Opinion.newBuilder().setVersion(1).setType(Opinion.Type.FAILED).addAllReasons(banReasons.asJava)
              val opinionEntry = Opinions.Entry.newBuilder().setVersion(1).setDomain(domain).setOpinion(opinion)

              opinions.addEntries(opinionEntry)
            }
            builder.setOpinions(opinions)
          }

          // Reseller updated time
          if (!splitted(3).equals(No)) {
            splitted(3)
              .split(Comma)
              .map { item =>
                val s = item.split(Hash, 2)
                val ts = ZonedDateTime.parse(s(1)).toEpochSecond * 1000
                UsersAutoru.valueOf(s(0)) -> ts
              }
              .foreach { case (category, ts) =>
                val banSignal =
                  Model.BanSignal
                    .newBuilder()
                    .setVersion(1)
                    .setDomain(Model.Domain.newBuilder().setVersion(1).setUsersAutoru(category))
                    .setTimestamp(ts)

                builder.addSignals(
                  Model.Signal.newBuilder().setVersion(1).setBanSignal(banSignal)
                )
              }
          }
        }
      }

      override def send(signal: ModerationService.Signal): F[ModerationService.SendResult] = ???

      override def send(switchOffs: ModerationService.SwitchOffs): F[ModerationService.SendResult] = ???

      override def instanceOpt(user: ModerationService.ExternalId): F[Option[Model.Instance]] = {
        userInstanceMap.get(user.userId).map(_.build()).pure[F]
      }
    }
}
