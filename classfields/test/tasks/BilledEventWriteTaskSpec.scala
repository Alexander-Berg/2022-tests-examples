package ru.yandex.vertis.billing.tasks

import java.util.NoSuchElementException
import org.scalatest.Assertion
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.billing.dao.BilledEventDao.ModifiedSinceBatched
import ru.yandex.vertis.billing.dao.CallCenterCallDao.ByIds
import ru.yandex.vertis.billing.dao.impl.jdbc.{JdbcKeyValueDao, JdbcSpecTemplate}
import ru.yandex.vertis.billing.dao.{
  BilledEventDivisionDaoResolver,
  BindingDao,
  CallCenterCallDao,
  CampaignCallDao,
  OrderDao
}
import ru.yandex.vertis.billing.model_core.Division.Components
import ru.yandex.vertis.billing.model_core.EventStat.{
  CallRawEventDetail,
  CommonRawEventDetail,
  Details,
  RawEventDetails
}
import ru.yandex.vertis.billing.model_core.gens.{
  extendedCampaignCallFactGen,
  orderTransactionGen,
  withdrawWithRawEventDetails,
  AsRandomCall,
  AsRevenue,
  BindingGen,
  CallCenterCallGen,
  OrderTransactionGenParams,
  Producer
}
import ru.yandex.vertis.billing.model_core.BilledEventInfo.ServiceObjectLinkedCall
import ru.yandex.vertis.billing.model_core.callcenter.CallCenterCall
import ru.yandex.vertis.billing.model_core.{
  BilledEventDivision,
  BilledEventInfo,
  Binding,
  BindingFilter,
  CallFactHeader,
  CostPerCall,
  CostPerClick,
  CostPerIndexing,
  Division,
  Epoch,
  EpochWithId,
  ExtendedCampaignCallFact,
  FixPrice,
  Funds,
  Highlighting,
  OrderTransaction,
  OrderTransactions,
  Product,
  SupportedBilledEventDivisions,
  Withdraw2
}
import ru.yandex.vertis.billing.service.OrderService.GetTransactionFilter
import ru.yandex.vertis.billing.service.TypedKeyValueService
import ru.yandex.vertis.billing.service.impl.TypedKeyValueServiceImpl
import ru.yandex.vertis.billing.service.metered.MeteredStub
import ru.yandex.vertis.billing.tasks.BilledEventWriteTaskSpec.{
  convert,
  genTransactions,
  spoilTransactionByIncrease,
  updateTransactions,
  TransactionCallInfo,
  TransactionWithInfo,
  UnsupportedBilledEventDivisionsValues
}
import ru.yandex.vertis.billing.util.DateTimeUtils
import ru.yandex.vertis.mockito.MockitoSupport

import scala.math.max
import scala.util.{Random, Success}

class BilledEventWriteTaskSpec extends AnyWordSpec with Matchers with JdbcSpecTemplate with MockitoSupport {

  private val keyValueDao = new JdbcKeyValueDao(billingDatabase)

  def genTransactionWithInfo(component: String): Iterable[TransactionWithInfo] = {
    val trs = genTransactions(component)
    trs.map {
      case w @ Withdraw2(_, _, _, Some(RawEventDetails(items)), _) =>
        val curItemFacts = items.map {
          case c: CallRawEventDetail =>
            val curCallfact = extendedCampaignCallFactGen().next
            val curCallfactId = CallFactHeader(curCallfact.fact.call).identity
            val callCenterCall = curCallfact.fact.callCenterCallId.map { c =>
              CallCenterCallGen.next.copy(id = c)
            }

            val callInfo = TransactionCallInfo(curCallfact, callCenterCall)
            (c.copy(callFactId = curCallfactId), Some(callInfo))
          case other =>
            (other, None)
        }
        val (curItems, trInfo) = curItemFacts.unzip
        val changedTr = w.copy(details = Some(RawEventDetails(curItems)))

        val binding = BindingGen.next
        val changedBinding = binding.copy(point = binding.point.copy(campaignId = w.snapshot.campaignId))

        TransactionWithInfo(changedTr, Some(changedBinding), trInfo.flatten.headOption)
      case other =>
        TransactionWithInfo(other, None, None)
    }
  }

  case class TestContext(
      billedEventDaoResolver: BilledEventDivisionDaoResolver,
      task: BilledEventWriteTask,
      typedKeyValueService: TypedKeyValueService)

