package ru.yandex.auto.searchline

import java.io.{
  ByteArrayOutputStream,
  FileOutputStream,
  FileWriter,
  OutputStreamWriter
}
import java.lang.reflect.Modifier
import java.net.SocketException

import com.opencsv.CSVWriter
import org.apache.commons.io.IOUtils
import org.apache.http.client.methods.{HttpGet, HttpPost, HttpPut}
import org.apache.http.entity.ByteArrayEntity
import org.apache.http.impl.client.HttpClients
import ru.yandex.auto.core.dictionary.{InspectableMap, RegionType, Type}
import ru.yandex.auto.lifecycle.AppLifecycleTrait
import ru.yandex.auto.searcher.query2.QueryParserBase
import ru.yandex.auto.searchline.yt.{CliYtClient, YtClient}
import YtClient.{CreateParams, NodeType, WriteParams}
import ru.yandex.yt.yson.YsonUtil
import ru.yandex.yt.yson.dom.{YsonInteger, YsonMap, YsonString}
import ru.auto.api.ApiOfferModel.Category
import ru.yandex.auto.core.dictionary.query.auto.{GenerationType, ModelType}

import scala.collection.JavaConverters._
import scala.io.Source

/**
  * Put all parser terms ("official" names and aliases) to YT
  */
object TermsGenerator extends AppLifecycleTrait {
  private val http = HttpClients.createDefault()
  private val TermTableSchema =
    "[{name = category; type = string};{name = typeName; type = string};{name = typeId; type = int64};{name = term; type = string};{name = code; type = string};{name = parentCode; type = string};{name = parentTypeId; type = int64}]"
  private val CodeTableSchema =
    "[{name = category; type = string};{name = typeName; type = string};{name = typeId; type = int64};{name = code; type = string}]"

  private type TypeId = Int

  private lazy val TypeIdToName: Map[Int, String] = {
    classOf[QueryParserBase].getDeclaredFields
      .filter { f =>
        Modifier.isPublic(f.getModifiers) && Modifier.isStatic(f.getModifiers) && Modifier
          .isFinal(f.getModifiers) && f.getType.toString == "int"
      }
      .map { field =>
        field.getInt(null) -> field.getName
      }
      .groupBy { case (value, key) => value }
      .map { case (value, keys) => value -> keys.head._2 }
  }

  def main(args: Array[String]): Unit = {
    var YT_PROXY = System.getenv("YT_PROXY")
    var YT_TOKEN = System.getenv("YT_TOKEN")
    var YT_HOME = System.getenv("YT_HOME")
    args.toList.sliding(2).foreach {
      case "--YT_PROXY" :: value :: Nil => YT_PROXY = value
      case "--YT_TOKEN" :: value :: Nil => YT_TOKEN = value
      case "--YT_HOME" :: value :: Nil  => YT_HOME = value
      case _                            =>
    }
    if (YT_TOKEN == null || YT_TOKEN.isEmpty) {
      val filepath = System.getProperty("user.home") + "/.yt/token"
      if (new java.io.File(filepath).exists) {
        val file = Source.fromFile(new java.io.File(filepath), "UTF-8")
        file.withClose { () =>
          file.getLines().foreach { line =>
            if (line.nonEmpty) YT_TOKEN = line
          }
        }
      }
    }
    require(YT_PROXY != null && !YT_PROXY.isEmpty, "YT_PROXY param required")
    require(YT_TOKEN != null && !YT_TOKEN.isEmpty, "YT_TOKEN param required")
    require(YT_HOME != null && !YT_HOME.isEmpty, "YT_HOME param required")

    val client = new CliYtClient(YT_PROXY, YT_TOKEN)
    val (names, aliases, codes) = getNamesAndAliases()

    createYtTable(
      client,
      "names_v2.table",
      YT_PROXY,
      YT_TOKEN,
      YT_HOME,
      TermTableSchema
    )
    fillYtTable(
      client,
      "names_v2.table",
      rowsToYson(names),
      YT_PROXY,
      YT_TOKEN,
      YT_HOME
    )

    createYtTable(
      client,
      "aliases_v2.table",
      YT_PROXY,
      YT_TOKEN,
      YT_HOME,
      TermTableSchema
    )
    fillYtTable(
      client,
      "aliases_v2.table",
      rowsToYson(aliases),
      YT_PROXY,
      YT_TOKEN,
      YT_HOME
    )

    createYtTable(
      client,
      "codes_v2.table",
      YT_PROXY,
      YT_TOKEN,
      YT_HOME,
      CodeTableSchema
    )
    fillYtTable(
      client,
      "codes_v2.table",
      codeRowsToYson(codes),
      YT_PROXY,
      YT_TOKEN,
      YT_HOME
    )
  }

  private def rowsToYson(rows: Seq[Row]): Iterator[Array[Byte]] = {
    rows.toIterator.map { row =>
      val kv = YsonMap
        .builder()
        .put("category", new YsonString(row.category.name()))
        .put("typeName", new YsonString(TypeIdToName(row.typeId)))
        .put("typeId", new YsonInteger(row.typeId))
        .put("term", new YsonString(row.term.getBytes("UTF-8")))
        .put("code", new YsonString(row.aType.code.getBytes("UTF-8")))
        .put(
          "parentCode",
          new YsonString(row.aType.getParentCode().getOrElse(""))
        )
        .put(
          "parentTypeId",
          new YsonInteger(row.aType.getParentTypeId().getOrElse(0))
        )
        .build()
      YsonUtil.toByteArray(kv) :+ ';'.toByte
    }
  }

