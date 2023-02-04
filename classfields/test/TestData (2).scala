package ru.yandex.vertis.subscriptions.util.test

import ru.yandex.vertis.subscriptions.DSL._
import ru.yandex.vertis.subscriptions.Model._
import com.google.protobuf.ByteString

/** Contains and produces data for testing purposes.
  */
object TestData {
  def subscription(id: String) = template(id).build

  def template(id: String) =
    Subscription
      .newBuilder()
      .setId(id)
      .setService("test")
      .setUser(userStub)
      .setRequest(Request.newBuilder().setQuery(queryStub))
      .setDelivery(deliveryStub)
      .setState(activeCurrentState)
      .setView(viewStub)

  val deliveryStub = Delivery
    .newBuilder()
    .setEmail(
      Delivery.Email
        .newBuilder()
        .setAddress("email@example.com")
        .setPeriod(Duration.newBuilder().setLength(1).setTimeUnit(TimeUnit.HOURS))
    )
    .build()

  def activeCurrentState = State.newBuilder().setValue(State.Value.ACTIVE).setTimestamp(System.currentTimeMillis())

  val viewStub = View.newBuilder().setTitle("title").setBody("body")

  val userStub = User.newBuilder().setUid("uid")

  val queryStub = and(term(point("key", "value")), term(range("range", 1d, 10d)))

  def document(id: String) =
    Document
      .newBuilder()
      .setId(id)
      .setCreateTimestamp(System.currentTimeMillis())
      .setUpdateTimestamp(System.currentTimeMillis())
      .setRawContent(ByteString.copyFromUtf8("none"))
      .addTerm(point("key", "value"))
      .addTerm(intRange("range", 1, 10))
      .build()
}
