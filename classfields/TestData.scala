package ru.yandex.vertis.chat.model

/**
  * Provides fixed data for testing.
  *
  * @author dimas
  */
object TestData {
  val Alice: User = User("Alice")
  val Bob: User = User("Bob")
  val John: User = User("John")
  val Peter: User = User("Peter")

  val Users: Set[User] = Set(Alice, Bob, John, Peter)

  val Hello: MessagePayload = "hello"
}
