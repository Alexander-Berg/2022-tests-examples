package ru.yandex.vertis.general.search.logic.test

import common.geobase.model.RegionIds
import general.globe.api.GetRegionsResponse
import general.globe.model.Region
import ru.yandex.vertis.general.bonsai.public.BonsaiSnapshot
import ru.yandex.vertis.general.globe.testkit.TestGeoService
import ru.yandex.vertis.general.search.logic.VasgenFilterMapper
import ru.yandex.vertis.general.search.model.{SearchFilter, _}
import common.zio.logging.Logging
import vertis.vasgen.common.IntegerValue.Primitive.Sint64
import vertis.vasgen.common.{IntegerValue, RawValue}
import vertis.vasgen.query.Filter
import zio.test.Assertion._
import zio.test.{DefaultRunnableSpec, _}
import zio.{ZIO, ZRef}

object DefaultVasgenFilterMapperTest extends DefaultRunnableSpec {

  val categoryName = "category-0"
  val categories = Seq(categoryName)
  val paramFieldName = "field-0"
  val params = Seq(SearchFilter(paramFieldName, In(Seq(StringValue("test")))))
  val area = Toponyms(RegionIds.Voronezh, Nil, Nil)
  val Russia = Toponyms(RegionIds.Russia, Nil, Nil)

