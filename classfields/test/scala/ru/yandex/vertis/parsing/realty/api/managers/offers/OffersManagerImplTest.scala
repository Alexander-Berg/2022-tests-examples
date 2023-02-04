package ru.yandex.vertis.parsing.realty.api.managers.offers

import org.junit.runner.RunWith
import org.mockito.Mockito._
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.parsing.common.UrlOrHashOrId
import ru.yandex.vertis.parsing.realty.dao.mailhash.MailHashDao
import ru.yandex.vertis.parsing.realty.dao.offers.{ParsedRealtyOffersDao, QueryParams}
import ru.yandex.vertis.parsing.util.http.tracing.TracedUtils
import ru.yandex.vertis.tracing.Traced

@RunWith(classOf[JUnitRunner])
class OffersManagerImplTest extends FunSuite with MockitoSupport {
  private val mockedDao = mock[ParsedRealtyOffersDao]
  private val mockedMailHashDao = mock[MailHashDao]

  private val offersManager = new OffersManagerImpl {
    override def parsedRealtyOffersDao: ParsedRealtyOffersDao = mockedDao

    override def mailHashDao: MailHashDao = mockedMailHashDao
  }

  implicit private val trace: Traced = TracedUtils.empty

  test("empty offers list") {
    when(
      mockedDao.getParsedOffersByParams(
        eq("offers_by_encrypted_hash"),
        eq(
          QueryParams(
            id = Seq(2885)
          )
        ),
        eq(false),
        eq(false)
      )(?)
    ).thenReturn(Seq.empty)
    val result = offersManager.getOffers("ttldEnGhq1ut5ynZ3t1PfA==")
    assert(result.isEmpty)
  }

  test("hash from mail hash") {
    val hash = "ttldEnGhq1ut5ynZ3t1Pf"
    when(mockedMailHashDao.getIds(eq(hash))(?)).thenReturn(Seq(2885L))
    when(
      mockedDao.getParsedOffersByParams(
        eq("offers_by_encrypted_hash"),
        eq(
          QueryParams(
            id = Seq(2885)
          )
        ),
        eq(false),
        eq(false)
      )(?)
    ).thenReturn(Seq.empty)
    val result = offersManager.getOffers(hash)
    assert(result.isEmpty)
  }

  test("normalize published phones") {
    when(mockedDao.setPublished(?, ?, ?)(?)).thenReturn(Some(true))
    val hash = "hash"
    val offerId = "100500-pewpew"
    offersManager.setPublished(UrlOrHashOrId.Hash(hash), offerId, Seq("+79295554411"))
    verify(mockedDao).setPublished(eq(hash), eq(offerId), eq(Seq("79295554411")))(?)
  }
}
