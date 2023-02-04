package vertis.palma.service

import java.time.Instant
import com.google.protobuf.{Descriptors, Message}
import org.scalatest.Assertion
import ru.yandex.vertis.generators.ProducerProvider
import ru.yandex.vertis.ops.test.TestOperationalSupport
import ru.yandex.vertis.palma.event.dictionary_event.MutationEvent
import vertis.zio._
import vertis.core.utils.NoWarnFilters
import vertis.palma.DictionaryId
import vertis.palma.conf.{EncryptionConfig, MasterKeyConfig}
import vertis.palma.dao.model.Sorting.{FieldTypes, JsonField}
import vertis.palma.dao.{DictionariesDaoBaseSpec, DictionariesDaoImpl, EncryptedSecretsDaoImpl}
import vertis.palma.dao.model.{DictionaryFilter, DictionaryItem, Pagination, Sorting}
import vertis.palma.dao.ydb.YdbColumns
import vertis.palma.dao.ydb.storage.EncryptedSecretsYdbStorage
import vertis.palma.encrypted.cipher.AESGCMCipher
import vertis.palma.encrypted.crypter.EncrypterImpl
import vertis.palma.encrypted.key.EnvelopeKeyManager
import vertis.palma.external.broker.ChangelogService
import vertis.palma.external.avatars.TestAvatarsService
import vertis.palma.gen.ModelGenerators.StrGen
import vertis.palma.gen.ModelProtoGenerators._
import vertis.palma.service.DictionaryRepository._
import vertis.palma.service.descriptors.TestDescriptorRepository
import vertis.palma.service.model.{RequestContext, ResponseItem}
import vertis.palma.test._
import vertis.palma.utils.AuditLogger
import vertis.palma.utils.ProtoEnrichers._
import vertis.zio.test.ZioSpecBase
import zio.{RIO, Task, ZIO}

import scala.annotation.nowarn
import scala.jdk.CollectionConverters._

/** Spec on [[DictionaryRepository]].
  *
  * @author ruslansd
  */
