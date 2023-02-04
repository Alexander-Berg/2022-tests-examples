package vasgen.indexer.saas.dm

import java.io.PrintWriter

/** Упрощение генерации **зонных** и **статических** факторов для saas
  * конфигурации.
  *
  * Алгоритм использования:
  *   - указать в переменной newTextAttributes или newStatAttributes новые
  *     атрибуты (без префиксов)
  *   - убрать `ignore` с теста
  *   - запустить runConfiguration `AddFactorEasily.run.xml` (предварительно
  *     скопировать из темплейта, там явки/пароли, как ходить в saas (спойлер:
  *     через saas-indexer'ные креды))
  *
  * На выходе:
  *   - вывод в stdout хвоста полинома (необходимо, чтобы значения логировались
  *     на стороне ml)
  *   - файлы `updated-config-*.json`, в котором **еще нужно**:
  *   1. Дописать секции(не парсятся в основном коде, не рискую парсить сейчас
  *      для утилиты)
  *   - user_functions
  *   - geo_layers 2. Декодировать полиномы(не все), дописать хвост полинома с
  *     новыми атрибутами, закодировать
  *
  * После этого можно заливать на saas, сверять изменения и деплоить
  */
object AddFactorEasily extends ZIOSpecDefault with Logging {

  // todo fill before launch (without prefix)
  val newTextAttributes: List[String] = List("title")
  val newStatAttributes: List[String] = List("stats.category_cnt_active_offers")

  val customEnv = {

    val metrics: ULayer[Metrics] = Metrics.live
    val env: ZLayer[zio.ZEnv, ReadError[String], Terrain] =
      (TestEnvironment.live ++ System.live) >>> Terrain.live

    val tracing = Tracing.noop
    val tvm     = env >>> TVM.live

    val base = TestEnvironment.live ++ metrics ++ env ++ tracing

    val httpClient =
      (AsyncHttpClientZioBackend.layer().orDie ++ tvm ++ base) >>>
        HttpClient.live

    val dmConfig = env >>> Terrain.configLayer(DmConfigDescriptor)

    val dmClient = (dmConfig ++ httpClient) >>> DmClient.live
    dmClient ++ base
  }

  val currentConfigZ: ZIO[Has[DmClient.Service], Throwable, Relev.Relev] =
    for {
      dm <- ZIO.service[DmClient.Service]
      data <- dm.getConfig(
        RtyServer,
        dm.clusterService,
        s"$configName-${dm.clusterService}",
      )
      current <- ZIO.fromEither(Relev(data.data))
      _       <- log.info(s"$current")
    } yield current

  private val textPrefixFamily = List(
    "_BM25F_Sy",
    "_BM25F_Lm",
    "_BM25F_St",
    "_CZL_Sy",
    "_CZL_Lm",
    "_CZL_St",
    "_CZ_Lm",
    "_CZ_St",
    "_CZ_Sy",
    "_CM_Sy",
    "_CM_Lm",
    "_CM_St",
    "_ZL_Sy",
    "_ZL_Lm",
    "_ZL_St",
    "_IF_Sy",
    "_IF_Lm",
    "_IF_St",
  )

  private val polynomGlue = "+0*"
  private val configName  = "relev.conf"

