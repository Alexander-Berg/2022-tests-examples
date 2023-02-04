package ru.yandex.vertis.general.bonsai.logic.test

import java.time.Instant
import java.util.concurrent.TimeUnit
import common.scalapb.ScalaProtobuf
import common.zio.ydb.Ydb
import common.zio.ydb.testkit.TestYdb
import general.bonsai.attribute_model.AttributeDefinition
import general.bonsai.category_model.{Category, CategoryAttribute, CategoryState}
import general.bonsai.internal.internal_api.{CategoryFilter, PagingRequest, SearchParameters}
import general.bonsai.restriction_model.{Restriction, RestrictionRule}
import ru.yandex.vertis.general.bonsai.logic.{AttributeManager, CategoryManager, SearchManager}
import ru.yandex.vertis.general.bonsai.model.CommentType.Comment
import ru.yandex.vertis.general.bonsai.model.{ExactVersion, Latest, _}
import ru.yandex.vertis.general.bonsai.storage.testkit._
import ru.yandex.vertis.general.bonsai.storage.ydb._
import ru.yandex.vertis.general.bonsai.storage.ydb.sign.EntityUpdateChecker
import ru.yandex.vertis.general.bonsai.storage.{ConstraintsDao, HierarchyDao}
import ru.yandex.vertis.general.common.model.pagination.LimitOffset
import common.zio.logging.Logging
import zio.ZIO
import zio.clock.Clock
import zio.test.Assertion._
import zio.test.TestAspect._
import zio.test._

