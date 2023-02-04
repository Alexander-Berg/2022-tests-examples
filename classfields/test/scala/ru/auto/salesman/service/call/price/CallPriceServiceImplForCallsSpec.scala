package ru.auto.salesman.service.call.price

import ru.auto.salesman.Task
import ru.auto.salesman.model.{OfferMark, ProductId}

class CallPriceServiceImplForCallsSpec
    extends CallPriceServiceImplSpec[
      ProductId.Call.type,
      Task[List[OfferMark]]
    ] {}
