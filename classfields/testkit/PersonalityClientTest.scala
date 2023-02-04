package common.clients.personality.testkit

import common.clients.personality.PersonalityClient
import common.clients.personality.model.{Address, AddressType, PersonalityError}
import common.tvm.model.UserTicket.TicketBody
import zio.{IO, ULayer, ZLayer}

object PersonalityClientTest extends PersonalityClient.Service {
  val Test: ULayer[PersonalityClient.PersonalityClient] = ZLayer.succeed(PersonalityClientTest)

  private val defaultAddress = Address(
    "home",
    "Мой адрес",
    37.618423,
    55.751244,
    Some("ул. Пушкина, дом Колотушкина"),
    Some("Россия, Москва, ул. Пушкина, дом Колотушкина")
  )

  private val workAddress = Address(
    "work",
    "Не мой адрес",
    53.150804,
    24.455918,
    Some("Садовая улица 28"),
    Some("Волковыск, Гродненская область, Беларусь, Садовая улица, 28")
  )

  override def getAddresses(userId: Long, userTicket: TicketBody): IO[PersonalityError, Seq[Address]] =
    IO.effectTotal(Seq(defaultAddress, workAddress))

  override def getAddressByType(
      userId: Long,
      userTicket: TicketBody,
      addressType: AddressType): IO[PersonalityError, Address] = addressType match {
    case AddressType.Home => IO.effectTotal(defaultAddress)
    case AddressType.Work => IO.effectTotal(workAddress)
  }
}