  val VoronezhParentId = 123123123L

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] =
    suite("VasgenFilterMapper")(
      testM("Expand filter") {
        for {
          _ <- TestGeoService.setGetAllParentRegionsResponse(_ =>
            ZIO.succeed(
              GetRegionsResponse(
                regions = Map(
                  RegionIds.Voronezh.id -> Region(id = RegionIds.Voronezh.id, parentId = VoronezhParentId),
                  VoronezhParentId -> Region(id = VoronezhParentId, parentId = RegionIds.Russia.id),
                  RegionIds.Russia.id -> Region(id = RegionIds.Russia.id, parentId = RegionIds.Eurasia.id),
                  RegionIds.Eurasia.id -> Region(id = RegionIds.Eurasia.id, parentId = 0)
                )
              )
            )
          )
          noExpansion <- VasgenFilterMapper.createExpandedVasgenFilters(
            categories,
            params,
            area,
            0,
            SearchContext.Unset
          )
          voronezhParentExpansion <- VasgenFilterMapper.createExpandedVasgenFilters(
            categories,
            params,
            area,
            1,
            SearchContext.Unset
          )
          russiaExpansion <- VasgenFilterMapper.createExpandedVasgenFilters(
            categories,
            params,
            area,
            2,
            SearchContext.Unset
          )
          voronezhWithoutFilters <- VasgenFilterMapper.createExpandedVasgenFilters(
            categories,
            params,
            area,
            3,
            SearchContext.Unset
          )
          voronezhParentWithoutFilters <- VasgenFilterMapper.createExpandedVasgenFilters(
            categories,
            params,
            area,
            4,
            SearchContext.Unset
          )
          russiaWithoutFilters <- VasgenFilterMapper.createExpandedVasgenFilters(
            categories,
            params,
            area,
            5,
            SearchContext.Unset
          )
          fail <- VasgenFilterMapper
            .createExpandedVasgenFilters(categories, params, area, 6, SearchContext.Unset)
            .run

        } yield assert(voronezhParentExpansion.filter.and)(exists(equalTo(Filter(not = Some(noExpansion.filter))))) &&
          assert(russiaExpansion.filter.and)(
            exists(
              equalTo(
                Filter(not =
                  Some(
                    voronezhParentExpansion.filter.copy(and =
                      voronezhParentExpansion.filter.and.filterNot(_ == Filter(not = Some(noExpansion.filter)))
                    )
                  )
                )
              )
            )
          ) &&
          assert(noExpansion.filter.and)(exists(hasField("", _.op.in.exists(_.field == paramFieldName), isTrue))) &&
          assert(voronezhParentWithoutFilters.filter.and)(
            not(exists(hasField("", _.op.in.exists(_.field == paramFieldName), isTrue)))
          ) &&
          assert(russiaWithoutFilters.canExpand)(isFalse) &&
          assert(fail)(fails(isSubtype[ExpansionError](anything)))
      },
      testM("deduplicate expansions") {
        for {
          _ <- TestGeoService.setGetAllParentRegionsResponse(_ =>
            ZIO.succeed(
              GetRegionsResponse(
                regions = Map(
                  Russia.region.id -> Region(id = Russia.region.id, parentId = RegionIds.Eurasia.id),
                  RegionIds.Eurasia.id -> Region(id = RegionIds.Eurasia.id)
                )
              )
            )
          )
          result <- VasgenFilterMapper.createExpandedVasgenFilters(
            categories,
            params,
            Toponyms(RegionIds.Russia, List(123), List(456)),
            3, // [russia with districts, russia, russia with districts without params, russia without params],
            SearchContext.Unset
          )
          fail <- VasgenFilterMapper
            .createExpandedVasgenFilters(
              categories,
              params,
              Russia,
              2, // [russia, russia without params]
              SearchContext.Unset
            )
            .run
        } yield assert(result)(hasField("canExpand", _.canExpand, isFalse)) && assert(fail)(
          fails(isSubtype[ExpansionError](anything))
        )
      },
      testM("Fail if nothing to expand") {
        for {
          _ <- TestGeoService.setGetAllParentRegionsResponse(_ =>
            ZIO.succeed(
              GetRegionsResponse(
                regions = Map(
                  Russia.region.id -> Region(id = Russia.region.id, parentId = RegionIds.Eurasia.id),
                  RegionIds.Eurasia.id -> Region(id = RegionIds.Eurasia.id)
                )
              )
            )
          )
          result <- VasgenFilterMapper
            .createExpandedVasgenFilters(categories, Nil, Russia, 1, SearchContext.Unset)
            .run
        } yield assert(result)(fails(isSubtype[ExpansionError](anything)))
      },
      testM("Maximum expanded filter") {
        for {
          _ <- TestGeoService.setGetAllParentRegionsResponse(_ =>
            ZIO.succeed(
              GetRegionsResponse(
                regions = Map(
                  RegionIds.Voronezh.id -> Region(id = RegionIds.Voronezh.id, parentId = VoronezhParentId),
                  VoronezhParentId -> Region(id = VoronezhParentId, parentId = RegionIds.Russia.id),
                  RegionIds.Russia.id -> Region(id = RegionIds.Russia.id, parentId = RegionIds.Eurasia.id),
                  RegionIds.Eurasia.id -> Region(id = RegionIds.Eurasia.id, parentId = 0)
                )
              )
            )
          )
          maximumExpandedFilters <- VasgenFilterMapper.createMaximumExpandedVasgenFilters(
            categories,
            params,
            area,
            SearchContext.Unset
          )

        } yield assert(maximumExpandedFilters.and)(
          exists(
            hasField(
              "op.in",
              (_: Filter).op.in,
              isSome(
                hasField("field", (_: vertis.vasgen.query.In).field, equalTo("offer.category.all")) &&
                  hasField(
                    "value",
                    (_: vertis.vasgen.query.In).value,
                    hasSameElements(Seq(RawValue(RawValue.ValueTypeOneof.String(categoryName))))
                  )
              )
            )
          ) &&
            exists(
              hasField(
                "or",
                (_: Filter).or,
                exists(
                  hasField(
                    "op.in",
                    (_: Filter).op.in,
                    isSome(
                      hasField("field", (_: vertis.vasgen.query.In).field, equalTo("offer.region"))
                        &&
                          hasField(
                            "value",
                            (_: vertis.vasgen.query.In).value,
                            hasSameElements(
                              Seq(RawValue(RawValue.ValueTypeOneof.Integer(IntegerValue(Sint64(RegionIds.Russia.id)))))
                            )
                          )
                    )
                  )
                )
              )
            )
        )
      }
    ).provideCustomLayer {
      Logging.live >+>
        ZRef
          .make(BonsaiSnapshot(List.empty, List.empty))
          .toLayer ++ TestGeoService.layer >+> VasgenFilterMapper.live
    } @@ zio.test.TestAspect.sequential
}
