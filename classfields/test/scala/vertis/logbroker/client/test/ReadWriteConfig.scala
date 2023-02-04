package vertis.logbroker.client.test

import java.util.concurrent.ThreadLocalRandom

import zio.duration.Duration

/** @author Ratskevich Natalia reimai@yandex-team.ru
  */
case class ReadWriteConfig(
    msgsPerPartition: Int,
    partitionCount: Int,
    readQSize: Int,
    readBatchSize: Int,
    networkBlinkOnWrite: Option[Duration],
    networkBlinkOnRead: Option[Duration]) {

  val partitions: Seq[Int] = Iterator.from(0).take(partitionCount).toSeq
  val groups: Seq[Int] = partitions.map(_ + 1)
  val totalMsgs: Int = msgsPerPartition * partitionCount

  override def toString: String = {
    s"$msgsPerPartition x $partitionCount, $readBatchSize x $readQSize read buf, " +
      s"write ${networkStatus(networkBlinkOnWrite)}, " +
      s"read ${networkStatus(networkBlinkOnRead)}"
  }

  private def networkStatus(blinks: Option[Duration]): String =
    blinks match {
      case None => "is fine"
      case Some(duration) => s"blinks every ${duration.toMillis}ms"
    }
}

object ReadWriteConfig {

  def createRandom(): ReadWriteConfig = {
    val rnd = ThreadLocalRandom.current()
    val msgsPerPartition = rnd.nextInt(100) + 1
    val partitionCount = rnd.nextInt(5) + 1
    val readQSize = if (rnd.nextBoolean()) 1 else rnd.nextInt(14) + 2
    val readBatchSize = rnd.nextInt(7) + 1
    ReadWriteConfig(msgsPerPartition, partitionCount, readQSize, readBatchSize, None, None)
  }
}
