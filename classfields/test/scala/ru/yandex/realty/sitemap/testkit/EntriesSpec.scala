package ru.yandex.realty.sitemap.testkit

import ru.yandex.realty.sitemap.service.entry.Entries
import ru.yandex.realty.sitemap.service.entry.Entries.Entries
import zio._
import zio.test._

object EntriesSpec {

  def specEffect[A: Tag](
    expected: Seq[A],
    loggedPrefix: Option[String] = None,
    preSort: Option[Ordering[A]] = None
  ): RIO[Entries[A], TestResult] =
    Entries
      .all[A]
      .runCollect
      .map { res =>
        assert(res)(
          CustomAssertions.seqEquals(expected, loggedPrefix = loggedPrefix, preSort)
        )
      }
}
