package vertis.palma.dao.ydb.storage

import vertis.palma.dao.DictionariesDao.Page
import vertis.palma.dao.model.Pagination
import zio.{RIO, ZIO}

/** @author kusaeva
  */
object PaginationTestHelper {

  def readAll[E, T](result: Seq[T], pagination: Pagination, readPage: Pagination => RIO[E, Page[T]]): RIO[E, Seq[T]] =
    for {
      page <- readPage(pagination)
      r <-
        if (page.nonEmpty) {
          readAll(result ++ page.items, Pagination(page.lastKey, pagination.limit), readPage)
        } else ZIO.succeed(result)
    } yield r
}
