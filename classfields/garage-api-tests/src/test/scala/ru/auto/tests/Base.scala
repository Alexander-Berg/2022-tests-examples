package ru.auto.tests

import com.google.inject.Inject
import org.junit.After
import ru.auto.tests.adaptor.GarageApiAdaptor

import scala.collection.mutable
import scala.util.Random

import scala.jdk.CollectionConverters._

trait Base {

  @Inject
  protected val adaptor: GarageApiAdaptor = null

  private val userPrefix = "qa_user"
  private val users = mutable.ArrayBuffer.empty[String]

  protected def genUser(): String = {
    val userId = s"$userPrefix:${new Random().nextInt(99999999)}"
    users += userId
    userId
  }

  @After
  def cleanup(): Unit = {
    val cards = users.map(id => id -> adaptor.getCards(id))
    cards.foreach {
      case (id, card) =>
        Option(card.getListing).foreach(a => a.asScala.foreach(card => adaptor.deleteCard(card.getId.toLong, id)))
    }
  }

}
