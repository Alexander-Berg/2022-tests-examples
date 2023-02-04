package ru.yandex.vertis.general.bonsai.logic.test

import common.scalapb.ScalaProtobuf
import common.zio.ydb.Ydb
import common.zio.ydb.testkit.TestYdb
import general.bonsai.attribute_model.AttributeDefinition.AttributeSettings
import general.bonsai.attribute_model.AttributeDefinition.AttributeSettings.{
  DictionarySettings => AttributeDictionarySettings
}
import general.bonsai.attribute_model.DictionarySettings.DictionaryValue
import general.bonsai.attribute_model.{AttributeDefinition, BooleanSettings, DictionarySettings}
import general.bonsai.category_model.{Category, CategoryAttribute}
import general.bonsai.internal.internal_api.{AttributeFilter, PagingRequest, SearchParameters}
import general.bonsai.restriction_model.DictionaryRestriction.DictionaryRestrictionValue
import general.bonsai.restriction_model.Restriction.TypeRestriction.{
  DictionaryRestriction => TypedDictionaryRestriction
}
import general.bonsai.restriction_model.{DictionaryRestriction, Restriction, RestrictionRule}
import ru.yandex.vertis.general.bonsai.logic.{AttributeManager, CategoryManager, SearchManager}
import ru.yandex.vertis.general.bonsai.model._
import ru.yandex.vertis.general.bonsai.storage.ConstraintsDao
import ru.yandex.vertis.general.bonsai.storage.testkit._
import ru.yandex.vertis.general.bonsai.storage.ydb.sign.EntityUpdateChecker
import ru.yandex.vertis.general.bonsai.storage.ydb.{
  BonsaiTxRunner,
  YdbConstraintsDao,
  YdbEntityDao,
  YdbGlobalHistoryDao,
  YdbHierarchyDao,
  YdbHistoryDao,
  YdbSuggestEntityIndexDao
}
import ru.yandex.vertis.general.common.model.pagination.LimitOffset
import common.zio.logging.Logging
import zio.ZIO
import zio.clock.Clock
import zio.test.Assertion._
import zio.test._

