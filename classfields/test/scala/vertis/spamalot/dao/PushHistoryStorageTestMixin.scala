package vertis.spamalot.dao

import com.yandex.ydb.table.query.Params
import common.zio.logging.Logging
import ru.yandex.vertis.ydb.zio.TxRIO
import vertis.spamalot.dao.queries.push_history.CleanQuery

trait PushHistoryStorageTestMixin { self: PushHistoryStorage =>

  def clean: TxRIO[Logging.Logging, Unit] =
    prepareAndExecute(
      CleanQuery,
      Params.empty()
    ).unit

}