  private def prepare(domain: String, trsWithInfo: Iterable[TransactionWithInfo]): TestContext = {
    val typedKeyValueService = new TypedKeyValueServiceImpl(keyValueDao)
    typedKeyValueService.set("billed-event-task", EpochWithId(0L, None)).get

    val billedEventDaoResolver = BilledEventDivisionDaoResolver.forSupported(
      eventStorageDatabase,
      eventStorageDatabase
    )

    val factsWithCallCenterCalls = trsWithInfo.flatMap(_.callInfo)
    val facts = factsWithCallCenterCalls.map(_.expandedCallFact)

    val callFactDaoMock = mock[CampaignCallDao]
    when(callFactDaoMock.getExtendedCampaignCallFact(?)).thenReturn(Success(facts))

    val callCenterCalls = factsWithCallCenterCalls.flatMap(_.callCenterCall)
    val callCenterCallMock = mock[CallCenterCallDao]
    when(callCenterCallMock.getTry(ByIds(?))).thenReturn(Success(callCenterCalls))

    val orderDao = {
      val dao = mock[OrderDao]

      var source = trsWithInfo.map(_.tr).grouped(100).toSeq

      stub(dao.getTransactions(_: GetTransactionFilter, _: Boolean)) { case _ =>
        if (source.isEmpty) {
          Success(Iterable.empty)
        } else {
          val result = Success(source.head)
          source = source.tail
          result
        }
      }

      dao
    }

    val bindingMock = {
      val m = mock[BindingDao]

      val bindings = trsWithInfo.flatMap(_.binding)

      stub(m.get _) {
        case BindingFilter.ForCampaigns(ids) =>
          val valuable = bindings.filter(b => ids.contains(b.point.campaignId))
          Success(valuable)
        case _ =>
          Success(Iterable.empty)
      }
      m
    }

    val billedEventWriteTask = new BilledEventWriteTask(
      domain,
      billedEventDaoResolver,
      typedKeyValueService,
      orderDao,
      callFactDaoMock,
      bindingMock,
      callCenterCallMock
    ) with MeteredStub
    TestContext(billedEventDaoResolver, billedEventWriteTask, typedKeyValueService)
  }

  private def check(
      division: BilledEventDivision,
      trsWithInfo: Iterable[TransactionWithInfo],
      epoch: Epoch): Assertion = {
    val context = prepare(division.project, trsWithInfo)
    val billedEventDao = context.billedEventDaoResolver.resolve(division).get
    context.task.execute().get

    val filteredTransactions = trsWithInfo.collect {
      case t @ TransactionWithInfo(Withdraw2(_, _, _, Some(RawEventDetails(_)), _), _, _) => t
    }

    val expected = filteredTransactions.flatMap(convert)
    val actual = billedEventDao.get(ModifiedSinceBatched(0L, None, expected.size)).get
    val actualNormalized = actual.map(_.copy(epoch = None))
    expected should contain theSameElementsAs actualNormalized

    val last = trsWithInfo.last.tr
    val expectedEpoch = EpochWithId(last.epoch.get, Some(last.id))
    val actualEpoch = context.typedKeyValueService.get[EpochWithId]("billed-event-task").get
    expectedEpoch shouldBe actualEpoch
  }

  "BilledEventWriteTaskSpec" should {
    "work correctly with update" in {
      var epoch = DateTimeUtils.now().getMillis
      SupportedBilledEventDivisions.Values.foreach { division =>
        val trsWithInfo = genTransactionWithInfo(division.component)
        check(division, trsWithInfo, epoch)
        check(division, updateTransactions(trsWithInfo, 1), epoch)
        epoch = DateTimeUtils.now().getMillis
      }
    }
    "failed with unexpected revenue state" in {
      SupportedBilledEventDivisions.Values.foreach { division =>
        val trsWithInfo = genTransactionWithInfo(division.component)
        intercept[IllegalStateException] {
          check(division, trsWithInfo.map(spoilTransactionByIncrease), 0L)
        }
      }
    }
    "failed with unsupported divisions" in {
      UnsupportedBilledEventDivisionsValues.foreach { division =>
        intercept[NoSuchElementException] {
          check(division, Seq.empty, 0L)
        }
      }
    }
  }

}

object BilledEventWriteTaskSpec {

  case class TransactionCallInfo(expandedCallFact: ExtendedCampaignCallFact, callCenterCall: Option[CallCenterCall])

  case class TransactionWithInfo(tr: OrderTransaction, binding: Option[Binding], callInfo: Option[TransactionCallInfo])

