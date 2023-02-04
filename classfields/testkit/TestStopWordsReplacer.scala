package auto.dealers.calltracking.storage.testkit

import auto.dealers.calltracking.storage.util.Trie
import auto.dealers.calltracking.storage.StopWordsReplacerRepository
import auto.dealers.calltracking.storage.StopWordsReplacerRepository.StopWordsReplacerRepository
import auto.dealers.calltracking.storage.palma.PalmaStopWordsReplacerRepository
import zio._

object TestStopWordsReplacer {

  def make(dict: Set[String]): ULayer[StopWordsReplacerRepository] =
    ZLayer.fromEffect {
      Ref.make(Trie.fromDict(dict)).map(new PalmaStopWordsReplacerRepository(_))
    }
}
