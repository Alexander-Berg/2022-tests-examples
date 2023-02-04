package ru.auto.salesman.tasks.kafka.processors

import org.joda.time.DateTime
import ru.auto.cabinet.DealerAutoru.Dealer
import ru.auto.salesman.client.cabinet.CabinetClient
import ru.auto.salesman.client.cabinet.model.BalanceOrder
import ru.auto.salesman.tasks.kafka.processors.impl.NewDealerUpdatesProcessorImpl
import ru.auto.salesman.test.BaseSpec

class NewDealerUpdatesProcessorImplSpec extends BaseSpec {

  import NewDealerUpdatesProcessorImplSpec._

  private val cabinetClient = mock[CabinetClient]

  val processor = new NewDealerUpdatesProcessorImpl(cabinetClient)

  "NewDealerUpdatesProcessorImpl" should {

    "create balance order through cabinet" in {
      (cabinetClient.createBalanceOrder _)
        .expects(clientId, true)
        .returningZ(balanceOrder)

      processor.process(dealer).success.value
    }

  }

}

object NewDealerUpdatesProcessorImplSpec {

  private val clientId = 100500

  private val dealer = Dealer.newBuilder().setId(clientId).build()

  private val now = DateTime.now()

  private val balanceOrder = BalanceOrder(
    15006630L,
    99L,
    1L,
    7320375L,
    175L,
    0L,
    0L,
    1L,
    now,
    now,
    "Технические услуги в отношении Объявлений Заказчика в соответствии с «Условиями оказания услуг на сервисе Auto.ru» msk7471",
    0L,
    now,
    now
  )

}
