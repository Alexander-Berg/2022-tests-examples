package ru.auto.salesman.dao.user

import ru.auto.salesman.test.BaseSpec
import ru.auto.salesman.test.model.gens.user.UserModelGenerators

trait GoodsBundlesDaoSpec extends BaseSpec with UserModelGenerators {

  def newDao(): GoodsBundlesDao
}
