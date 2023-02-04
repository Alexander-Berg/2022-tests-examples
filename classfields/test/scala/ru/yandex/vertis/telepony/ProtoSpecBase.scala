package ru.yandex.vertis.telepony

import org.scalatest.matchers.should.Matchers
import ru.yandex.vertis.telepony.proto.ProtoConversion

/**
  * @author neron
  */
trait ProtoSpecBase { t: Matchers =>

  def test[M, P](model: M, c: ProtoConversion[M, P]): Unit = {
    val actualProto = c.to(model)
    val actualModel = c.from(actualProto)
    actualModel shouldEqual model
    val actualProtoAgain = c.to(actualModel)
    actualProtoAgain shouldEqual actualProto
  }

}
