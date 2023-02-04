package ru.yandex.realty.mocks.yt

import java.util.Optional
import java.util.function.{Consumer, Function => JFunction}
import java.{lang, util}

import org.joda.time.Instant
import ru.yandex.bolts.collection.CloseableIteratorF
import ru.yandex.inside.yt.kosher.common.GUID
import ru.yandex.inside.yt.kosher.cypress.YPath
import ru.yandex.inside.yt.kosher.impl.common.http.Compressor
import ru.yandex.inside.yt.kosher.tables._
import ru.yandex.inside.yt.kosher.ytree.YTreeNode

class MockYtTable[E](entries: Seq[E]) extends YtTables {

  override def read[T](yPath: YPath, yTableEntryType: YTableEntryType[T], consumer: Consumer[T]): Unit =
    entries.foreach(e => consumer.accept(e.asInstanceOf[T]))

  override def write[T](
    optional: Optional[GUID],
    b: Boolean,
    yPath: YPath,
    yTableEntryType: YTableEntryType[T],
    iterator: util.Iterator[T],
    tableWriterOptions: TableWriterOptions
  ): Unit = ???

  override def read[T, U](
    optional: Optional[GUID],
    b: Boolean,
    yPath: YPath,
    yTableEntryType: YTableEntryType[T],
    function: JFunction[util.Iterator[T], U],
    tableReaderOptions: TableReaderOptions
  ): U = ???

  override def read[T](
    optional: Optional[GUID],
    b: Boolean,
    yPath: YPath,
    yTableEntryType: YTableEntryType[T],
    tableReaderOptions: TableReaderOptions
  ): CloseableIteratorF[T] = ???

  override def selectRows[T, U](
    optional: Optional[GUID],
    s: String,
    optional1: Optional[Instant],
    optional2: Optional[Integer],
    optional3: Optional[Integer],
    b: Boolean,
    yTableEntryType: YTableEntryType[T],
    function: JFunction[util.Iterator[T], U]
  ): U = ???

  override def lookupRows[TInput, TOutput, T](
    optional: Optional[GUID],
    yPath: YPath,
    optional1: Optional[Instant],
    yTableEntryType: YTableEntryType[TInput],
    iterable: lang.Iterable[TInput],
    yTableEntryType1: YTableEntryType[TOutput],
    function: JFunction[util.Iterator[TOutput], T],
    compressor: Compressor
  ): T = ???

  override def insertRows[T](
    optional: Optional[GUID],
    yPath: YPath,
    b: Boolean,
    b1: Boolean,
    b2: Boolean,
    yTableEntryType: YTableEntryType[T],
    iterator: util.Iterator[T],
    compressor: Compressor
  ): Unit = ???

  override def deleteRows[T](
    optional: Optional[GUID],
    yPath: YPath,
    b: Boolean,
    yTableEntryType: YTableEntryType[T],
    iterable: lang.Iterable[T],
    compressor: Compressor
  ): Unit = ???

  override def trimRows(yPath: YPath, l: Long, l1: Long): Unit = ???

  override def mount(
    yPath: YPath,
    optional: Optional[Integer],
    optional1: Optional[Integer],
    optional2: Optional[GUID]
  ): Unit = ???

  override def remount(yPath: YPath, optional: Optional[Integer], optional1: Optional[Integer]): Unit = ???

  override def unmount(yPath: YPath, optional: Optional[Integer], optional1: Optional[Integer], b: Boolean): Unit = ???

  override def freeze(yPath: YPath, optional: Optional[Integer], optional1: Optional[Integer]): Unit = ???

  override def unfreeze(yPath: YPath, optional: Optional[Integer], optional1: Optional[Integer]): Unit = ???

  override def reshard(
    optional: Optional[GUID],
    b: Boolean,
    yPath: YPath,
    list: util.List[util.List[YTreeNode]],
    optional1: Optional[Integer],
    optional2: Optional[Integer]
  ): Unit = ???

  override def alterTable(
    yPath: YPath,
    optional: Optional[lang.Boolean],
    optional1: Optional[YTreeNode],
    optional2: Optional[GUID]
  ): Unit = ???

  override def alterTableReplica(guid: GUID, optional: Optional[ReplicaMode], optional1: Optional[lang.Boolean]): Unit =
    ???

}