  override def spec = {
    (
      suite("add factors")(
        test("add text factors") {

          val newZoneFactors = textPrefixFamily.flatMap(prefix =>
            newTextAttributes.map(attr => s"${prefix}_z_$attr"),
          )

          val invoke =
            for {
              currentConfig <- currentConfigZ
              alreadyDefinedFactorsNames <- ZIO
                .fromOption(currentConfig.zoneFactors.map(_.keys.toSeq))
              used <- ZIO.from(
                newZoneFactors.filter(newAttr =>
                  alreadyDefinedFactorsNames.contains(newAttr),
                ),
              )
              _ <- log
                .warn(s"Some zone factors have already in config: $used")
                .when(used.nonEmpty)
            } yield {
              val updatedConf = currentConfig.copy(zoneFactors =
                currentConfig
                  .zoneFactors
                  .map(
                    _ ++
                      indexedFactors(currentConfig, newZoneFactors, used, None),
                  ),
              )
              new PrintWriter("updated-config-text-factors.json") {
                write(updatedConf.asJson.deepDropNullValues.toString)
                close()
              }
              ()
            }
          assertZIO(invoke)(isUnit)
        },
        test("add static factors") {
          val newStatFactors = newStatAttributes.map(attr =>
            FieldMappingBase
              .convertName(SaasIndexType.Prefix.factor, "_g", FieldName(attr)),
          )
          val invoke =
            for {
              currentConfig <- currentConfigZ
              alreadyDefinedFactorsNames <- ZIO
                .fromOption(currentConfig.staticFactors.map(_.keys.toSeq))
              used <- ZIO.from(
                newStatFactors.filter(newAttr =>
                  alreadyDefinedFactorsNames.contains(newAttr),
                ),
              )
              _ <- log
                .warn(s"Some static factors have already in config: $used")
                .when(used.nonEmpty)
            } yield {

              val updatedConf = currentConfig.copy(zoneFactors =
                currentConfig
                  .staticFactors
                  .map(
                    _ ++
                      indexedFactors(
                        currentConfig,
                        newStatFactors,
                        used,
                        Some(0.0),
                      ),
                  ),
              )
              val res = updatedConf.asJson.deepDropNullValues.toString
              new PrintWriter("updated-config-stat-factors.json") {
                write(res)
                close()
              }
              res
            }

          assertZIO(invoke)(isNonEmptyString)
        },
      ),
    ).provideCustomLayerShared(customEnv.mapError(e => TestFailure.fail(e))) @@
      ignore
  }

  private def indexedFactors(
    currentConfig: Relev.Relev,
    newZoneFactors: Seq[String],
    used: Seq[String],
    defaultValue: Option[Double],
  ): Map[String, Relev._Factor] = {
    val maxIndex         = currentConfig.toFactors.map(f => f.index).max
    val reallyNewFactors = newZoneFactors.diff(used)
    val startWith: Int   = maxIndex + 1
    if (reallyNewFactors.nonEmpty) {
      val polynomTail = polynomGlue + reallyNewFactors.mkString(polynomGlue)
      println(s"Add `$polynomTail` to decoded polynom")
    } else {
      println(s"Nothing to add!")
    }

    reallyNewFactors
      .zip(startWith until startWith + reallyNewFactors.length)
      .map { case (name, index) =>
        (name, Relev._Factor(index, defaultValue))
      }
      .toMap
  }

}

object Encoders {

  implicit val encodeFormula: Encoder[Relev._Formula] =
    deriveEncoder[Relev._Formula]

  implicit val encodeFactor: Encoder[Relev._Factor] = Encoder.instance(f =>
    if (f.defaultValue.isDefined) {
      deriveEncoder[Relev._Factor](io.circe.derivation.renaming.snakeCase)(f)
    } else {
      Json.fromInt(f.index)
    },
  )

  implicit val encodeFactors: Encoder[Map[String, Relev._Factor]] = Encoder
    .instance { a =>
      val prepared = a
        .toSeq
        .sortBy(_._2.index)
        .map { case (k, v) =>
          (k, encodeFactor(v))
        }
      JsonObject.apply(prepared: _*).asJson
    }

  implicit val encodeFormulas: Encoder[Map[String, Relev._Formula]] = Encoder
    .instance { a =>
      val prepared = a
        .toSeq
        .map { case (k, v) =>
          (k, encodeFormula(v))
        }
      JsonObject(prepared: _*).asJson
    }

  implicit val encodeRelev: Encoder[Relev.Relev] = deriveEncoder[Relev.Relev](
    io.circe.derivation.renaming.snakeCase,
  )

}
