package scandex.db.index

import scandex.core.function
import scandex.db.segments.forward.ForwardSegment
import scandex.db.segments.posting.UniquePostingList
import scandex.db.segments.values.{
  ValueEncoder,
  ValueLookupSegment,
  ValueStorageSegment,
}
import scandex.model.serde.SegmentHeader
import zio.stream.ZStream

import scala.collection.Searching.{Found, InsertionPoint}
import scala.math.Ordering
import scala.util.{Success, Try}

object MockedSegments {

  case class SingleForwardSegmentMock(array: Array[Long])
      extends ForwardSegment {

    override def valueIndexesIterator(
      documentId: DocumentId,
    ): Iterator[ValueIdx] = {
      val idx = ValueIdx(array(documentId.toInt))
      if (idx == ValueIdx.NOT_FOUND)
        Iterator.empty[ValueIdx]
      else
        Iterator(idx)
    }

    override def foldValueIndexes[Z](documentId: DocumentId)(z: Z)(
      fold: function.~~>[Z, ValueIdx, Z],
    ): Z = ???

    override def getHeader: SegmentHeader = ???

    /** @return
      *   количество документов, содержащихся в данном сегменте
      */
    override def numberOfDocuments: Long = array.length.toLong

    override def validate(header: SegmentHeader): Try[Unit] = Success(())

    override def getSerializedSize: Long = 0L

    override def serializeToStream: ZStream[Any, Throwable, Byte] =
      ZStream.empty

    override def readFrom(offset: Long, chunk: Array[Byte]): Try[Unit] = ???
  }

  case class MultiForwardSegmentMock(array: Seq[Set[Long]])
      extends ForwardSegment {

    override def valueIndexesIterator(
      documentId: DocumentId,
    ): Iterator[ValueIdx] = {
      val idxs = array(documentId.toInt).map(ValueIdx(_))
      if (idxs.contains(ValueIdx.NOT_FOUND))
        Iterator.empty[ValueIdx]
      else
        idxs.iterator
    }

    override def foldValueIndexes[Z](documentId: DocumentId)(z: Z)(
      fold: function.~~>[Z, ValueIdx, Z],
    ): Z = ???

    override def getHeader: SegmentHeader = ???

    /** @return
      *   количество документов, содержащихся в данном сегменте
      */
    override def numberOfDocuments: Long = array.length.toLong

    override def validate(header: SegmentHeader): Try[Unit] = Success(())

    override def getSerializedSize: Long = 0L

    override def serializeToStream: ZStream[Any, Throwable, Byte] =
      ZStream.empty

    override def readFrom(offset: Long, chunk: Array[Byte]): Try[Unit] = ???
  }

  case class ValueStorageSegmentMock[T](array: Array[T])
      extends ValueEncoder[T] {

    override def getByValueIdx(index: ValueIdx): T = array(index.toInt)

    override def numOfValues: Long = array.length.toLong

    override def getHeader: SegmentHeader = ???

    override def lookup(key: T): ValueIdx = ???

    override def validate(header: SegmentHeader): Try[Unit] = Success(())

    override def getSerializedSize: Long = 0L

    override def serializeToStream: ZStream[Any, Throwable, Byte] =
      ZStream.empty

    override def readFrom(offset: Long, chunk: Array[Byte]): Try[Unit] = ???
  }

  case class UniquePostingListMock(array: Array[DocumentId])
      extends UniquePostingList {
    override def getDocument(idx: ValueIdx): DocumentId = array(idx.toInt)

    override def getHeader: SegmentHeader = ???

    override def getSerializedSize: Long = ???

    override def serializeToStream: ZStream[Any, Throwable, Byte] = ???

    override def validate(header: SegmentHeader): Try[Unit] = ???

    override def readFrom(offset: Long, chunk: Array[Byte]): Try[Unit] = ???
  }

  case class LookupStorageSegmentMock[PK](array: Array[PK])(
      implicit
      ord: Ordering[PK],
  ) extends ValueLookupSegment[PK]
         with ValueStorageSegment[PK] {

    override def lookup(key: PK): ValueIdx = {
      array.search(key) match {
        case Found(foundIndex) =>
          ValueIdx(foundIndex.toLong)
        case InsertionPoint(_) =>
          ValueIdx.NOT_FOUND
      }
    }

    override def getByValueIdx(index: ValueIdx): PK = array(index.toInt)

    override def numOfValues: Long = array.length.toLong

    override def getHeader: SegmentHeader = ???

    override def validate(header: SegmentHeader): Try[Unit] = Success(())

    override def getSerializedSize: Long = 0L

    override def serializeToStream: ZStream[Any, Throwable, Byte] =
      ZStream.empty

    override def readFrom(offset: Long, chunk: Array[Byte]): Try[Unit] = ???
  }

}
