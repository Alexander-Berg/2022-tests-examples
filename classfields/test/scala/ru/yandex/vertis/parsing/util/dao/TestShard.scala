package ru.yandex.vertis.parsing.util.dao

/**
  * TODO
  *
  * @author aborunov
  */
class TestShard extends Shard {
  override val master: TestDatabase = new TestDatabase

  override val slave: TestDatabase = new TestDatabase

  override def index: Int = 0
}

object TestShard extends TestShard
