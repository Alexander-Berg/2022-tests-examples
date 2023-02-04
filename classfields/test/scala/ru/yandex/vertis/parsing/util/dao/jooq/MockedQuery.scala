package ru.yandex.vertis.parsing.util.dao.jooq

import java.util.concurrent.atomic.AtomicReference

import org.jooq.tools.jdbc.MockResult

/**
  * TODO
  *
  * @author aborunov
  */
case class MockedQuery(
    expectedQuery: AtomicReference[(Seq[String], Seq[AnyRef])] = new AtomicReference((Seq.empty, Seq.empty)),
    providedResult: AtomicReference[Seq[MockResult]] = new AtomicReference(Seq.empty)
)
