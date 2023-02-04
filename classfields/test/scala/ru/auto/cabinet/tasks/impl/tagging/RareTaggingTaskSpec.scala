package ru.auto.cabinet.tasks.impl.tagging

import org.mockito.ArgumentMatchers.{any, eq => argEq}
import org.mockito.Mockito._
import ru.auto.api.ApiOfferModel._
import ru.auto.api.CarsModel.CarInfo
import ru.auto.cabinet.dao.jdbc.{JdbcClientDao, JdbcKeyValueDao}
import ru.auto.cabinet.model.offer.VosOfferCategories._
import ru.auto.cabinet.service.SearcherClient
import ru.auto.cabinet.service.vos.VosClient
import ru.auto.cabinet.util.GeoIds._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/** Testing of [[RareAutoTaggingTask]]
  */
class RareTaggingTaskSpec extends TaggingTaskSpec {

  private val keyValueDao = mock[JdbcKeyValueDao]
  private val vosClient = mock[VosClient]
  private val clientsDao = mock[JdbcClientDao]
  private val searcherClient = mock[SearcherClient]
  private val CarsInMoscow = 100000
  private val CarsOnAutoru = 1000000
  private val Tesla = "TESLA"
  private val ModelS = "MODEL S"
  private val Lada = "LADA"
  private val Calina = "KALINA"
  private val TeslaOffer = carOffer("13", Tesla, ModelS, RegMoscow)
  private val LadaOffer = carOffer("12", Lada, Calina, RegMoscow)

  private val task =
    new RareAutoTaggingTask(clientsDao, keyValueDao, vosClient, searcherClient)
      with TestInstrumented

  "Rare cars tagging" should "work as expected" in {

    // set up mocks
    when(clientsDao.getActiveClientIds).thenReturn(Future(Seq(ClientId1)))
    when(
      vosClient.getOffers(
        argEq(ClientId1),
        argEq(Cars),
        any(),
        any(),
        any(),
        any(),
        any())(any()))
      .thenReturn(
        Future(
          Seq(
            LadaOffer,
            TeslaOffer
          )
        )
      )
    when(searcherClient.countOnAutoRu()).thenReturn(Future(CarsOnAutoru))
    when(searcherClient.countByRegion(RegMoscow))
      .thenReturn(Future(CarsInMoscow))

    // Lada is not rare auto
    when(searcherClient.countByMarkModelAndRegion(Lada, Calina, RegMoscow))
      .thenReturn(
        Future(
          (CarsInMoscow * RareAutoTaggingTask.MarkModelInRegionToAllCarsInRegionRareRatio + 1).toInt)
      )
    when(searcherClient.countByMarkModelOnAutoRu(Lada, Calina)).thenReturn(
      Future(
        (CarsOnAutoru * RareAutoTaggingTask.MarkModelOnAutoruToAllCarsOnAutoruRareRatio + 1).toInt)
    )

    // Tesla is rare auto
    when(searcherClient.countByMarkModelAndRegion(Tesla, ModelS, RegMoscow))
      .thenReturn(
        Future(
          (CarsInMoscow * RareAutoTaggingTask.MarkModelInRegionToAllCarsInRegionRareRatio).toInt)
      )
    when(searcherClient.countByMarkModelOnAutoRu(Tesla, ModelS)).thenReturn(
      Future(
        (CarsOnAutoru * RareAutoTaggingTask.MarkModelOnAutoruToAllCarsOnAutoruRareRatio).toInt)
    )

    when(vosClient.putTags(any(), any())(any())).thenReturn(Future(()))

    task.doTagging(None).futureValue

    verify(vosClient, times(0)).putTags(argEq(LadaOffer.getId), any())(any())
    verify(vosClient, times(1))
      .putTags(TeslaOffer.getId, Set(RareCarOnAutoru, RareCarInRegion))
  }

  def carOffer(
      offerId: String,
      mark: String,
      model: String,
      regionId: Long): Offer = {
    Offer
      .newBuilder()
      .setId(offerId)
      .setCarInfo(CarInfo.newBuilder().setMark(mark).setModel(model).build())
      .setSeller(
        Seller
          .newBuilder()
          .setLocation(
            Location.newBuilder().setGeobaseId(regionId).build()
          )
          .build()
      )
      .build()
  }
}