  private def codeRowsToYson(rows: Seq[CodeRow]): Iterator[Array[Byte]] = {
    rows.toIterator.map { row =>
      val kv = YsonMap
        .builder()
        .put("category", new YsonString(row.category.name()))
        .put("typeName", new YsonString(TypeIdToName(row.typeId)))
        .put("typeId", new YsonInteger(row.typeId))
        .put("code", new YsonString(row.aType.code.getBytes("UTF-8")))
        .build()
      YsonUtil.toByteArray(kv) :+ ';'.toByte
    }
  }

  private def createYtTable(client: YtClient,
                            name: String,
                            YT_PROXY: String,
                            YT_TOKEN: String,
                            YT_HOME: String,
                            schema: String): Unit = {
    client.create(
      NodeType.Table,
      YT_HOME + "/" + name,
      Some(Seq("schema" -> schema)),
      Some(CreateParams(force = true))
    )
  }

  private def fillYtTable(client: YtClient,
                          name: String,
                          content: Iterator[Array[Byte]],
                          YT_PROXY: String,
                          YT_TOKEN: String,
                          YT_HOME: String): Unit = {
    val file = java.io.File.createTempFile(name, ".bin")
    try {
      val writer = new FileOutputStream(file)
      try {
        content.foreach(writer.write)
      } finally {
        writer.close()
      }
      println(s"Temporary file ${file.getAbsoluteFile} created")
      client.writeTable(
        YT_HOME + "/" + name,
        file.getAbsolutePath,
        Some(WriteParams(encode_utf8 = true))
      )
    } finally {
      file.delete()
      println(s"Temporary file ${file.getAbsoluteFile} deleted")
    }
  }

  private def getNamesAndAliases(): (Seq[Row], Seq[Row], Seq[CodeRow]) = {
    val termSet = collection.mutable.Set.empty[String]
    val names = collection.mutable.Buffer.empty[Row]
    val aliases = collection.mutable.Buffer.empty[Row]

    getDicts().foreach {
      case (category, typeId, inspectableMap) =>
        for (kv <- inspectableMap.names().entrySet().asScala) {
          val name = kv.getKey.toLowerCase.trim
          termSet.add(name)
          for (t <- anyRefToList(kv.getValue)) {
            names.append(Row(category, typeId, name, t))
          }
        }
        for (kv <- inspectableMap.aliases().entrySet().asScala) {
          val alias = kv.getKey.toLowerCase.trim
          if (!termSet.contains(alias)) {
            for (t <- anyRefToList(kv.getValue)) {
              aliases.append(Row(category, typeId, alias, t))
            }
          }
        }
    }

    val codes =
      aliases.toList.map(_.getCodeRow) ++ names.toList.map(_.getCodeRow)

    (names.distinct, aliases.distinct, codes.distinct)
  }

  private def anyRefToList(a: AnyRef): List[AType] =
    a match {
      case t: Type        => List(DictType(t))
      case rt: RegionType => List(DictRegionType(rt))
      case s: String      => List(DictString(s))
      case list: java.util.List[AnyRef] =>
        list.asScala.flatMap(anyRefToList).toList
    }

  private def getDicts(): Seq[(Category, TypeId, InspectableMap[AnyRef])] = {
    val context = createApplicationContext("classpath:searchline-core.xml")
    List(
      Category.CARS -> "dictionariesMap",
      Category.TRUCKS -> "trucksDictionariesMap",
      Category.MOTO -> "motoDictionariesMap"
    ).flatMap {
      case (category, dictName) =>
        val dicts = context.getBean(
          dictName,
          classOf[java.util.Map[Integer, InspectableMap[AnyRef]]]
        )
        dicts.asScala.toSeq
          .sortBy {
            case (typeId, _) =>
              typeId -> QueryParserBase.DICTIONARIES_ORDER.indexOf(typeId)
          }
          .map {
            case (typeId, inspectableMap) =>
              (category, typeId.toInt, inspectableMap)
          }
    }
  }

  private case class Row(category: Category,
                         typeId: Int,
                         term: String,
                         aType: AType) {
    def getCodeRow(): CodeRow = CodeRow(category, typeId, aType)
  }
  private case class CodeRow(category: Category, typeId: Int, aType: AType)

  private sealed trait AType {
    def code: String
    protected def getParent(): Option[(String, Int)] = None
    def getParentCode(): Option[String] = getParent().map(_._1)
    def getParentTypeId(): Option[Int] = getParent().map(_._2)
  }
  private case class DictType(t: Type) extends AType {
    override def code: String = t.getCode
    override protected def getParent(): Option[(String, TypeId)] = t match {
      case model: ModelType =>
        Some(model.getMark.getCode -> QueryParserBase.MARK)
      case gen: GenerationType =>
        Some(gen.getModel.getCode -> QueryParserBase.MODEL)
      case _ => None
    }
  }
  private case class DictRegionType(t: RegionType) extends AType {
    override def code: String = t.getType.getCode
  }
  private case class DictString(t: String) extends AType {
    override def code: String = t
  }
}
