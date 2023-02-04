package vertis.spamalot.dao

import vertis.spamalot.SpamalotYdbTest
import vertis.spamalot.dao.queries.channels.{UnreadCountQuery, UpsertChannelQuery}
import vertis.spamalot.dao.queries.notifications.ListQuery.{ListAllFilteredQuery, ListAllQuery}
import vertis.spamalot.dao.queries.notifications._
import vertis.zio.test.ZioSpecBase

/** To manually check ydb execution plans
  * Could be run as a test to check all queries compile
  * Could be an alert for the word 'FullScan' appearing
  *
  * @author Ratskevich Natalia reimai@yandex-team.ru
  */
class StorageExplanationIntSpec extends ZioSpecBase with SpamalotYdbTest {

  "StorageExplanation" should {

    "explain notifications" in explainAll(
      AddQuery.AddBatchQuery,
      AddQuery.InsertOneQuery,
      AddQuery.UpsertOneQuery,
      AddQuery.InsertCampaignQuery,
      AddQuery.UpsertCampaignQuery,
      CancelQuery.CancelOneQuery,
      CancelQuery.CancelManyQuery,
      FilterQuery,
      GetQuery,
      ListAllQuery,
      ListAllFilteredQuery(topics = Set("topic"), ids = Seq("id"), newOnly = true),
      MarkReadQuery.MarkAllReadQuery,
      MarkReadQuery.MarkReadByIdsQuery,
      MarkReadQuery.MarkFilteredReadQuery
    )

    "explain channels" in explainAll(
      UnreadCountQuery,
      UpsertChannelQuery
    )
  }
}
