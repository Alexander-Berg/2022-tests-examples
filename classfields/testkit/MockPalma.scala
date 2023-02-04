package common.palma.testkit

import common.palma.Palma
import common.palma.Palma._
import common.palma.testkit.MockPalma._
import ru.yandex.vertis.palma.palma_options.PalmaOptionsProto
import scalapb.{GeneratedMessage, GeneratedMessageCompanion}
import zio.stm._
import zio.stream.ZStream
import zio.{Has, IO, UIO, URIO, ZIO}

import java.time.Instant

final class MockPalma(dictionaries: TMap[String, MemoryDictionary[_]]) extends Palma.Service {

  private def dictionaryName[T: PalmaType] = PalmaType[T].dictionaryName

  private def dictionary[T: PalmaType]: STM[Nothing, MemoryDictionary[T]] = dictionaries
    .getOrElse(dictionaryName[T], MemoryDictionary[T](dictionaryName[T], Map.empty))
    .map(_.asInstanceOf[MemoryDictionary[T]])

  override def create[T: PalmaType](entity: T): IO[Palma.PalmaError, Palma.Item[T]] =
    STM.atomically {
      for {
        dict <- dictionary[T]
        code = codeFor(entity)
        _ <- dict.get(code) match {
          case Some(_) => STM.fail(Palma.AlreadyExists(s"$code already exist in dictionary ${dict.name}"))
          case None => STM.unit
        }
        item = Palma.Item(entity, UpdateInfo("", "", Instant.now, "1"))
        newDict = dict.create(code, item)
        _ <- dictionaries.put(dictionaryName[T], newDict)
      } yield item
    }

  override def update[T: PalmaType](entity: T): IO[Palma.PalmaError, Palma.Item[T]] =
    STM.atomically {
      for {
        dict <- dictionary[T]
        code = codeFor(entity)
        oldItem <- dict.get(code) match {
          case Some(v) => STM.succeed(v)
          case None => STM.fail(Palma.NotFound(s"$code not found in dictionary ${dict.name}"))
        }
        item = Palma.Item(entity, UpdateInfo("", "", Instant.now, (oldItem.updateInfo.version.toLong + 1).toString))
        newDict = dict.create(code, item)
        _ <- dictionaries.put(dictionaryName[T], newDict)
      } yield item
    }

  override def updateCas[T: PalmaType](entity: T, casVersion: String): IO[Palma.PalmaError, Palma.Item[T]] = ???

  override def delete[T: PalmaType](key: String): IO[Palma.PalmaError, Unit] =
    STM.atomically {
      for {
        dict <- dictionary[T]
        _ <- dict.get(key) match {
          case Some(_) => STM.unit
          case None => STM.fail(Palma.NotFound(s"$key not found in dictionary ${dict.name}"))
        }
        newDict = dict.delete(key)
        _ <- dictionaries.put(dictionaryName[T], newDict)
      } yield ()
    }

  override def deleteCas[T: PalmaType](key: String, casVersion: String): IO[Palma.PalmaError, Unit] = ???

  override def get[T: PalmaType](key: String): IO[Palma.PalmaError, Option[Palma.Item[T]]] =
    dictionary[T].map(_.get(key)).commit

  override def list[T: PalmaType](
      filters: Seq[Palma.Filter],
      pagination: Option[Palma.Pagination],
      sorting: Option[Palma.Sorting]): IO[Palma.PalmaError, Palma.Listing[T]] =
    dictionary[T].map { dict =>
      var res = dict.data.values.toVector

      res = filters.foldLeft(res) { case (res, filter) =>
        res.filter { item =>
          val map = fields(item.value)
          val value = map.getOrElse(filter.fieldName, "")
          value == filter.fieldValue
        }
      }
      res = res.sortBy(e => codeFor(e.value))

      res = pagination match {
        case None => res
        case Some(p) => res.filter(i => codeFor(i.value) >= p.lastKey).take(p.limit)
      }

      res = sorting match {
        case Some(s) =>
          val ord = if (s.ascending) Ordering.String else Ordering.String.reverse
          res.sortBy(i => (fields(i.value).getOrElse(s.fieldName, ""), codeFor(i.value)))(
            Ordering.Tuple2(ord, Ordering.String)
          )
        case None => res
      }

      Palma.Listing(res, res.lastOption.map(e => codeFor(e.value)).getOrElse(""))
    }.commit

  override def all[T: PalmaType]: ZStream[Any, Palma.PalmaError, Palma.Item[T]] =
    ZStream.fromIterableM(dictionary[T].map(_.data.values).commit)

  def clean[T: PalmaType]: UIO[Unit] = dictionary[T].flatMap(d => dictionaries.put(dictionaryName[T], d.clean)).commit

}

object MockPalma {

  def make: UIO[Palma.Service with MockPalma] = TMap.empty[String, MemoryDictionary[_]].commit.map(new MockPalma(_))

  def clean[T: PalmaType]: URIO[Has[MockPalma], Unit] =
    ZIO.serviceWith[MockPalma](_.clean)

  private def message[T](e: T): T with GeneratedMessage = {
    e match {
      case m: GeneratedMessage => m.asInstanceOf[T with GeneratedMessage]
      case _ => sys.error(s"Not scalapb message: $e")
    }
  }

  private def companion[T](e: T): GeneratedMessageCompanion[_] =
    message(e).companion

  private def codeFor[T](e: T): String = {
    val c = companion(e)
    val d = c.scalaDescriptor
    val codeField = d.fields
      .filter(f => f.getOptions.extension(PalmaOptionsProto.field).map(_.key).getOrElse(false))
      .headOption
      .getOrElse(sys.error(s"Not found key field in message ${d.fullName}"))

    message(e).getField(codeField).as[String]
  }

  private def fields[T](e: T): Map[String, String] = {
    val c = companion(e)
    val d = c.scalaDescriptor
    d.fields.map { f =>
      f.name -> message(e).getField(f).as[String]
    }.toMap
  }

  case class MemoryDictionary[T](name: String, data: Map[String, Palma.Item[T]]) {

    def create(code: String, item: Palma.Item[T]): MemoryDictionary[T] = {
      copy(data = data + (code -> item))
    }

    def delete(code: String) = copy(data = data - code)

    def get(key: String) = data.get(key)

    def clean = copy(data = Map.empty)
  }
}
