package vertis.palma.model

import org.scalactic.Equality
import vertis.palma.dao.model.DictionaryItem
import vertis.palma.dao.model.DictionaryItem.{EncryptedPayload, Payload, PlainPayload}

/** @author ruslansd
  */
object CustomEquality {

  implicit val DictionaryItemEquality: Equality[DictionaryItem] =
    new Equality[DictionaryItem] {

      def areEqual(a: DictionaryItem, b: Any): Boolean =
        b match {
          case d: DictionaryItem =>
            a.id == d.id &&
              a.indexes == d.indexes &&
              a.relations == d.relations &&
              a.lastUpdateInfo == d.lastUpdateInfo &&
              payloadEqual(a.payload, d.payload)
          case _ => false
        }
    }

  private def payloadEqual(a: Payload, b: Payload): Boolean =
    (a, b) match {
      case (a: EncryptedPayload, b: EncryptedPayload) =>
        a.bytes.sameElements(b.bytes) &&
          a.secretKeyId == b.secretKeyId
      case (a: PlainPayload, b: PlainPayload) =>
        a.bytes.sameElements(b.bytes) &&
          a.jsonView == b.jsonView
      case _ => false
    }
}
