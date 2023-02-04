package ru.auto.cabinet.tasks.impl.tagging

import ru.auto.cabinet.service.instr.{EmptyInstr, Instr}
import ru.auto.cabinet.tasks.instrument.InstrumentedTask
import scala.concurrent.ExecutionContext.Implicits.global

trait TestInstrumented extends InstrumentedTask {

  implicit override protected def instr: Instr = new EmptyInstr("test-autoru")
}
