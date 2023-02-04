package ru.yandex.vertis.subscriptions.core.matcher.qbd

/** [[ru.yandex.vertis.subscriptions.core.matcher.qbd.CandidatesTree.Callback]]
  * that simply collects values called on
  */
class CollectorCallback[V] extends CandidatesTree.Callback[V] {

  private var collected = Iterable.empty[V]

  def getCollected = collected

  def apply(values: Iterable[V]) =
    collected = collected ++ values
}