  private def convert(tr: TransactionWithInfo): Iterable[BilledEventInfo] = tr match {
    case TransactionWithInfo(w: Withdraw2, binding, info) =>
      w.details match {
        case Some(RawEventDetails(items)) =>
          val trWithoutDetails = w.copy(details = None)
          val (_, events) = items.foldLeft((w.amount, Seq.empty[BilledEventInfo])) { case ((rest, seq), i) =>
            i match {
              case CommonRawEventDetail(id, offerId, Some(revenue)) =>
                val curRest = max(rest - revenue, 0)
                val e = BilledEventInfo(
                  Some(id),
                  offerId,
                  None,
                  revenue,
                  rest - curRest,
                  trWithoutDetails,
                  None
                )
                (curRest, seq :+ e)
              case CallRawEventDetail(id, callFactId, revenue) =>
                val curRest = max(rest - revenue, 0)
                val callFact = info.map(_.expandedCallFact).get
                val callCenterCall = info.flatMap(_.callCenterCall)
                val linkedCall = ServiceObjectLinkedCall(callFact, binding.map(_.point.offerId), callCenterCall)
                val e = BilledEventInfo(
                  id,
                  None,
                  Some(callFactId),
                  revenue,
                  rest - curRest,
                  trWithoutDetails,
                  Some(linkedCall)
                )
                (curRest, seq :+ e)
              case _ =>
                throw new IllegalArgumentException("Unexpected")
            }
          }
          events
        case _ =>
          Iterable.empty
      }
    case _ =>
      Iterable.empty
  }

  private def randomAmount(details: Details): Funds = {
    details match {
      case RawEventDetails(items) =>
        val count = Random.nextInt(items.size + 1)
        val prefixSum = items
          .take(count)
          .collect { case CommonRawEventDetail(_, _, revenueOpt) =>
            revenueOpt.get
          }
          .sum
        val aim = items
          .drop(count)
          .headOption
          .map {
            case CommonRawEventDetail(_, _, revenueOpt) =>
              revenueOpt.get
            case _ =>
              0L
          }
          .getOrElse(0L)
          .toInt

        prefixSum + Random.nextInt(aim + 1)
      case _ => 0L
    }
  }

  private val WithdrawWithOldDetailsGen = {
    orderTransactionGen(OrderTransactionGenParams().withType(OrderTransactions.Withdraw)).map {
      case w: Withdraw2 => w
      case other => throw new IllegalArgumentException(s"Unexpected $other")
    }
  }

  private def genTransactions(component: String): Seq[Withdraw2] = {
    val price = FixPrice(100L)
    val cost = Components.withName(component) match {
      case Components.Click =>
        CostPerClick(price)
      case Components.PhoneShow =>
        CostPerCall(price)
      case Components.Indexing =>
        CostPerIndexing(price)
      case other =>
        throw new IllegalArgumentException(s"Unexpected $other")
    }

    val source = Random.shuffle(
      Seq(
        withdrawWithRawEventDetails(AsRevenue).next(50),
        withdrawWithRawEventDetails(AsRandomCall).next(50),
        WithdrawWithOldDetailsGen.next(50)
      ).flatten
    )

    var curSource = DateTimeUtils.now().getMillis

    source.map { t =>
      val curTr = t.copy(
        snapshot = t.snapshot.copy(product = Product(Highlighting(cost))),
        epoch = Some(curSource),
        amount = t.details.map(randomAmount).getOrElse(t.amount)
      )
      curSource = curSource + 1
      curTr
    }
  }

  private def changeDetailsAmount(details: Details, amountChange: Int): Option[RawEventDetails] = {
    details match {
      case RawEventDetails(items) =>
        val changed = items.map {
          case c: CommonRawEventDetail =>
            c.copy(revenue = c.revenue.map(_ + 1))
          case c: CallRawEventDetail =>
            c.copy(revenue = c.revenue + 1)
        }
        Some(RawEventDetails(changed))
      case _ =>
        None
    }
  }

  private def updateTransactions(
      trsWithInfo: Iterable[TransactionWithInfo],
      detailsAmountChange: Int): Iterable[TransactionWithInfo] = {
    trsWithInfo.map {
      case t @ TransactionWithInfo(w: Withdraw2, _, _) =>
        val changedDetails = w.details.flatMap(changeDetailsAmount(_, detailsAmountChange))
        val changedAmount = changedDetails.map(randomAmount).getOrElse(w.amount)
        val changedTr = w.copy(
          details = changedDetails,
          amount = changedAmount
        )
        t.copy(tr = changedTr)
      case t =>
        t
    }
  }

  private def spoilTransactionByIncrease(trsWithInfo: TransactionWithInfo): TransactionWithInfo = {
    trsWithInfo.tr match {
      case w: Withdraw2 =>
        val changedTr = w.details match {
          case Some(RawEventDetails(items)) =>
            val sum = items.map {
              case c: CommonRawEventDetail => c.revenue.get
              case c: CallRawEventDetail => c.revenue
            }.sum

            w.copy(amount = sum + 1)
          case _ =>
            w
        }
        trsWithInfo.copy(tr = changedTr)
      case _ =>
        trsWithInfo
    }
  }

  import Division.Components._
  import Division.Locales._
  import Division.Projects._

  private val UnsupportedBilledEventDivisionsValues = Seq(
    BilledEventDivision("test", Ru.toString, Click.toString)
  )

}
