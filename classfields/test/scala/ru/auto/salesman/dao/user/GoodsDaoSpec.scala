package ru.auto.salesman.dao.user

import ru.auto.salesman.test.model.gens.user.UserModelGenerators
import ru.auto.salesman.test.BaseSpec

trait GoodsDaoSpec extends BaseSpec with UserModelGenerators {

  def newDao(): GoodsDao
}
