package ru.yandex.vertis.vsquality.techsupport.service

import cats.syntax.applicative._
import org.scalacheck.{Arbitrary, Prop}
import org.scalatestplus.scalacheck.Checkers
import ru.yandex.vertis.vsquality.techsupport.clients.impl.HttpJivositeClient
import ru.yandex.vertis.vsquality.techsupport.service.impl.ExternalTechsupportServiceImpl
import ru.yandex.vertis.vsquality.techsupport.util.SpecBase
import ru.yandex.vertis.vsquality.utils.cats_utils.Awaitable._

/**
  * @author devreggs
  */
class ExternalTechsupportServiceSpec extends SpecBase {

  import ru.yandex.vertis.vsquality.techsupport.Arbitraries._
  import ru.yandex.vertis.vsquality.techsupport.CoreArbitraries._

  private val httpJivositeClient: HttpJivositeClient[F] = mock[HttpJivositeClient[F]]

  private val externalTechsupportServiceImpl = new ExternalTechsupportServiceImpl[F](httpJivositeClient)

  "JivositeService" should {
    "sends requests" in {
      Checkers.check(Prop.forAll(implicitly[Arbitrary[ExternalTechsupportService.Envelope]].arbitrary) {
        envelope: ExternalTechsupportService.Envelope =>
          stub(httpJivositeClient.request(_, _)) { case (_, _) => ().pure[F] }
          externalTechsupportServiceImpl.send(envelope).await
          true
      })
    }
  }
}
