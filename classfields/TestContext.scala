package ru.auto.chatbot.app

import akka.actor.ActorSystem
import com.typesafe.config.Config
import org.apache.curator.framework.CuratorFramework
import ru.auto.chatbot.extdata.ChatBotDataTypes
import ru.auto.chatbot.model._
import ru.auto.chatbot.model.questions.BunkerQuestionsVariations
import ru.auto.chatbot.prometheus.Operational
import ru.auto.chatbot.utils.StateWrapper
import ru.yandex.vertis.baker.util.extdata.{EdsDataEngine, ExtDataEngine}
import ru.yandex.vertis.s3edr.core.s3.{S3Auth, S3Settings}
import scalikejdbc.ConnectionPool

import scala.concurrent.ExecutionContext
import scala.concurrent.forkjoin.ForkJoinPool

/**
  * Created by Karpenko Maksim (knkmx@yandex-team.ru) on 2019-03-05.
  */
object TestContext {

  implicit val system: ActorSystem = ActorSystem(env.serviceName, env.config)

  lazy val env = new TestEnvironment

  lazy val operational: Operational =
    Operational.default(env.environmentType, env.dataCenter, env.hostName)(env.componentName)
  val config: Config = env.config.getConfig("autoru-chat-bot")
  dbInit()

  lazy val zkCommonClient: CuratorFramework = {
    val connectString = config.getString("zookeeper.connect-string")
    val namespace = env.serviceName + "/chatbot"
    DefaultComponents.zkCommonClient(connectString, namespace)
  }

  lazy val zkClient: CuratorFramework = {
    zkCommonClient.usingNamespace(zkCommonClient.getNamespace + "/" + env.componentName)
  }

  private val (s3Settings, s3prefix) = {
    val config = env.config.getConfig("auto.s3edr")
    val settings = S3Settings(
      url = config.getString("s3.url"),
      auth = S3Auth(
        key = config.getString("s3.auth.key"),
        secret = config.getString("s3.auth.secret")
      ),
      bucket = config.getString("s3.bucket")
    )

    (settings, config.getString("key-prefix"))
  }

  ChatBotDataTypes.rawBunker

  protected val extDataEngine: ExtDataEngine = EdsDataEngine.create(
    env.dataPath,
    s3Settings,
    s3prefix,
    operational.prometheusRegistry,
    ExecutionContext.fromExecutor(new ForkJoinPool(2)),
    Seq(ChatBotDataTypes.rawBunker)
  )

  lazy val bunkerMessages: BunkerMessages = BunkerMessages.from(extDataEngine)
  lazy val bunkerButtons: BunkerButtons = BunkerButtons.from(extDataEngine)
  lazy val bunkerQuestionsVariations: BunkerQuestionsVariations = BunkerQuestionsVariations.from(extDataEngine)
  lazy val bunkerEmailDescriptions: BunkerEmailDescriptions = BunkerEmailDescriptions.from(extDataEngine)

  lazy val stateWrapper: StateWrapper = new StateWrapper(bunkerQuestionsVariations, bunkerEmailDescriptions)

  def dbInit(): Unit = {
    val dbUrl = env.config.getString("db.default.url") + s"&sslrootcert=${getClass.getResource("/root.crt").getPath}"
    val dbUser = env.config.getString("db.default.user")
    val dbPassword = env.config.getString("db.default.password")

    ConnectionPool.singleton(dbUrl, dbUser, dbPassword)
  }
}
