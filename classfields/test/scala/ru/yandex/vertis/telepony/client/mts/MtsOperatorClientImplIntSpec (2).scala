package ru.yandex.vertis.telepony.client.mts

import org.scalatest.Ignore
import ru.yandex.vertis.telepony.IntegrationSpecTemplate
import ru.yandex.vertis.telepony.SampleHelper._
import ru.yandex.vertis.telepony.client.OperatorClientSpec
import ru.yandex.vertis.telepony.model.mts.{IvrMenu, VoiceMenu}
import ru.yandex.vertis.telepony.model.{Operator, Operators, Phone}
import ru.yandex.vertis.telepony.service.MtsClient.MenuValue
import ru.yandex.vertis.telepony.service.OperatorClient
import ru.yandex.vertis.telepony.service.impl.mts.MtsOperatorClient
import ru.yandex.vertis.telepony.util.Threads

import scala.concurrent.duration._
import scala.io.Source
import scala.util.Random

/**
  * @author neron
  */
@Ignore
class MtsOperatorClientImplIntSpec extends OperatorClientSpec with IntegrationSpecTemplate {

  private val voiceMenu = {
    val menuXml =
      Source
        .fromInputStream(
          getClass.getClassLoader.getResourceAsStream("mts/empty.xml")
        )
        .mkString
    VoiceMenu(MenuValue(menuXml), Seq.empty)
  }

  private lazy val realMtsClient = MtsClientImplIntSpec.createMtsClient(materializer)

  private lazy val allPhones = realMtsClient.clientV4.getUniversalNumbers().futureValue.toSeq

  override def operatorClient: OperatorClient =
    new MtsOperatorClient(
      mtsClient = realMtsClient,
      crm = None,
      voiceMenu = voiceMenu,
      ivrMenu = IvrMenu(Nil),
      makeCallTime = 29.seconds,
      maxQueueTime = 30.seconds,
      channels = 1,
      masterOpt = mtsDomainSettings.masterPhone,
      dynamicProperties = dynamicProperties
    )(Threads.lightWeightTasksEc)

  override def operator: Operator = Operators.Mts

  override def existingPhone: Phone = allPhones(Random.nextInt(allPhones.size))
}