object DefaultCategoryManagerTest extends DefaultRunnableSpec {

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = {
    suite("DefaultCategoryManager")(
      testM("Create attribute and check it's id and version") {
        for {
          category <- CategoryManager.createCategory(Category(uriPart = "create1", uniqueName = "create1Name"))
        } yield assert(category.id)(isNonEmptyString)
      },
      testM("Create category and get it") {
        for {
          created <- CategoryManager.createCategory(
            Category(uriPart = "create2", uniqueName = "create2Name", state = CategoryState.DEFAULT)
          )
          returned <- CategoryManager.getCategory(created.id, ExactVersion(created.version))
        } yield assert(returned.getCategory)(equalTo(created))
      },
      testM("Throw uniq constraints violation when trying to create category with non uniq uri part") {
        for {
          _ <- CategoryManager.createCategory(Category(uriPart = "part", uniqueName = "partName"))
          result <- CategoryManager.createCategory(Category(uriPart = "part", uniqueName = "partName")).flip
        } yield assert(result)(isSubtype[UniqConstraintViolation](anything))
      },
      testM("Throw parent entity not exists when trying to create category with non existing parent") {
        for {
          result <- CategoryManager
            .createCategory(Category(parentId = "something", uriPart = "parent1", uniqueName = "parent1Name"))
            .flip
        } yield assert(result)(isSubtype[ParentEntityNotExists](anything))
      },
      testM("Can create archived category") {
        for {
          c1 <- CategoryManager.createCategory(
            Category(uriPart = "create3", uniqueName = "create3Name", state = CategoryState.ARCHIVED)
          )
          c2 <- CategoryManager.createCategory(
            Category(uriPart = "create3", uniqueName = "create3Name", state = CategoryState.ARCHIVED)
          )
        } yield assert(c1.id)(not(equalTo(c2.id))) && assert(c1.uriPart)(equalTo(c2.uriPart))
      },
      testM("Add category to hierarchy table when creating with parent") {
        for {
          parent <- CategoryManager.createCategory(Category(uriPart = "parent2", uniqueName = "parent2Name"))
          category <- CategoryManager.createCategory(
            Category(parentId = parent.id, uriPart = "parent22", uniqueName = "parent22Name")
          )
          children <- runTx(HierarchyDao.getChildren(parent.id))
        } yield assert(children)(hasSize(equalTo(1)))
      },
      testM("Create reference in constraints dao when creating category with attribute") {
        for {
          attribute <- AttributeManager.createAttribute(AttributeDefinition(uriPart = "add1", uniqueName = "add1Name"))
          category <- CategoryManager.createCategory(
            Category(uriPart = "add12", uniqueName = "add12Name", attributes = Seq(CategoryAttribute(attribute.id)))
          )
          categories <- runTx(ConstraintsDao.getIncomingReferences(EntityRef.from(attribute)))
        } yield assert(categories)(hasSize(equalTo(1)))
      },
      testM(
        "Throw restriction attributes missing when some attributes that exist in restriction, missing at category"
      ) {
        for {
          result <-
            CategoryManager
              .createCategory(
                Category(
                  uriPart = "restrictionMissing",
                  uniqueName = "restrictionMissingName",
                  restrictionRules = Seq(RestrictionRule(restrictions = Seq(Restriction(attributeId = "some"))))
                )
              )
              .flip
        } yield assert(result)(isSubtype[RestrictionAttributesMissing](anything))
      },
      testM("Throw duplicate attributes when trying to create category with duplicate attributes") {
        for {
          attrbute <- AttributeManager.createAttribute(
            AttributeDefinition(uriPart = "createDuplicateAtribute", uniqueName = "createDuplicateAtributeName")
          )
          result <-
            CategoryManager
              .createCategory(
                Category(
                  uriPart = "createDuplicateAttribute",
                  uniqueName = "createDuplicateAttributeName",
                  attributes = Seq(CategoryAttribute(attrbute.id), CategoryAttribute(attrbute.id))
                )
              )
              .flip
        } yield assert(result)(isSubtype[DuplicateAttributes](anything))
      },
      testM("Throw not found when trying to get non existing category") {
        for {
          result <- CategoryManager.getCategory("someid", Latest).flip
        } yield assert(result)(isSubtype[EntityNotFound](anything))
      },
      testM("List categories without filter and paging") {
        for {
          _ <- CategoryManager.createCategory(
            Category(uriPart = "list_base1", uniqueName = "list_base1Name", innerName = "list_base1")
          )
          _ <- CategoryManager.createCategory(
            Category(uriPart = "list_base2", uniqueName = "list_base2Name", innerName = "list_base2")
          )
          list <-
            CategoryManager.listCategories(CategoryFilter(namePrefixFilter = "list_base"), PagingRequest(limit = 10))
        } yield assert(list.categories)(isNonEmpty)
      },
      testM("List categories without filter and paging #2") {
        for {
          _ <- CategoryManager.createCategory(
            Category(uriPart = "list_paging1", uniqueName = "list_paging1Name", innerName = "list_paging1")
          )
          _ <- CategoryManager.createCategory(
            Category(uriPart = "list_paging2", uniqueName = "list_paging2Name", innerName = "list_paging2")
          )
          _ <- CategoryManager.createCategory(
            Category(uriPart = "list_paging3", uniqueName = "list_paging3Name", innerName = "list_paging3")
          )
          _ <- CategoryManager.createCategory(
            Category(uriPart = "list_paging4", uniqueName = "list_paging4Name", innerName = "list_paging4")
          )
          _ <- CategoryManager.createCategory(
            Category(uriPart = "list_paging5", uniqueName = "list_paging5Name", innerName = "list_paging5")
          )
          list <-
            CategoryManager.listCategories(CategoryFilter(namePrefixFilter = "list_paging"), PagingRequest(limit = 3))
          list2 <- CategoryManager.listCategories(
            CategoryFilter(namePrefixFilter = "list_paging"),
            PagingRequest(limit = 3, cursor = list.getPaging.cursorEnd)
          )
        } yield assert(list.categories)(hasSize(equalTo(3))) &&
          assert(list.getPaging.hasNextPage)(isTrue) &&
          assert(list2.categories)(hasSize(equalTo(2))) &&
          assert(list2.getPaging.hasNextPage)(isFalse)
      },
      testM("List categories with filtering archived categories") {
        for {
          _ <- CategoryManager.createCategory(
            Category(uriPart = "list_archived1", uniqueName = "list_archived1Name", innerName = "list_archived1")
          )
          _ <- CategoryManager.createCategory(
            Category(
              uriPart = "list_archived2",
              uniqueName = "list_archived2Name",
              innerName = "list_archived2",
              state = CategoryState.ARCHIVED
            )
          )
          list <- CategoryManager.listCategories(
            CategoryFilter(namePrefixFilter = "list_archived", onlyActive = true),
            PagingRequest(limit = 10)
          )
        } yield assert(list.categories)(hasSize(equalTo(1)))
      },
      testM("List categories with filtering symlinks") {
        for {
          _ <- CategoryManager.createCategory(
            Category(uriPart = "list_symlink1", uniqueName = "list_symlink1Name", innerName = "list_symlink1")
          )
          cat <- CategoryManager.createCategory(
            Category(uriPart = "list_symlink2", uniqueName = "list_symlink2Name", innerName = "list_symlink2")
          )
          _ <- CategoryManager.createCategory(
            Category(
              uriPart = "list_symlink3",
              uniqueName = "list_symlink3Name",
              innerName = "list_symlink3",
              symlinkToCategoryId = cat.id
            )
          )
          list <- CategoryManager.listCategories(
            CategoryFilter(namePrefixFilter = "list_symlink", skipSymlink = true),
            PagingRequest(limit = 10)
          )
        } yield assert(list.categories)(hasSize(equalTo(2)))
      },
      testM("Update created category") {
        for {
          created <- CategoryManager.createCategory(
            Category(uriPart = "update1", uniqueName = "update1Name", state = CategoryState.DEFAULT)
          )
          updated <- CategoryManager.updateCategory(created.copy(shortName = "sn"), created.version)
          afterUpdate <- CategoryManager.getCategory(updated.id, ExactVersion(updated.version))
        } yield assert(afterUpdate.getCategory)(equalTo(updated))
      },
      testM("Allow to modify uri_part") {
        for {
          created <- CategoryManager.createCategory(
            Category(uriPart = "uriPartModifiable", uniqueName = "uriPartModifiableName")
          )
          _ <- CategoryManager.updateCategory(
            created.copy(uriPart = "uriPartAnother", uniqueName = "uriPartAnotherName"),
            created.version
          )
          res1 <- CategoryManager
            .createCategory(Category(uriPart = "uriPartModifiable", uniqueName = "uriPartModifiableName"))
            .run
          res2 <- CategoryManager
            .createCategory(Category(uriPart = "uriPartAnother", uniqueName = "uriPartAnotherName"))
            .run
        } yield assert(res1)(succeeds(anything)) && assert(res2)(fails(isSubtype[UniqConstraintViolation](anything)))
      },
      testM("Archive with uri_part change") {
        for {
          created <- CategoryManager.createCategory(
            Category(uriPart = "uriPartArchive", uniqueName = "uriPartArchiveName")
          )
          _ <- CategoryManager.createCategory(Category(uriPart = "uriPartArchive2", uniqueName = "uriPartArchive2Name"))

          _ <- CategoryManager.updateCategory(
            created.copy(
              uriPart = "uriPartArchive2",
              uniqueName = "uriPartArchive2Name",
              state = CategoryState.ARCHIVED
            ),
            created.version
          )

          res <- CategoryManager
            .createCategory(Category(uriPart = "uriPartArchive", uniqueName = "uriPartArchiveName"))
            .run
        } yield assert(res.untraced)(succeeds(anything))
      },
      testM("Restore with uri_part change") {
        for {
          created <- CategoryManager.createCategory(
            Category(uriPart = "uriPartRestore", uniqueName = "uriPartRestoreName", state = CategoryState.ARCHIVED)
          )
          _ <- CategoryManager.createCategory(
            Category(uriPart = "uriPartRestore", uniqueName = "uriPartRestoreName", state = CategoryState.DEFAULT)
          )

          _ <- CategoryManager.updateCategory(
            created.copy(
              uriPart = "uriPartRestore2",
              uniqueName = "uriPartRestore2Name",
              state = CategoryState.DEFAULT
            ),
            created.version
          )

          res <- CategoryManager
            .createCategory(Category(uriPart = "uriPartRestore2", uniqueName = "uriPartRestore2Name"))
            .run
        } yield assert(res)(fails(isSubtype[UniqConstraintViolation](anything)))
      },
      testM("Throw entity already modified when trying to update old version") {
        for {
          created <- CategoryManager.createCategory(Category(uriPart = "modifyOld", uniqueName = "modifyOldName"))
          _ <- CategoryManager.updateCategory(created.copy(innerName = "descrr"), created.version)
          result <- CategoryManager.updateCategory(created.copy(innerName = "descrr"), created.version).flip
        } yield assert(result)(isSubtype[EntityAlreadyModified](anything))
      },
      testM("Throw hierarchy cycle when trying to create hierarchy cycle") {
        for {
          parent <- CategoryManager.createCategory(Category(uriPart = "cycle1", uniqueName = "cycle1Name"))
          created <- CategoryManager.createCategory(
            Category(uriPart = "cycle2", uniqueName = "cycle2Name", parentId = parent.id)
          )
          result <- CategoryManager.updateCategory(parent.copy(parentId = created.id), parent.version).flip
        } yield assert(result)(isSubtype[CategoriesHierarchyCycle](anything))
      },
      testM("Throw hierarchy cycle when trying to set category to be parent of it self") {
        for {
          created <- CategoryManager.createCategory(Category(uriPart = "selfCycle", uniqueName = "selfCycleName"))
          result <- CategoryManager.updateCategory(created.copy(parentId = created.id), created.version).flip
        } yield assert(result)(isSubtype[CategoriesHierarchyCycle](anything))
      },
      testM("Throw duplicate attributes when trying to update category via adding duplicate attributes") {
        for {
          attrbute <- AttributeManager.createAttribute(
            AttributeDefinition(uriPart = "updateDuplicateAttribute", uniqueName = "updateDuplicateAttributeName")
          )
          category <- CategoryManager.createCategory(
            Category(uriPart = "updateDuplicateAttribute", uniqueName = "updateDuplicateAttributeName")
          )
          result <-
            CategoryManager
              .updateCategory(
                category.copy(attributes = Seq(CategoryAttribute(attrbute.id), CategoryAttribute(attrbute.id))),
                category.version
              )
              .flip
        } yield assert(result)(isSubtype[DuplicateAttributes](anything))
      },
      testM("Clear references when deleting attributes") {
        for {
          attribute <- AttributeManager.createAttribute(
            AttributeDefinition(uriPart = "clearRef", uniqueName = "clearRefName")
          )
          category <- CategoryManager.createCategory(
            Category(
              uriPart = "clearRef",
              uniqueName = "clearRefName",
              attributes = Seq(CategoryAttribute(attribute.id))
            )
          )
          refs <- runTx(ConstraintsDao.getIncomingReferences(EntityRef.from(attribute)))
          _ <-
            CategoryManager.updateCategory(category.copy(attributes = Seq.empty[CategoryAttribute]), category.version)
          refsUpdated <- runTx(ConstraintsDao.getIncomingReferences(EntityRef.from(attribute)))
        } yield assert(refs)(hasSize(equalTo(1))) && assert(refsUpdated)(isEmpty)
      },
      testM("Add references when adding attributes") {
        for {
          attribute <- AttributeManager.createAttribute(
            AttributeDefinition(uriPart = "addRef", uniqueName = "addRefName")
          )
          category <- CategoryManager.createCategory(
            Category(uriPart = "addRef", uniqueName = "addRefName", attributes = Seq.empty)
          )
          refs <- runTx(ConstraintsDao.getIncomingReferences(EntityRef.from(attribute)))
          _ <- CategoryManager.updateCategory(
            category.copy(attributes = Seq(CategoryAttribute(attribute.id))),
            category.version
          )
          refsUpdated <- runTx(ConstraintsDao.getIncomingReferences(EntityRef.from(attribute)))
        } yield assert(refs)(isEmpty) && assert(refsUpdated)(hasSize(equalTo(1)))
      },
      testM("Do not add reference if attribute is added to archived category") {
        for {
          attribute <- AttributeManager.createAttribute(
            AttributeDefinition(uriPart = "attribute_to_add", uniqueName = "attribute_to_addName")
          )
          category <- CategoryManager.createCategory(
            Category(
              uriPart = "archived_category",
              uniqueName = "archived_categoryName",
              attributes = Seq.empty,
              state = CategoryState.ARCHIVED
            )
          )
          refs <- runTx(ConstraintsDao.getIncomingReferences(EntityRef.from(attribute)))
          _ <- CategoryManager.addAttribute(
            categoryId = category.id,
            attributeId = attribute.id
          )
          refsUpdated <- runTx(ConstraintsDao.getIncomingReferences(EntityRef.from(attribute)))
        } yield assert(refs)(isEmpty) && assert(refsUpdated)(isEmpty)
      },
      testM("Return category with old version") {
        for {
          created <- CategoryManager.createCategory(
            Category(uriPart = "update2", uniqueName = "update2Name", state = CategoryState.DEFAULT)
          )
          updated <- CategoryManager.updateCategory(created.copy(shortName = "sn"), created.version)
          oldVersion <- CategoryManager.getCategory(updated.id, ExactVersion(created.version))
        } yield assert(oldVersion.getCategory)(equalTo(created))
      },
      testM("Move category") {
        for {
          parent <- CategoryManager.createCategory(Category(uriPart = "move1", uniqueName = "move1Name"))
          newParent <- CategoryManager.createCategory(Category(uriPart = "move11", uniqueName = "move11Name"))
          category <- CategoryManager.createCategory(
            Category(uriPart = "move12", uniqueName = "move12Name", parentId = parent.id)
          )
          moved <- CategoryManager.moveCategory(category.id, newParent.id)
        } yield assert(moved.parentId)(equalTo(newParent.id))
      },
      testM("Change data in hierarchy table when moving category") {
        for {
          parent <- CategoryManager.createCategory(Category(uriPart = "move2", uniqueName = "move2Name"))
          newParent <- CategoryManager.createCategory(Category(uriPart = "move21", uniqueName = "move21Name"))
          category <- CategoryManager.createCategory(
            Category(uriPart = "move22", uniqueName = "move22Name", parentId = parent.id)
          )
          _ <- CategoryManager.moveCategory(category.id, newParent.id)
          oldParendChildren <- runTx(HierarchyDao.getChildren(parent.id))
          newParentChildren <- runTx(HierarchyDao.getChildren(newParent.id))
        } yield assert(oldParendChildren)(isEmpty) && assert(newParentChildren)(hasSize(equalTo(1)))
      },
      testM("Throw parent not exist when moving category to non existing parent") {
        for {
          parent <- CategoryManager.createCategory(Category(uriPart = "move3", uniqueName = "move3Name"))
          category <- CategoryManager.createCategory(
            Category(uriPart = "move31", uniqueName = "move31Name", parentId = parent.id)
          )
          result <- CategoryManager.moveCategory(category.id, "newId").flip
        } yield assert(result)(isSubtype[ParentEntityNotExists](anything))
      },
      testM("Archive category") {
        for {
          created <- CategoryManager.createCategory(Category(uriPart = "archive1", uniqueName = "archive1Name"))
          _ <- CategoryManager.archiveCategory(created.id)
          archived <- CategoryManager.getCategory(created.id, Latest)
        } yield assert(archived.getCategory.state.isArchived)(isTrue)
      },
      testM("Delete uniq constraints and attribute references when archiving category") {
        for {
          attribute <- AttributeManager.createAttribute(
            AttributeDefinition(uriPart = "archiveClear", uniqueName = "archiveClearName")
          )
          first <- CategoryManager.createCategory(Category(uriPart = "archiveClear1", uniqueName = "archiveClear1Name"))
          second <- CategoryManager.createCategory(
            Category(
              uriPart = "archiveClear2",
              uniqueName = "archiveClear2Name",
              attributes = Seq(CategoryAttribute(attribute.id))
            )
          )
          _ <- CategoryManager.archiveCategory(second.id)
          references <- runTx(ConstraintsDao.getIncomingReferences(EntityRef.from(attribute)))
          valueFree <- runTx(ConstraintsDao.checkUniqValue(EntityRef("category", "newId"), "uriPart", "archiveClear"))
        } yield assert(valueFree)(isTrue) && assert(references)(isEmpty)
      },
      testM("Restore archived category") {
        for {
          created <- CategoryManager.createCategory(
            Category(uriPart = "restore1", uniqueName = "restore1Name", state = CategoryState.DEFAULT)
          )
          _ <- CategoryManager.archiveCategory(created.id)
          _ <- CategoryManager.restoreCategory(created.id)
          restored <- CategoryManager.getCategory(created.id, Latest)
        } yield assert(restored.getCategory.state.isArchived)(isFalse)
      },
      testM("Add references to attributes when restoring category") {
        for {
          attribute <- AttributeManager.createAttribute(
            AttributeDefinition(uriPart = "restorerefs", uniqueName = "restorerefsName")
          )
          created <- CategoryManager.createCategory(
            Category(
              uriPart = "restorerefs",
              uniqueName = "restorerefsName",
              attributes = Seq(CategoryAttribute(attribute.id))
            )
          )
          _ <- CategoryManager.archiveCategory(created.id)
          refs <- runTx(ConstraintsDao.getIncomingReferences(EntityRef.from(attribute)))
          _ <- CategoryManager.restoreCategory(created.id)
          refsRestored <- runTx(ConstraintsDao.getIncomingReferences(EntityRef.from(attribute)))
        } yield assert(refs)(isEmpty) && assert(refsRestored)(hasSize(equalTo(1)))
      },
      testM("Throw constraints violation when trying to restore category with non uniq uriPart") {
        for {
          first <- CategoryManager.createCategory(Category(uriPart = "nonuniq", uniqueName = "nonuniqName"))
          _ <- CategoryManager.archiveCategory(first.id)
          _ <- CategoryManager.createCategory(Category(uriPart = "nonuniq", uniqueName = "nonuniqName"))
          result <- CategoryManager.restoreCategory(first.id).flip
        } yield assert(result)(isSubtype[UniqConstraintViolation](anything))
      },
      testM("Throw constraints violation when trying to archive category with external references") {
        for {
          first <- CategoryManager.createCategory(
            Category(uriPart = "archive_referenced1", uniqueName = "archive_referenced1Name")
          )
          _ <- CategoryManager.createCategory(
            Category(
              uriPart = "archive_referenced2",
              uniqueName = "archive_referenced2Name",
              symlinkToCategoryId = first.id
            )
          )
          result <- CategoryManager.archiveCategory(first.id).flip
        } yield assert(result)(isSubtype[ExternalReferenceViolation](anything))
      },
      testM("Throw error when trying to create category with missing link to category") {
        for {
          first <-
            CategoryManager
              .createCategory(
                Category(
                  uriPart = "create_missing_category_link1",
                  uniqueName = "create_missing_category_link1Name",
                  symlinkToCategoryId = "create_missing_category_link2"
                )
              )
              .flip
        } yield assert(first)(isSubtype[ReferenceEntitiesNotExists](anything))
      },
      testM("Throw error when trying to create category with archived link to category") {
        for {
          first <- CategoryManager.createCategory(
            Category(uriPart = "create_archived_category_link1", uniqueName = "create_archived_category_link1Name")
          )
          _ <- CategoryManager.archiveCategory(first.id)
          second <-
            CategoryManager
              .createCategory(
                Category(
                  uriPart = "archived_category_link2",
                  uniqueName = "archived_category_link2Name",
                  symlinkToCategoryId = first.id
                )
              )
              .flip
        } yield assert(second)(isSubtype[ReferenceEntitiesAreArchived](anything))
      },
      testM("Throw error when trying to restore category with missing link to category") {
        for {
          first <- CategoryManager.createCategory(
            Category(uriPart = "restore_archived_category_link1", uniqueName = "restore_archived_category_link1Name")
          )
          another <- CategoryManager.createCategory(
            Category(
              uriPart = "restore_archived_category_link2",
              uniqueName = "restore_archived_category_link2Name",
              symlinkToCategoryId = first.id
            )
          )
          _ <- CategoryManager.archiveCategory(another.id)
          _ <- CategoryManager.archiveCategory(first.id)
          result <- CategoryManager.restoreCategory(another.id).flip
        } yield assert(result)(isSubtype[ReferenceEntitiesAreArchived](anything))
      },
      testM("Throw error when trying to create category with missing link to attribute") {
        for {
          first <-
            CategoryManager
              .createCategory(
                Category(
                  uriPart = "create_missing_attribute_link1",
                  uniqueName = "create_missing_attribute_link1Name",
                  attributes = Seq(CategoryAttribute("create_missing_attribute_link2"))
                )
              )
              .flip
        } yield assert(first)(isSubtype[ReferenceEntitiesNotExists](anything))
      },
      testM("Throw error when trying to create category with archived link to category") {
        for {
          attribute <-
            AttributeManager.createAttribute(
              AttributeDefinition(
                uriPart = "create_archived_attribute_link1",
                uniqueName = "create_archived_attribute_link1Name"
              )
            )
          _ <- AttributeManager.archiveAttribute(attribute.id)
          result <-
            CategoryManager
              .createCategory(
                Category(
                  uriPart = "archived_category_link2",
                  uniqueName = "archived_category_link2Name",
                  attributes = Seq(CategoryAttribute(attribute.id))
                )
              )
              .flip
        } yield assert(result)(isSubtype[ReferenceEntitiesAreArchived](anything))
      },
      testM("Reorder categories") {
        for {
          previous <- CategoryManager.createCategory(
            Category(order = 1, uriPart = "reorder1", uniqueName = "reorder1Name")
          )
          next <- CategoryManager.createCategory(
            Category(order = 5, uriPart = "reorder11", uniqueName = "reorder11Name")
          )
          category <- CategoryManager.createCategory(
            Category(order = 2, uriPart = "reorder12", uniqueName = "reorder12Name")
          )
          reordered <- CategoryManager.reorderCategories(category.id, previous.id, next.id)
        } yield assert(reordered.order)(equalTo(3.0))
      },
      testM("Throw parent entity not exists when reordered categories have different parents") {
        for {
          parent <- CategoryManager.createCategory(
            Category(uriPart = "reorderParent", uniqueName = "reorderParentName")
          )
          previous <-
            CategoryManager.createCategory(
              Category(order = 1, uriPart = "reorderParent1", uniqueName = "reorderParent1Name", parentId = parent.id)
            )
          next <- CategoryManager.createCategory(
            Category(order = 5, uriPart = "reorderParent2", uniqueName = "reorderParent2Name")
          )
          category <- CategoryManager.createCategory(
            Category(order = 2, uriPart = "reorderParent3", uniqueName = "reorderParent3Name")
          )
          result <- CategoryManager.reorderCategories(category.id, previous.id, next.id).flip
        } yield assert(result)(isSubtype[ParentEntityNotExists](anything))
      },
      testM("Add attribute to category") {
        for {
          category <- CategoryManager.createCategory(
            Category(uriPart = "addattribute1", uniqueName = "addattribute1Name")
          )
          attribute <- AttributeManager.createAttribute(
            AttributeDefinition(uriPart = "addattribute12", uniqueName = "addattribute12Name")
          )
          _ <- CategoryManager.addAttribute(category.id, attribute.id)
          withAttribute <- CategoryManager.getCategory(category.id, Latest)
        } yield assert(withAttribute.getCategory.attributes)(hasSize(equalTo(1)))
      },
      testM("Add attribute reference when adding attribute to category") {
        for {
          category <- CategoryManager.createCategory(
            Category(uriPart = "attributeaddref", uniqueName = "attributeaddrefName")
          )
          attribute <- AttributeManager.createAttribute(
            AttributeDefinition(uriPart = "attributeaddref", uniqueName = "attributeaddrefName")
          )
          _ <- CategoryManager.addAttribute(category.id, attribute.id)
          refs <- runTx(ConstraintsDao.getIncomingReferences(EntityRef.from(attribute)))
        } yield assert(refs)(hasSize(equalTo(1)))
      },
      testM("Throw duplicate attributes when trying add duplicate attribute") {
        for {
          attribute <- AttributeManager.createAttribute(
            AttributeDefinition(uriPart = "addDuplicateAttribute", uniqueName = "addDuplicateAttributeName")
          )
          category <- CategoryManager.createCategory(
            Category(
              uriPart = "addDuplicateAttribute",
              uniqueName = "addDuplicateAttributeName",
              attributes = Seq(CategoryAttribute(attribute.id))
            )
          )
          result <- CategoryManager.addAttribute(category.id, attribute.id).flip
        } yield assert(result)(isSubtype[DuplicateAttributes](anything))
      },
      testM("List category history") {
        for {
          createdCategory <- CategoryManager.createCategory(
            Category(uriPart = "initial", uniqueName = "initialName", state = CategoryState.DEFAULT),
            NewVersion(0L, "create")
          )
          updatedCategory <- CategoryManager.updateCategory(
            createdCategory.copy(shortName = "updated"),
            createdCategory.version,
            NewVersion(0L, "update")
          )
          _ <- CategoryManager.archiveCategory(updatedCategory.id, NewVersion(0L, "archive"))
          archivedCategory <- CategoryManager.getCategory(updatedCategory.id, Latest).map(_.getCategory)
          attribute <- AttributeManager.createAttribute(
            AttributeDefinition(uriPart = "history attribute", uniqueName = "history attributeName")
          )
          categoryWithAttribute <-
            CategoryManager.addAttribute(archivedCategory.id, attribute.id, NewVersion(0L, "attribute"))

          history <- CategoryManager
            .getHistory(categoryId = createdCategory.id, paging = blankPagingHistoryRequest)
            .map(_.historyEntries)
          commentsAssert = assert(history.map(_.comment))(
            equalTo(Seq("attribute", "archive", "update", "create"))
          )

          now <- zio.clock.instant
          timestamps = history.flatMap(_.timestamp).map(ScalaProtobuf.timestampToInstant)
          timestampsNumber = assert(timestamps)(hasSize(equalTo(4)))
          timestampsTime = assert(timestamps)(hasLast(isGreaterThan(now.minusSeconds(100))))
          timestampsSorted = assert(timestamps)(isSortedReverse)
          timestampsAssert = timestampsNumber && timestampsTime && timestampsSorted

          historyCategories = history.map(_.category)
          expectedCategories =
            Seq(categoryWithAttribute, archivedCategory, updatedCategory, createdCategory).map(Some(_))
          categoriesAssert = assert(historyCategories)(equalTo(expectedCategories))
        } yield commentsAssert && timestampsAssert && categoriesAssert
      },
      testM("Get categories by attribute usage") {
        for {
          attribute <- AttributeManager.createAttribute(
            AttributeDefinition(uriPart = "multiUse", uniqueName = "multiUseName")
          )
          category1 <- CategoryManager.createCategory(
            Category(
              uriPart = "multiUse_1",
              uniqueName = "multiUse_1Name",
              attributes = Seq(CategoryAttribute(attribute.id))
            )
          )
          category2 <- CategoryManager.createCategory(
            Category(
              uriPart = "multiUse_2",
              uniqueName = "multiUse_2Name",
              attributes = Seq(CategoryAttribute(attribute.id))
            )
          )
          result <- CategoryManager.listCategoriesWithAttribute(attribute.id)
          resultIds = result.map(_.id)
        } yield assert(resultIds)(equalTo(Set(category1.id, category2.id)))
      },
      testM("Check that no entity references category when archiving") {
        for {
          referenced <- CategoryManager.createCategory(
            Category(uriPart = "referenced_category", uniqueName = "referenced_categoryName")
          )
          _ <- CategoryManager.createCategory(
            Category(
              uriPart = "referencing_category",
              uniqueName = "referencing_categoryName",
              symlinkToCategoryId = referenced.id
            )
          )
          result <- CategoryManager
            .updateCategory(referenced.copy(state = CategoryState.ARCHIVED), version = referenced.version)
            .run
        } yield assert(result)(fails(isSubtype[ExternalReferenceViolation](anything)))
      },
      testM("Do not archive category if it has active child") {
        for {
          parent <- CategoryManager.createCategory(
            Category(uriPart = "parent_category", uniqueName = "parent_categoryName")
          )
          child1 <- CategoryManager.createCategory(
            Category(uriPart = "child_1", uniqueName = "child_1Name", parentId = parent.id)
          )
          child2 <- CategoryManager.createCategory(
            Category(uriPart = "child_2", uniqueName = "child_2Name", parentId = parent.id)
          )

          updateBothChildrenActive <- CategoryManager
            .updateCategory(parent.copy(state = CategoryState.ARCHIVED), parent.version)
            .run
          archivingBothChildrenActive <- CategoryManager.archiveCategory(parent.id).run

          _ <- CategoryManager.archiveCategory(child1.id)
          updateOneChildActive <- CategoryManager
            .updateCategory(parent.copy(state = CategoryState.ARCHIVED), parent.version)
            .run
          archivingOneChildActive <- CategoryManager.archiveCategory(parent.id).run

          _ <- CategoryManager.archiveCategory(child2.id)
          _ <- CategoryManager.updateCategory(parent.copy(state = CategoryState.ARCHIVED), parent.version)
          _ <- CategoryManager.restoreCategory(parent.id) // restore to archive again
          _ <- CategoryManager.archiveCategory(parent.id)
        } yield assert(
          Seq(
            archivingBothChildrenActive,
            updateBothChildrenActive,
            archivingOneChildActive,
            updateOneChildActive
          )
        )(forall(fails(isSubtype[ExternalReferenceViolation](anything))))
      },
      testM("Do not restore category if its parent is archived") {
        for {
          parent <- CategoryManager.createCategory(
            Category(uriPart = "parent_category_2", uniqueName = "parent_category_2Name", state = CategoryState.DEFAULT)
          )
          child1 <- CategoryManager.createCategory(
            Category(
              uriPart = "child_3",
              uniqueName = "child_3Name",
              parentId = parent.id,
              state = CategoryState.DEFAULT
            )
          )
          _ <- CategoryManager.archiveCategory(child1.id)
          _ <- CategoryManager.archiveCategory(parent.id)
          childVersion <- CategoryManager.getCategory(child1.id, Latest).map(_.category.get.version)
          updateResult <- CategoryManager.updateCategory(child1.copy(state = CategoryState.DEFAULT), childVersion).run
          restoreResult <- CategoryManager.restoreCategory(child1.id).run

          _ <- CategoryManager.restoreCategory(parent.id)
          _ <- CategoryManager.updateCategory(child1.copy(state = CategoryState.DEFAULT), childVersion)
          _ <- CategoryManager.archiveCategory(child1.id) // archive to restore again
          _ <- CategoryManager.restoreCategory(child1.id)
        } yield assert(Seq(restoreResult, updateResult))(
          forall(fails(isSubtype[ReferenceEntitiesAreArchived](anything)))
        )
      },
      testM("Throw duplicate attribute names when trying to create category with attributes having non-unique names") {
        for {
          attribute1 <- AttributeManager.createAttribute(
            AttributeDefinition(uriPart = "dup-name-1", uniqueName = "non-unique-unique-name")
          )
          attribute2 <- AttributeManager.createAttribute(
            AttributeDefinition(uriPart = "dup-name-2", uniqueName = "non-unique-unique-name")
          )
          result <-
            CategoryManager
              .createCategory(
                Category(
                  uriPart = "createDuplicateAttribute",
                  uniqueName = "createDuplicateAttributeName",
                  attributes = Seq(CategoryAttribute(attribute1.id), CategoryAttribute(attribute2.id))
                )
              )
              .run
        } yield assert(result)(fails(isSubtype[DuplicateAttributeNames](anything)))
      },
      testM("Не валидировать unique_name для симлинков") {
        for {
          first <- CategoryManager.createCategory(
            Category(uriPart = "symlink_unique_name_0", uniqueName = "симлинка название")
          )
          symlink <- CategoryManager.createCategory(
            Category(
              uriPart = "symlink_unique_name_1",
              uniqueName = "симлинка название",
              symlinkToCategoryId = first.id
            )
          )
          _ <- CategoryManager.archiveCategory(symlink.id)
          archived <- CategoryManager.getCategory(symlink.id, Latest).flatMap(v => ZIO.fromOption(v.category))
          noSymlink <- CategoryManager.updateCategory(
            archived.copy(symlinkToCategoryId = ""),
            version = archived.version
          )
          restoreNonUnique <- CategoryManager.restoreCategory(noSymlink.id).run
          changeUniqueName <- CategoryManager.updateCategory(
            noSymlink.copy(uniqueName = "просто название"),
            noSymlink.version
          )
          _ <- CategoryManager.restoreCategory(changeUniqueName.id)
          restored <- CategoryManager.getCategory(changeUniqueName.id, Latest).flatMap(v => ZIO.fromOption(v.category))
          _ <- CategoryManager.updateCategory(
            restored.copy(uniqueName = first.uniqueName, symlinkToCategoryId = first.id),
            restored.version
          )
        } yield assert(restoreNonUnique)(fails(isSubtype[UniqConstraintViolation](anything)))
      },
      testM("Ё/Е normalized category search") {
        for {
          _ <- CategoryManager.createCategory(
            Category(
              uriPart = "category_to_yo_normalize",
              uniqueName = "parameter_to_yo_normalizeName",
              name = "Серёжа"
            )
          )
          searchManager <- ZIO.service[SearchManager.Service]
          normalized1 <- searchManager.searchCategories(
            SearchParameters(searchString = "Серёжа"),
            LimitOffset(offset = 0, limit = 1)
          )
          normalized2 <- searchManager.searchCategories(
            SearchParameters(searchString = "Сережа"),
            LimitOffset(offset = 0, limit = 1)
          )
          bothFound = normalized1.entities.nonEmpty && normalized2.entities.nonEmpty
        } yield assert(bothFound)(isTrue)
      },
      testM("Category symlinks search") {
        for {
          createdCategory <- CategoryManager.createCategory(
            Category(
              uriPart = "category_symlinks_search",
              uniqueName = "category_symlinks_searchName"
            )
          )
          _ <- CategoryManager.createCategory(
            Category(
              uriPart = "category_symlinks_search_symlink1",
              uniqueName = "category_symlinks_search_symlink1Name",
              symlinkToCategoryId = createdCategory.id
            )
          )
          _ <- CategoryManager.createCategory(
            Category(
              uriPart = "category_symlinks_search_symlink2",
              uniqueName = "category_symlinks_search_symlink2Name",
              symlinkToCategoryId = createdCategory.id
            )
          )
          foundSymlinks <- CategoryManager.getCategorySymlinks(createdCategory.id)
          test = (foundSymlinks.length == 2) && foundSymlinks.forall(_.symlinkToCategoryId == createdCategory.id)
        } yield assert(test)(isTrue)
      }
    )
  }.provideCustomLayer {
    val clock = Clock.live
    val logging = Logging.live
    val ydb = TestYdb.ydb
    val updateChecker = EntityUpdateChecker.live
    val txRunner = ((ydb >>> Ydb.txRunner) ++ updateChecker) >>> BonsaiTxRunner.live
    val entityDao = (ydb ++ updateChecker) >>> YdbEntityDao.live
    val historyDao = ydb >>> YdbHistoryDao.live
    val constraintsDao = ydb >>> YdbConstraintsDao.live
    val hierarchyDao = ydb >>> YdbHierarchyDao.live
    val globalHistoryDao = ydb >>> YdbGlobalHistoryDao.live
    val suggestIndexDao = ydb >>> YdbSuggestEntityIndexDao.live
    val searchManager = (entityDao ++ txRunner ++ clock ++ suggestIndexDao) >>> SearchManager.live

    val deps =
      entityDao ++ historyDao ++ globalHistoryDao ++ constraintsDao ++ suggestIndexDao ++ hierarchyDao ++ txRunner ++ clock ++ logging
    (deps >>> CategoryManager.live) ++ (deps >>> AttributeManager.live) ++ constraintsDao ++ hierarchyDao ++ txRunner ++
      searchManager
  }
}