object DefaultAttributeManagerTest extends DefaultRunnableSpec {

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = {

    suite("DefaultAttributeManager")(
      testM("Create attribute and check it's id and version") {
        for {
          attribute <- AttributeManager.createAttribute(
            AttributeDefinition(uriPart = "create", uniqueName = "createName")
          )
        } yield assert(attribute.id)(isNonEmptyString)
      },
      testM("Create attribute and get it") {
        for {
          created <- AttributeManager.createAttribute(
            AttributeDefinition(uriPart = "createget", uniqueName = "creategetName")
          )
          returned <- AttributeManager.getAttribute(created.id, ExactVersion(created.version))
        } yield assert(returned.getAttribute)(equalTo(created))
      },
      testM("Can create archived attribute") {
        for {
          a1 <- AttributeManager.createAttribute(
            AttributeDefinition(uriPart = "create3", uniqueName = "create3Name", isArchived = true)
          )
          a2 <- AttributeManager.createAttribute(
            AttributeDefinition(uriPart = "create3", uniqueName = "create3Name", isArchived = true)
          )
        } yield assert(a1.id)(not(equalTo(a2.id))) && assert(a1.uriPart)(equalTo(a2.uriPart))
      },
      testM("Throw constrains violation when creating attribute with not uniq uri_part") {
        for {
          _ <- AttributeManager.createAttribute(AttributeDefinition(uriPart = "part", uniqueName = "partName"))
          result <- AttributeManager
            .createAttribute(AttributeDefinition(uriPart = "part", uniqueName = "partName"))
            .flip
        } yield assert(result)(isSubtype[UniqConstraintViolation](anything))
      },
      testM("Throw entity not found when trying to get non existing attribute") {
        for {
          result <- AttributeManager.getAttribute("someId", Latest).flip
        } yield assert(result)(isSubtype[EntityNotFound](anything))
      },
      testM("List attributes without filter and paging") {
        for {
          _ <- AttributeManager.createAttribute(AttributeDefinition(uriPart = "list1", uniqueName = "list1Name"))
          _ <- AttributeManager.createAttribute(AttributeDefinition(uriPart = "list2", uniqueName = "list2Name"))
          list <- AttributeManager.listAttributes(AttributeFilter(), PagingRequest(limit = 10))
        } yield assert(list.attributes)(isNonEmpty)
      },
      testM("Update created attribute") {
        for {
          first <- AttributeManager.createAttribute(
            AttributeDefinition(uriPart = "update1", uniqueName = "update1Name")
          )
          updated <- AttributeManager.updateAttribute(first.copy(description = "aaaa"), first.version)
          afterUpdate <- AttributeManager.getAttribute(updated.id, ExactVersion(updated.version))
        } yield assert(afterUpdate.getAttribute)(equalTo(updated))
      },
      testM("Throw error when change attribute type") {
        for {
          attribute <- AttributeManager.createAttribute(
            AttributeDefinition(
              uriPart = "change attribute type",
              uniqueName = "change attribute typeName",
              attributeSettings = AttributeDictionarySettings(
                DictionarySettings(allowedValues = Seq(DictionaryValue(key = "val", name = "val")))
              )
            )
          )
          updated <- AttributeManager
            .updateAttribute(
              attribute.copy(attributeSettings = AttributeSettings.BooleanSettings(value = BooleanSettings())),
              attribute.version
            )
            .flip
        } yield assert(updated)(isSubtype[FieldIsReadOnly](anything))
      },
      testM("Throw constraints violation when trying to archive attribute with external references") {
        for {
          first <- AttributeManager.createAttribute(
            AttributeDefinition(uriPart = "archive_referenced1", uniqueName = "archive_referenced1Name")
          )
          another <- AttributeManager.createAttribute(
            AttributeDefinition(
              uriPart = "archive_referenced2",
              uniqueName = "archive_referenced2Name",
              migrateToAttributeId = first.id
            )
          )
          result <- AttributeManager.archiveAttribute(first.id).flip
        } yield assert(result)(isSubtype[ExternalReferenceViolation](anything))
      },
      testM("Throw error when trying to create attribute with missing link to attribute") {
        for {
          first <-
            AttributeManager
              .createAttribute(
                AttributeDefinition(
                  uriPart = "create_missing_attribute_link1",
                  uniqueName = "create_missing_attribute_link1Name",
                  migrateToAttributeId = "create_missing_attribute_link2"
                )
              )
              .flip
        } yield assert(first)(isSubtype[ReferenceEntitiesNotExists](anything))
      },
      testM("Throw error when trying to create category with archived link to category") {
        for {
          first <- AttributeManager.createAttribute(
            AttributeDefinition(
              uriPart = "create_archived_category_link1",
              uniqueName = "create_archived_category_link1Name"
            )
          )
          _ <- AttributeManager.archiveAttribute(first.id)
          second <-
            AttributeManager
              .createAttribute(
                AttributeDefinition(
                  uriPart = "archived_category_link2",
                  uniqueName = "archived_category_link2Name",
                  migrateToAttributeId = first.id
                )
              )
              .flip
        } yield assert(second)(isSubtype[ReferenceEntitiesAreArchived](anything))
      },
      testM("Allow to modify uri_part") {
        for {
          created <- AttributeManager.createAttribute(
            AttributeDefinition(uriPart = "uriPartModifiable", uniqueName = "uriPartModifiableName")
          )
          _ <- AttributeManager.updateAttribute(
            created.copy(uriPart = "uriPartAnother", uniqueName = "uriPartAnotherName"),
            created.version
          )
          res1 <- AttributeManager
            .createAttribute(AttributeDefinition(uriPart = "uriPartModifiable", uniqueName = "uriPartModifiableName"))
            .run
          res2 <- AttributeManager
            .createAttribute(AttributeDefinition(uriPart = "uriPartAnother", uniqueName = "uriPartAnotherName"))
            .run
        } yield assert(res1)(succeeds(anything)) && assert(res2)(fails(isSubtype[UniqConstraintViolation](anything)))
      },
      testM("Archive with uri_part change") {
        for {
          created <- AttributeManager.createAttribute(
            AttributeDefinition(uriPart = "uriPartArchive", uniqueName = "uriPartArchiveName")
          )
          _ <- AttributeManager.createAttribute(
            AttributeDefinition(uriPart = "uriPartArchive2", uniqueName = "uriPartArchive2Name")
          )

          _ <- AttributeManager.updateAttribute(
            created.copy(uriPart = "uriPartArchive2", uniqueName = "uriPartArchive2Name", isArchived = true),
            created.version
          )

          res <- AttributeManager
            .createAttribute(AttributeDefinition(uriPart = "uriPartArchive", uniqueName = "uriPartArchiveName"))
            .run
        } yield assert(res.untraced)(succeeds(anything))
      },
      testM("Restore with uri_part change") {
        for {
          created <-
            AttributeManager.createAttribute(
              AttributeDefinition(uriPart = "uriPartRestore", uniqueName = "uriPartRestoreName", isArchived = true)
            )
          _ <- AttributeManager.createAttribute(
            AttributeDefinition(uriPart = "uriPartRestore", uniqueName = "uriPartRestoreName")
          )

          _ <- AttributeManager.updateAttribute(
            created.copy(uriPart = "uriPartRestore2", uniqueName = "uriPartRestore2Name", isArchived = false),
            created.version
          )

          res <- AttributeManager
            .createAttribute(AttributeDefinition(uriPart = "uriPartRestore2", uniqueName = "uriPartRestore2Name"))
            .run
        } yield assert(res)(fails(isSubtype[UniqConstraintViolation](anything)))
      },
      testM("Throw entity already modified when trying to modify old version") {
        for {
          created <- AttributeManager.createAttribute(
            AttributeDefinition(uriPart = "updateOld", uniqueName = "updateOldName")
          )
          _ <- AttributeManager.updateAttribute(created.copy(description = "ddd"), created.version)
          result <- AttributeManager.updateAttribute(created.copy(description = "anotherOne"), created.version).flip
        } yield assert(result)(isSubtype[EntityAlreadyModified](anything))
      },
      testM("Throw dictionary attribute value missing when trying to delete using dictionary value") {
        for {
          attribute <- AttributeManager.createAttribute(
            AttributeDefinition(
              uriPart = "dictionary",
              uniqueName = "dictionaryName",
              attributeSettings = AttributeDictionarySettings(
                DictionarySettings(allowedValues = Seq(DictionaryValue(key = "val", name = "val")))
              )
            )
          )
          _ <- CategoryManager.createCategory(
            Category(
              uriPart = "dictionary",
              uniqueName = "dictionaryName",
              attributes = Seq(CategoryAttribute(attribute.id)),
              restrictionRules = Seq(
                RestrictionRule(restrictions =
                  Seq(
                    Restriction(
                      attribute.id,
                      TypedDictionaryRestriction(DictionaryRestriction(DictionaryRestrictionValue.MustEqual("val")))
                    )
                  )
                )
              )
            )
          )
          result <-
            AttributeManager
              .updateAttribute(
                attribute
                  .copy(attributeSettings = AttributeDictionarySettings(DictionarySettings(allowedValues = Seq.empty))),
                attribute.version
              )
              .flip
        } yield assert(result)(isSubtype[DictionaryAttributeValuesMissing](anything))
      },
      testM("Return attribute with old version") {
        for {
          created <- AttributeManager.createAttribute(AttributeDefinition(uriPart = "old", uniqueName = "oldName"))
          updated <- AttributeManager.updateAttribute(created.copy(description = "aaaa"), created.version)
          oldVersion <- AttributeManager.getAttribute(updated.id, ExactVersion(created.version))
        } yield assert(oldVersion.getAttribute)(equalTo(created))
      },
      testM("Archive attribute") {
        for {
          created <- AttributeManager.createAttribute(
            AttributeDefinition(uriPart = "archive", uniqueName = "archiveName")
          )
          _ <- AttributeManager.archiveAttribute(created.id)
          archived <- AttributeManager.getAttribute(created.id, Latest)
        } yield assert(archived.getAttribute.isArchived)(isTrue)
      },
      testM("Delete uniq constraints when archiving attribute") {
        for {
          attribute <- AttributeManager.createAttribute(
            AttributeDefinition(uriPart = "archiveClear", uniqueName = "archiveClearName")
          )
          _ <- AttributeManager.archiveAttribute(attribute.id)
          valueFree <- runTx(ConstraintsDao.checkUniqValue(EntityRef.forAttribute("newId"), "uriPart", "archiveClear"))
        } yield assert(valueFree)(isTrue)
      },
      testM("Fail archiving when have external references") {
        for {
          attribute <- AttributeManager.createAttribute(
            AttributeDefinition(uriPart = "archiveExternal", uniqueName = "archiveExternalName")
          )
          _ <- CategoryManager.createCategory(
            Category(
              uriPart = "archiveExternal",
              uniqueName = "archiveExternalName",
              attributes = Seq(CategoryAttribute(attribute.id))
            )
          )
          result <- AttributeManager.archiveAttribute(attribute.id).flip
        } yield assert(result)(isSubtype[ExternalReferenceViolation](anything))
      },
      testM("Restore archived attribute") {
        for {
          created <- AttributeManager.createAttribute(
            AttributeDefinition(uriPart = "restore", uniqueName = "restoreName")
          )
          _ <- AttributeManager.archiveAttribute(created.id)
          _ <- AttributeManager.restoreAttribute(created.id)
          restored <- AttributeManager.getAttribute(created.id, Latest)
        } yield assert(restored.getAttribute.isArchived)(isFalse)
      },
      testM("Throw constraints violation when trying to restore attribute with non uniq uriPart") {
        for {
          first <- AttributeManager.createAttribute(
            AttributeDefinition(uriPart = "nonuniq", uniqueName = "nonuniqName")
          )
          _ <- AttributeManager.archiveAttribute(first.id)
          _ <- AttributeManager.createAttribute(AttributeDefinition(uriPart = "nonuniq", uniqueName = "nonuniqName"))
          result <- AttributeManager.restoreAttribute(first.id).flip
        } yield assert(result)(isSubtype[UniqConstraintViolation](anything))
      },
      testM(
        "Throw constraints violation when trying to archive attribute which is referenced by previously restored attribute"
      ) {
        for {
          referenced <- AttributeManager.createAttribute(
            AttributeDefinition(uriPart = "referenced_attr", uniqueName = "referenced_attrName")
          )
          referencing <- AttributeManager.createAttribute(
            AttributeDefinition(
              uriPart = "referencing_attr",
              uniqueName = "referencing_attrName",
              migrateToAttributeId = referenced.id
            )
          ) // references first attribute
          _ <- AttributeManager.archiveAttribute(referencing.id)
          _ <- AttributeManager.restoreAttribute(referencing.id) // archive and restore referencing attribute
          result <- AttributeManager.archiveAttribute(referenced.id).run
        } yield assert(result)(fails(isSubtype[ExternalReferenceViolation](anything)))
      },
      testM("List attribute history") {
        for {
          createdAttribute <-
            AttributeManager.createAttribute(
              AttributeDefinition(uriPart = "initial", uniqueName = "initialName"),
              NewVersion(0L, "create")
            )
          updatedAttribute <- AttributeManager.updateAttribute(
            createdAttribute.copy(description = "updated"),
            createdAttribute.version,
            NewVersion(0L, "update")
          )
          _ <- AttributeManager.archiveAttribute(updatedAttribute.id, NewVersion(0L, "archive"))
          archivedAttribute <- AttributeManager.getAttribute(updatedAttribute.id, Latest).map(_.getAttribute)
          _ <- AttributeManager.restoreAttribute(archivedAttribute.id, NewVersion(0L, "restore"))
          restoredAttribute <- AttributeManager.getAttribute(archivedAttribute.id, Latest).map(_.getAttribute)

          resp <- AttributeManager.getHistory(attributeId = archivedAttribute.id, blankPagingHistoryRequest)
          commentsAssert = assert(resp.historyEntries.map(_.comment))(
            equalTo(Seq("restore", "archive", "update", "create"))
          )

          now <- zio.clock.instant
          timestamps = resp.historyEntries.flatMap(_.timestamp).map(ScalaProtobuf.timestampToInstant)
          timestampsNumber = assert(timestamps)(hasSize(equalTo(4)))
          timestampsTime = assert(timestamps)(hasLast(isGreaterThan(now.minusSeconds(100))))
          timestampsSorted = assert(timestamps)(isSortedReverse)
          timestampsAssert = timestampsNumber && timestampsTime && timestampsSorted

          historyAttributes = resp.historyEntries.map(_.attribute)
          expectedAttributes =
            Seq(restoredAttribute, archivedAttribute, updatedAttribute, createdAttribute).map(Some(_))
          attributesAssert = assert(historyAttributes)(equalTo(expectedAttributes))
        } yield timestampsAssert && commentsAssert && attributesAssert
      },
      testM("Update attribute with existing link on it and update version inside category") {
        for {
          attr <- AttributeManager.createAttribute(
            AttributeDefinition(uriPart = "update_cat", uniqueName = "update_catName")
          )
          category <- CategoryManager.createCategory(
            Category(
              uriPart = "c_update_cat",
              uniqueName = "c_update_catName",
              attributes = Seq(CategoryAttribute(attributeId = attr.id))
            )
          )
          updated <- AttributeManager.updateAttribute(attr.copy(name = "name"), attr.version)
          res <- CategoryManager.getCategory(category.id, Latest)
        } yield assert(res.getCategory.version)(equalTo(updated.version)) &&
          assert(res.getCategory.attributes)(hasFirst(hasField("version", _.version, equalTo(updated.version))))
      },
      testM("Update attribute with two categories referencing it") {
        for {
          attr <- AttributeManager.createAttribute(
            AttributeDefinition(uriPart = "update_cat_2", uniqueName = "update_cat_2Name")
          )
          category1 <- CategoryManager.createCategory(
            Category(
              uriPart = "c1_update_cat_2",
              uniqueName = "c1_update_cat_2Name",
              attributes = Seq(CategoryAttribute(attributeId = attr.id))
            )
          )
          category2 <- CategoryManager.createCategory(
            Category(
              uriPart = "c2_update_cat_2",
              uniqueName = "c2_update_cat_2Name",
              attributes = Seq(CategoryAttribute(attributeId = attr.id))
            )
          )
          updated <- AttributeManager.updateAttribute(attr.copy(name = "name"), attr.version)
          res1 <- CategoryManager.getCategory(category1.id, Latest)
          res2 <- CategoryManager.getCategory(category2.id, Latest)
        } yield assert(res1.getCategory.version)(equalTo(updated.version)) &&
          assert(res2.getCategory.version)(equalTo(updated.version)) &&
          assert(res1.getCategory.attributes)(hasFirst(hasField("version", _.version, equalTo(updated.version)))) &&
          assert(res2.getCategory.attributes)(hasFirst(hasField("version", _.version, equalTo(updated.version))))
      },
      testM("Do not replace uniq value on duplicate attribute creation") {
        for {
          attr <- AttributeManager.createAttribute(
            AttributeDefinition(uriPart = "duplicate_uniq", uniqueName = "duplicate_uniqName")
          )
          _ <- AttributeManager
            .createAttribute(AttributeDefinition(uriPart = "duplicate_uniq", uniqueName = "duplicate_uniqName"))
            .flip
          constraint <- runTx(ConstraintsDao.checkUniqValue(EntityRef.from(attr), "uri_part", attr.uriPart))
            .as(true)
        } yield assert(constraint)(isTrue)
      },
      testM("Do not create constraints if attribute is created in archived state") {
        for {
          referenced <- AttributeManager.createAttribute(
            AttributeDefinition(uriPart = "referenced_attr_2", uniqueName = "referenced_attr_2Name")
          )
          attributeToCreate = AttributeDefinition(
            uriPart = "archived_attr",
            uniqueName = "archived_attrName",
            isArchived = true,
            migrateToAttributeId = referenced.id
          )
          _ <- AttributeManager.createAttribute(attributeToCreate)
          _ <- AttributeManager.createAttribute(attributeToCreate)
          _ <- runTx(ConstraintsDao.checkNoReferences(EntityRef.from(referenced)))
        } yield assertCompletes
      },
      testM("Ё/Е normalized attribute search") {
        for {
          _ <- AttributeManager.createAttribute(
            AttributeDefinition(
              uriPart = "parameter_to_yo_normalize",
              uniqueName = "parameter_to_yo_normalizeName",
              name = "Серёжа",
              description = "Серёжа"
            )
          )
          searchManager <- ZIO.service[SearchManager.Service]
          normalized1 <- searchManager.searchAttributes(
            SearchParameters(searchString = "Серёжа"),
            LimitOffset(offset = 0, limit = 1)
          )
          normalized2 <- searchManager.searchAttributes(
            SearchParameters(searchString = "Сережа"),
            LimitOffset(offset = 0, limit = 1)
          )
          bothFound = normalized1.entities.nonEmpty && normalized2.entities.nonEmpty
        } yield assert(bothFound)(isTrue)
      }
    ).provideCustomLayer {
      val clock = Clock.live
      val ydb = TestYdb.ydb
      val logging = Logging.live
      val updateChecker = EntityUpdateChecker.live
      val txRunner = ((ydb >>> Ydb.txRunner) ++ updateChecker) >>> BonsaiTxRunner.live
      val entityDao = (ydb ++ updateChecker) >>> YdbEntityDao.live
      val historyDao = ydb >>> YdbHistoryDao.live
      val constraintsDao = ydb >>> YdbConstraintsDao.live
      val hierarchyDao = ydb >>> YdbHierarchyDao.live
      val globalHistoryDao = ydb >>> YdbGlobalHistoryDao.live
      val suggestIndexDao = ydb >>> YdbSuggestEntityIndexDao.live
      val searchManager = (entityDao ++ txRunner ++ clock ++ suggestIndexDao) >>> SearchManager.live

      val deps = entityDao ++ historyDao ++ globalHistoryDao ++ constraintsDao ++ hierarchyDao ++ suggestIndexDao ++
        txRunner ++ clock ++ logging ++ searchManager
      (deps >>> CategoryManager.live) ++ (deps >>> AttributeManager.live) ++ txRunner ++ constraintsDao ++
        suggestIndexDao ++ searchManager
    }
  }
}
