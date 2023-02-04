package auto.c2b.common.testkit

import shapeless.ops.hlist.{Mapper, ToList, Zip}
import shapeless.{::, Generic, HList, HNil, Poly1}

object Utils {

  /** Case class diff
   *
   * println {
   *   auto.c2b.common.testkit.Utils.caseClassDiff(lot, newLot)
   * }
   * Output:
   * Lot diff:
   * id -> [2367300231690044131 != 26]
   * status -> [WaitingInspection != New]
   * evaluationSheet -> [Some(http://s3.yandex.ru/f333c8b8-0b30-4d10-8c1c-305261be2eb7) != None]
   * inspectionSheet -> [Some(http://s3.yandex.ru/1120ffc5-1473-47d9-934a-d516146e4a64) != None]
   * createdAt -> [2004-04-12T04:09:56.961759435Z != 2022-06-09T10:51:58.282922Z]
   * startAt -> [2037-06-13T03:17:51.122923220Z != 2037-06-13T03:17:51.122923Z]
   * clientId -> [Some(37189c10-cc5c-4d06-aace-411549b22889) != None]
   * userId -> [Some(4e931ba3-53b1-4be9-9f85-a7bb402897e8) != None]
   * bidsCount -> [1702668575 != 0]
   * buyOutAlg -> [WithPreOffers != Auction]
   * topPropositions -> [Vector(Proposition(fsdfd,2559477592645000277,UnknownFieldSet(Map()))) != ArraySeq()]
   */
  def caseClassDiff[P1 <: Product, P2 <: Product, H1 <: HList, H2 <: HList, Zipped <: HList, Mapped <: HList](
      p1: P1,
      p2: P2
    )(implicit
      genP1: Generic.Aux[P1, H1],
      genP2: Generic.Aux[P2, H2],
      zipper: Zip.Aux[H1 :: H2 :: HNil, Zipped],
      mapper: Mapper.Aux[filterEqual.type, Zipped, Mapped],
      toList: ToList[Mapped, List[(Any, Any)]]): String = {
    if (p1.productElementNames.sameElements(p2.productElementNames)) {
      val h1 = genP1.to(p1)
      val h2 = genP2.to(p2)
      val diff = h1.zip(h2).map(filterEqual).toList
      val withNames = p1.productElementNames.zip(diff)
      val diffString = withNames.flatMap { case (name, values) =>
        values.map(t => s"$name -> [${t._1} != ${t._2}]\n")
      }.mkString
      s"${p1.productPrefix} diff: \n$diffString"
    } else "Incomparable"
  }

  private object filterEqual extends Poly1 {

    implicit def default[T] =
      at[(T, T)](t => if (t._1 == t._2) List.empty[(T, T)] else List(t))
  }
}