class DictionaryRepositoryIntSpec
  extends ZioSpecBase
  with DictionariesDaoBaseSpec
  with DictionaryBaseSpec
  with ProducerProvider {

  private val context = RequestContext(
    Some("test"),
    Some("test"),
    Some("test"),
    Some("test")
  )

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    val storage = new EncryptedSecretsYdbStorage(ydbWrapper, prometheusRegistry)
    zioRuntime.unsafeRunTask(storage.init())
  }

  // TODO rewrite: per test clean storage
  def runTest(action: DictionaryRepository => RIO[ServerEnv, _]): Unit = {
    val dao = new DictionariesDaoImpl(ydbWrapper, TestOperationalSupport.prometheusRegistry)
    val avatarsService = new TestAvatarsService
    val secretsDao = new EncryptedSecretsDaoImpl(ydbWrapper, TestOperationalSupport.prometheusRegistry)
    val cipher = new AESGCMCipher
    val masterKey = zioRuntime.unsafeRunTask(cipher.generatePlainKey)
    val encryptedConfig = EncryptionConfig(
      MasterKeyConfig(
        MasterKeyConfig.base64Encoder.encodeToString(masterKey.bytes),
        Seq.empty
      )
    )
    val keyManager = new EnvelopeKeyManager(secretsDao, encryptedConfig, cipher)
    val encrypterSupport = new EncrypterImpl(keyManager, cipher)
    val fakeChangelogService = new ChangelogService {
      override def logChanges(
          dictDescriptor: Descriptors.Descriptor,
          id: DictionaryItem.Id,
          context: RequestContext,
          eventTime: Instant,
          action: MutationEvent.ActionEnum.Action,
          previous: scala.Option[Message],
          current: scala.Option[Message]): RTask[Unit] = {
        ZIO.unit
      }
    }
    val fakeDescriptorRepo = new TestDescriptorRepository
    val repo =
      new DictionaryRepositoryImpl(dao, avatarsService, encrypterSupport, fakeChangelogService, fakeDescriptorRepo) {
        override def logEvent(
            dictionaryId: DictionaryId,
            key: scala.Option[String],
            context: RequestContext,
            eventTime: Instant,
            action: AuditLogger.Action): RTask[Unit] = ZIO.unit
      }
    ioTest(action(repo))
  }

  "DictionaryRepository" should {
    "create messages" in {
      runTest { repo =>
        for {
          created <- repo.create(toDynamicMessage(mark), context)
          _ <- check(created.message shouldBe toDynamicMessage(mark))
        } yield ()
      }
    }

    "create message with relation" in {
      runTest { repo =>
        for {
          _ <- repo.create(toDynamicMessage(red), context)
          _ <- repo.create(toDynamicMessage(black), context)
          _ <- repo.create(toDynamicMessage(model), context)
          created <- repo.read(model.getDescriptorForType, model.getCode, context)
          _ = {
            created.message shouldBe toDynamicMessage(modelCleaned)
          }
        } yield ()
      }
    }

    "create and read encrypted dictionary messsage" in {
      runTest { repo =>
        for {
          _ <- repo.create(toDynamicMessage(encryptedMark), context)
          _ <- repo.create(toDynamicMessage(encryptedModel), context)
          createdMark <- repo.read(encryptedMark.getDescriptorForType, encryptedMark.getCode, context)
          createdModel <- repo.read(encryptedModel.getDescriptorForType, encryptedModel.getCode, context)
          _ = {
            createdMark.message shouldBe toDynamicMessage(encryptedMark)
            createdModel.message shouldBe toDynamicMessage(encryptedModelCleaned)
          }
          listedModel <- repo.list(
            encryptedModel.getDescriptorForType,
            ListFilter(Seq(DictionaryFilter("mark", encryptedModel.getMark.getCode))),
            Pagination.default,
            None,
            context
          )
          _ = {
            listedModel.items.map(_.message) should contain theSameElementsAs (Seq(
              toDynamicMessage(encryptedModelCleaned)
            ))
          }
        } yield ()
      }
    }

    "create message with nested relation" in {
      runTest { repo =>
        for {
          _ <- repo.create(toDynamicMessage(Option1), context)
          _ <- repo.create(toDynamicMessage(Option2), context)
          _ <- repo.create(toDynamicMessage(modelWithNestedLink), context)
          created <- repo.read(modelWithNestedLink.getDescriptorForType, modelWithNestedLink.getCode, context)
          _ = {
            created.message shouldBe toDynamicMessage(modelWithNestedLinkCleaned)
          }
        } yield ()
      }
    }

    "update messages" in {
      runTest { repo =>
        for {
          _ <- repo.create(toDynamicMessage(anotherMark), context)
          toUpdate = anotherMark.toBuilder.setRussianAlias("UPDATED").build()
          _ <- repo.update(toDynamicMessage(toUpdate), context)
          updated <- repo.read(anotherMark.getDescriptorForType, anotherMark.getCode, context)
          _ <- check(updated.message shouldBe toDynamicMessage(toUpdate))
        } yield ()
      }
    }

    "atomic update messages" in {
      runTest { repo =>
        def update(mark: Mark): Mark = mark.toBuilder.setRussianAlias("UPDATED").build()
        val mark = anotherMark.toBuilder.setCode("BMW_3").build()
        for {
          _ <- repo.create(toDynamicMessage(mark), context)
          d = mark.getDescriptorForType
          _ <- repo.update(d, mark.getCode) { case ResponseItem(message, updateInfo) =>
            for {
              item <- DynamicMessageParser.parseDictionaryItem(message)
              current <- Task(mark.getParserForType.parseFrom(item.payload.toByteString))
            } yield (update(current), RequestContext(userId = updateInfo.userId, schemaVersion = Some("UPDATED")))
          }

          updated <- repo.read(mark.getDescriptorForType, mark.getCode, context)

          _ <- check(updated.message shouldBe toDynamicMessage(update(mark)))
          _ <- check(updated.updateInfo.userId shouldBe Some("test"))
          _ <- check(updated.updateInfo.schemaVersion shouldBe Some("UPDATED"))
        } yield ()
      }
    }

    "delete existing message" in {
      intercept[DictionaryDbException] {
        runTest { repo =>
          val modelX6 = model.toBuilder.setCode("X6").build()
          for {
            _ <- repo.create(toDynamicMessage(modelX6), context)
            _ <- repo.delete(modelX6.getDescriptorForType, modelX6.getCode, context)
            _ <- repo.read(modelX6.getDescriptorForType, modelX6.getCode, context)
          } yield ()
        }
      }
    }

    // based on previous tests. fixme!
    "list dictionary" in {
      runTest { repo =>
        for {
          marks <- repo.list(mark.getDescriptorForType, ListFilter(), Pagination.default, None, context)
          _ <- check(marks.items.size shouldBe 3)
          _ <- check(marks.lastKey shouldBe Some("BMW_3"))

          newOne = mark.toBuilder.setCode("LADA").build()
          _ <- repo.create(toDynamicMessage(newOne), context)
          marks <- repo.list(mark.getDescriptorForType, ListFilter(), Pagination.default, None, context)
          _ <- check(marks.items.size shouldBe 4)
          _ <- check(marks.lastKey shouldBe Some(newOne.getCode))
        } yield ()

      }
    }

    "list dictionary with sorting" in {
      runTest { repo =>
        val intOrderJsonName = "int_order"
        val stringOrderJsonName = "string_order"

        val keyFd = OrderedMsg.getDescriptor.getFields.asScala.find(_.isKey).orNull

        def gen(code: String, intOrder: Int, stringOrder: String): OrderedMsg =
          OrderedMsg
            .newBuilder()
            .setCode(code)
            .setIntOrder(intOrder)
            .setStringOrder(stringOrder)
            .build()

        @nowarn(NoWarnFilters.OtherMatchAnalysis)
        def list(name: String): RepositoryTask[ListResponse] = {
          val fieldType = name match {
            case `intOrderJsonName` => FieldTypes.Int32
            case `stringOrderJsonName` => FieldTypes.String
          }
          repo.list(
            OrderedMsg.getDescriptor,
            ListFilter(),
            Pagination.default,
            Some(
              Sorting(
                JsonField(
                  name = name,
                  fieldType = fieldType,
                  jsonName = YdbColumns.JsonContentView
                )
              )
            ),
            context
          )
        }

        def checkSorted(sorted: ListResponse, expected: Seq[String]): Task[Assertion] = check {
          (sorted.items.map(_.message.getField(keyFd)) should contain).theSameElementsInOrderAs(expected)
        }

        val one = gen("code_1", 1000, "B")
        val two = gen("code_2", 1, "A")
        val three = gen("code_3", 100, "C")
        for {
          _ <- repo.create(toDynamicMessage(one), context)
          _ <- repo.create(toDynamicMessage(two), context)
          _ <- repo.create(toDynamicMessage(three), context)
          sortedByInt <- list(intOrderJsonName)
          _ <- checkSorted(sortedByInt, Seq("code_2", "code_3", "code_1"))
          sortedByString <- list(stringOrderJsonName)
          _ <- checkSorted(sortedByString, Seq("code_2", "code_1", "code_3"))
        } yield ()

      }
    }

    "list dictionary by index" in {
      runTest { repo =>
        val aliases @ (one :: two :: Nil) = StrGen.next(2)
        val colorsOne = colorGen(one).next(3)
        val colorsTwo = colorGen(two).next(5)
        val colors = Seq(colorsOne, colorsTwo)

        for {
          _ <- ZIO.collectAll {
            colors.flatten
              .map(toDynamicMessage)
              .map(repo.create(_, context))
          }
          results <- ZIO.collectAll {
            aliases.map { alias =>
              repo.list(
                Color.getDescriptor,
                ListFilter(Seq(DictionaryFilter(field = "alias", value = alias))),
                Pagination.default,
                None,
                context
              )
            }
          }
          _ <- ZIO.collectAll {
            results.zip(colors).map { case (result, expected) =>
              check(result.items.map(_.message) should contain theSameElementsAs expected)
            }
          }
        } yield ()
      }
    }

    "list dictionary by relation" in {
      runTest { repo =>
        val marks @ (one :: two :: Nil) = markGen.next(2)
        val modelsOne = modelGen(one).next(3)
        val modelsTwo = modelGen(two).next(2)
        val models = modelsOne ++ modelsTwo

        for {
          _ <- ZIO.collectAll {
            (marks.map(toDynamicMessage) ++ models.map(toDynamicMessage))
              .map(repo.create(_, context))
          }
          results <- ZIO.collectAll {
            marks.map { mark =>
              repo.list(
                Model.getDescriptor,
                ListFilter(Seq(DictionaryFilter(field = "mark", value = mark.getCode))),
                Pagination.default,
                None,
                context
              )
            }
          }
          _ <- ZIO.collectAll {
            results.zip(Seq(modelsOne, modelsTwo)).map { case (result, expected) =>
              check(result.items.map(_.message) should contain theSameElementsAs expected.map(clean))
            }
          }
        } yield ()
      }
    }

    // todo: need fix, relies on previous tests
    "list message by nested relation" in {
      runTest { repo =>
        val descriptor = modelWithNestedLink.getDescriptorForType
        for {
          resp <- repo.list(
            descriptor,
            ListFilter(Seq(DictionaryFilter(field = "package.color", value = black.getCode))),
            Pagination.default,
            None,
            context
          )
          _ <- check {
            resp.items.map(_.message) should contain theSameElementsAs Seq(modelWithNestedLinkCleaned)
          }
        } yield ()
      }
    }

    "list message by unknown index" in {
      runTest { repo =>
        val descriptor = modelWithNestedLink.getDescriptorForType
        for {
          resp <- repo.list(
            descriptor,
            ListFilter(Seq(DictionaryFilter(field = "unknownIndexName", value = black.getCode))),
            Pagination.default,
            None,
            context
          )
          _ <- check {
            resp.items.map(_.message) shouldBe empty
          }
        } yield ()
      }
    }

    "fail to read non exist item" in {
      intercept[ItemNotFoundException] {
        runTest { repo =>
          repo.read(model.getDescriptorForType, "unknown", context)
        }
      }
    }

    "fail if relation does not exists" in {
      intercept[MissingRelationException] {
        runTest { repo =>
          val newMark = mark.toBuilder.setCode("VOLVO").build()
          val newModel = model.toBuilder.setMark(newMark).build()
          repo.create(toDynamicMessage(newModel), context)
        }
      }
    }

    "correctly store image" in {
      runTest { repo =>
        for {
          created <- repo.create(toDynamicMessage(testImage), context)
          _ = {
            val parsed = parse(created.message, testImage)
            parsed.getImage.getAliasesCount should not be 0
          }
        } yield ()
      }
    }
  }
}
