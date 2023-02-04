package auto.c2b.lotus.model.testkit

import auto.c2b.common.testkit.Utils
import auto.c2b.lotus.model.Lot

object Differ {

  // be careful, very compilation heavy thing
  // it's better to be implemented here instead of place of usage
  // otherwise it will be compiled every time you run a test (+15 sec to build time)
  def forLot(l: Lot, r: Lot): String = ??? // Utils.caseClassDiff(l, r)

}
