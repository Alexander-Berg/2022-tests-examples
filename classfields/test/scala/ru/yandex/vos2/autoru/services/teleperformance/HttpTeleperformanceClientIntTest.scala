package ru.yandex.vos2.autoru.services.teleperformance

import org.scalatest.funsuite.AnyFunSuite
import ru.yandex.vertis.ops.test.TestOperationalSupport
import ru.yandex.vertis.tracing.Traced
import ru.yandex.vos2.AutoruModel.AutoruOffer.TeleperformanceEvent.EventResult
import ru.yandex.vos2.autoru.InitTestDbs
import ru.yandex.vos2.util.ConfigUtils._

/**
  * Created by sievmi on 02.07.18
  */

class HttpTeleperformanceClientIntTest extends AnyFunSuite with InitTestDbs {
  implicit val trace = Traced.empty
  test("new and cancel") {
    pending
    val user = components.env.props.getString("vos2-autoru.teleperformance.user")
    val password = components.env.props.getString("vos2-autoru.teleperformance.password")
    println(user + "/" + password)
    val client = new DefaultTeleperformanceClient(
      hostname = components.env.props.getString("vos2-autoru.teleperformance.host"),
      port = components.env.props.getInt("vos2-autoru.teleperformance.port"),
      scheme = components.env.props.string("vos2-autoru.teleperformance.scheme"),
      user = user,
      password = password,
      proxyConfig = components.env.serviceConfig.getConfig("ipv4.proxy"),
      TestOperationalSupport
    )

    val newInfo = TeleperformanceInfo(
      name = "sievmi",
      phoneNumber = "0000000000",
      mark = "Volkswagen",
      model = "Tiguan",
      price = "950000",
      link = s"test_link_${System.currentTimeMillis()}",
      vin = "test_vin",
      status = "new",
      minPredictprice = "950000",
      maxPredictprice = "980000",
      bucket = "version_1"
    )

    val newRes = client.sendOffer(newInfo)
    val cancelInfo = newInfo.copy(status = "cancel")

    val cancelRes = client.sendOffer(cancelInfo)

    assert(newRes.isSuccess)
    assert(newRes.get == EventResult.SUCCESS)
    assert(cancelRes.isSuccess)
    assert(cancelRes.get == EventResult.SUCCESS)
  }

}
