package ru.auto.cabinet.service

import org.scalatest.concurrent.{PatienceConfiguration, ScalaFutures}
import org.scalatest.matchers.should.Matchers
import org.scalatest.flatspec.{AnyFlatSpecLike => FlatSpecLike}
import org.scalatestplus.mockito.MockitoSugar.mock
import org.mockito.{ArgumentMatchers => MM}
import org.mockito.Mockito.{times, verify, when}
import ru.auto.cabinet.model.{GeoBaseRecord, RegionId}
import ru.auto.cabinet.trace.Context

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.global

class GeoDataSourceSpec extends FlatSpecLike with Matchers with ScalaFutures {

  implicit private val rc = Context.unknown
  private val SourceIds = Set(1L)
  private val Level1 = Set(2L, 3L, 4L)
  private val Level2 = Set(5L, 6L, 7L, 8L)
  private val Level3 = Set(9L, 10L, 11L, 12L, 13L)

  private val timeout = PatienceConfiguration.Timeout(2.seconds)

  private val ResolverGen = () => {
    val m = mock[Set[RegionId] => Future[Set[RegionId]]]
    when(m.apply(MM.eq(SourceIds))).thenReturn(Future.successful(Level1))
    when(m.apply(MM.eq(Level1))).thenReturn(Future.successful(Level2))
    when(m.apply(MM.eq(Level2))).thenReturn(Future.successful(Level3))
    when(m.apply(MM.eq(Level3))).thenReturn(Future.successful(Set[RegionId]()))
    m
  }

  behavior.of("GeoDataSource")

  it should "return complete ids set" in {
    val resolver = ResolverGen()
    val subj = TestGeoDataSource(resolver)
    val expected = SourceIds ++ Level1 ++ Level2 ++ Level3
    whenReady(subj.findChildRegions(1), timeout) { result =>
      result should contain theSameElementsAs expected
      verify(resolver, times(4)).apply(MM.any())
    }
  }

  it should "return complete ids set using cache" in {
    val resolver = ResolverGen()
    val subj = TestGeoDataSource(resolver)
    val expected = SourceIds ++ Level1 ++ Level2 ++ Level3
    whenReady(subj.findChildRegions(1), timeout) { result =>
      result should contain theSameElementsAs expected
      whenReady(subj.findChildRegions(1), timeout) { next =>
        next should contain theSameElementsAs expected
        verify(resolver, times(4)).apply(MM.any())
      }
    }
  }

  it should "return partial ids set with strict search depth" in {
    val resolver = ResolverGen()
    val subj = TestGeoDataSource(resolver, 2)
    val expected = SourceIds ++ Level1 ++ Level2
    whenReady(subj.findChildRegions(1), timeout) { result =>
      result should contain theSameElementsAs expected
      verify(resolver, times(2)).apply(MM.any())
    }
  }

  private case class TestGeoDataSource(
      resolver: Set[RegionId] => Future[Set[RegionId]],
      searchDepth: Int = 10)
      extends GeoDataSource {
    implicit override protected val ec: ExecutionContext = global

    protected def getChildIds(ids: Set[RegionId])(implicit
        rc: Context): Future[Set[RegionId]] =
      resolver(ids)

    override protected def getRegionEntity(regionId: RegionId)(implicit
        rc: Context): Future[Option[GeoBaseRecord]] = ???

    override protected def getRegionEntities(regionIds: Set[RegionId])(implicit
        rc: Context): Future[Map[RegionId, GeoBaseRecord]] = ???
  }

}
