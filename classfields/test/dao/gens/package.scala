package ru.auto.salesman.test.dao

import org.joda.time.DateTime
import org.scalacheck.Gen
import ru.auto.api.ApiOfferModel.Section
import ru.auto.salesman.client.VosClient.{
  AddServicesResult,
  ServicesAdded,
  ServicesNotAdded
}
import ru.auto.salesman.dao.GoodsDao
import ru.auto.salesman.dao.GoodsDao.Filter.RecentFilter
import ru.auto.salesman.dao.GoodsDao.Record
import ru.auto.salesman.dao.OfferDao.OfferPatch
import ru.auto.salesman.dao.user.UserExclusionsDao.UserExclusion
import ru.auto.salesman.model.OfferStatuses.OfferStatus
import ru.auto.salesman.model._
import ru.auto.salesman.model.cashback.CashbackPeriod
import ru.auto.salesman.model.user.product.{AutoruProduct, Products}
import ru.auto.salesman.test.model.gens._
import ru.auto.salesman.util.{Page, Range, Slice, SlicedResult}
import ru.yandex.vertis.generators.BasicGenerators.bool
import ru.yandex.vertis.generators.DateTimeGenerators._
import ru.yandex.vertis.util.time.DateTimeUtil

package object gens {

  def goodRecordGen(
      offerIdGen: Gen[OfferId] = Gen.posNum[OfferId],
      offerHashGen: Gen[OfferHash] = Gen.alphaStr,
      offerCategoryGen: Gen[OfferCategory] = OfferCategoryGen,
      offerSectionGen: Gen[Section] = OfferSectionGen,
      productGen: Gen[ProductId] = GoodsNameGen,
      idGen: Gen[GoodsId] = Gen.posNum[GoodsId],
      clientIdGen: Gen[ClientId] = Gen.posNum[ClientId],
      goodStatusGen: Gen[GoodStatus] = GoodsStatusGen,
      firstActivateDateGen: Gen[FirstActivateDate] = FirstActivateDate(
        DateTimeUtil.now().minusDays(3)
      ),
      offerBilling: Gen[Option[Array[Byte]]] =
        Gen.option(Array.range(1, 10).map(_.toByte)),
      offerBillingDeadlineGen: Gen[DateTime] = DateTimeUtil.now().minusMinutes(2)
  ): Gen[Record] =
    for {
      primaryKeyId <- idGen
      offerId <- offerIdGen
      offerHash <- offerHashGen
      category <- offerCategoryGen
      section <- offerSectionGen
      clientId <- clientIdGen
      product <- productGen
      offerBillingDeadline <- offerBillingDeadlineGen
      status <- goodStatusGen
      createDate = DateTimeUtil.now()
      expireDate = DateTimeUtil.now().minusDays(2)
      firstActivateDate <- firstActivateDateGen
      offerBilling <- offerBilling
      holdTransactionId <- Gen.option(Gen.alphaStr)
      epoch <- Gen.posNum[Long]
    } yield
      GoodsDao.Record(
        primaryKeyId,
        offerId,
        offerHash,
        category,
        section,
        clientId,
        product,
        status,
        createDate,
        "",
        Some(expireDate),
        firstActivateDate,
        offerBilling,
        Some(offerBillingDeadline),
        holdTransactionId,
        Some(epoch)
      )

  val GoodRecordGen: Gen[GoodsDao.Record] = goodRecordGen()

  def carsNewActiveGoodGen(
      offerIdGen: Gen[OfferId] = Gen.posNum[OfferId],
      productGen: Gen[ProductId] = GoodsNameGen,
      idGen: Gen[GoodsId] = Gen.posNum[GoodsId],
      clientIdGen: Gen[ClientId] = Gen.posNum[ClientId],
      offerBilling: Gen[Option[Array[Byte]]] =
        Gen.option(Array.range(1, 10).map(_.toByte))
  ): Gen[GoodsDao.Record] =
    goodRecordGen(
      offerIdGen = offerIdGen,
      offerCategoryGen = OfferCategories.Cars,
      offerSectionGen = Section.NEW,
      productGen = productGen,
      idGen = idGen,
      clientIdGen = clientIdGen,
      goodStatusGen = GoodStatuses.Active,
      offerBilling = offerBilling
    )

  def clientDetailsGen(
      clientIdGen: Gen[ClientId] = Gen.posNum[ClientId],
      balanceAgencyIdGen: Gen[Option[BalanceClientId]] =
        Gen.option(Gen.posNum[BalanceClientId]),
      regionIdGen: Gen[RegionId] = regionIdGen,
      singlePaymentsGen: Gen[Set[AdsRequestType]] =
        Gen.listOfN(2, adsRequestTypeGen).map(_.toSet),
      paidCallsAvailableGen: Gen[Boolean] = bool
  ): Gen[DetailedClient] =
    for {
      clientId <- clientIdGen
      agencyId <- Gen.option(Gen.posNum[AgencyId])
      balanceClientId <- Gen.posNum[Long]
      balanceAgencyId <- balanceAgencyIdGen
      categorizedClientId <- Gen.posNum[Long]
      companyId <- Gen.posNum[Long]
      regionId <- regionIdGen
      cityId <- cityIdGen
      accountId <- Gen.posNum[Long]
      isActive <- Gen.oneOf(true, false)
      firstModerated <- Gen.oneOf(true, false)
      singlePayments <- singlePaymentsGen
      paidCallsAvailable <- paidCallsAvailableGen
    } yield
      DetailedClient(
        clientId,
        agencyId,
        balanceClientId,
        balanceAgencyId,
        Some(categorizedClientId),
        Some(companyId),
        regionId,
        cityId,
        accountId,
        isActive,
        firstModerated,
        singlePayments,
        paidCallsAvailable = paidCallsAvailable
      )

  val ClientDetailsGen: Gen[DetailedClient] = clientDetailsGen()

  def callClientGen(
      clientIdGen: Gen[ClientId] = Gen.posNum[ClientId],
      regionIdGen: Gen[RegionId] = regionIdGen
  ): Gen[DetailedClient] =
    clientDetailsGen(
      clientIdGen = clientIdGen,
      regionIdGen = regionIdGen,
      paidCallsAvailableGen = true
    )

  val RecentFilterGen: Gen[RecentFilter] = for {
    since <- Gen.posNum[Epoch]
    excludedProducts <- Gen.listOf(ProductIdGen)
    status <- Gen.option(GoodsStatusGen)
  } yield RecentFilter(since, excludedProducts, status)

  val addServicesResultGen: Gen[AddServicesResult] =
    Gen.oneOf(
      Gen.const(ServicesAdded),
      Gen.option(Gen.alphaStr).map(ServicesNotAdded)
    )

  val rangeGen: Gen[Range] = for {
    min <- Gen.choose(0, 100)
    max <- Gen.choose(101, 200)
  } yield Range(min, max)

  val pageGen: Gen[Page] = for {
    number <- Gen.posNum[Int]
    size <- Gen.posNum[Int]
  } yield Page(number, size)

  val sliceGen: Gen[Slice] = Gen.oneOf(rangeGen, pageGen)

  def slicedResultGen[A](gen: Gen[A]): Gen[SlicedResult[A]] =
    for {
      slice <- sliceGen
      values <- Gen.listOfN(2, gen) // 2, otherwise generation is too slow
      total <- Gen.posNum[Int]
    } yield SlicedResult(values, total, slice)

  def offerPatchGen(
      statusGen: Gen[Option[OfferStatus]] = Gen.option(OfferStatusGen)
  ): Gen[OfferPatch] =
    for {
      expireDate <- Gen.option(dateTimeInFuture())
      status <- statusGen
      setDate <- Gen.option(dateTimeInPast)
      freshDate <- Gen.option(dateTimeInPast)
    } yield OfferPatch(expireDate, status, setDate, freshDate)

  val activationOfferPatchGen: Gen[OfferPatch] =
    offerPatchGen(statusGen = Some(OfferStatuses.Show))

  val nonActivationOfferPatchGen: Gen[OfferPatch] =
    offerPatchGen(
      statusGen = Gen.option(
        Gen.oneOf(OfferStatuses.values.filter(_ != OfferStatuses.Show).toSeq)
      )
    )

  def cashbackPeriodIdGen: Gen[PeriodId] =
    for { id <- Gen.posNum[Long] } yield PeriodId(id)

  def cashbackPeriodGen(
      periodIdGen: Gen[PeriodId] = cashbackPeriodIdGen,
      previousPeriodIdGen: Gen[PeriodId] = cashbackPeriodIdGen,
      isActiveGen: Gen[Boolean] = bool
  ): Gen[CashbackPeriod] =
    for {
      id <- periodIdGen
      start <- dateTimeInPast
      finish <- dateTimeInPast()
      isActive <- isActiveGen
      previousPeriodGen <- Gen.option(previousPeriodIdGen)
    } yield
      CashbackPeriod(
        id = id,
        start = start,
        finish = finish,
        isActive = isActive,
        previousPeriod = previousPeriodGen
      )

  def userExclusionsGen(
      testDataSize: Int = 20,
      allowedProducts: Set[AutoruProduct] = Products.all(DeprecatedDomains.AutoRu)
  ): Gen[List[UserExclusion]] =
    for {
      products <- Gen.listOfN(
        testDataSize,
        Gen.oneOf(allowedProducts.toSeq)
      )
    } yield
      products.zipWithIndex.map { case (productId, userId) =>
        UserExclusion(AutoruUser(userId), productId)
      }
}
