package ru.yandex.realty.componenttest.data.mortgageprograms

import ru.yandex.realty.componenttest.data.banks.Bank_322371
import ru.yandex.realty.componenttest.data.utils.ComponentTestDataUtils.extractIdFromClassName
import ru.yandex.realty.model.message.Mortgages.MortgageProgram

import scala.collection.JavaConverters._

object MortgageProgram_2418785 {

  val Id: Long = extractIdFromClassName(getClass)

  val Bank: Bank_322371.type = Bank_322371

  val Proto: MortgageProgram =
    Bank.Proto.getMortgageProgramList.asScala
      .filter(_.getId == Id)
      .head

}
