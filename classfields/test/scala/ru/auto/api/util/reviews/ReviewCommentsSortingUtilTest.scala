package ru.auto.api.util.reviews

import org.scalatest.funsuite.AnyFunSuite
import ru.auto.api.services.phpapi.model.PhpComment
import ru.auto.api.services.review.util.ReviewCommentsSortingUtil
import org.scalatest.matchers.should.Matchers._

/**
  * Created by Karpenko Maksim (knkmx@yandex-team.ru) on 19/03/2018.
  */
class ReviewCommentsSortingUtilTest extends AnyFunSuite {

  test("revert comments") {
    val comments = List(
      PhpComment.withEmptyUserString("1", "1", "text", "1", "0"),
      PhpComment.withEmptyUserString("2", "2", "text", "1", "0"),
      PhpComment.withEmptyUserString("3", "2", "text", "1", "0"),
      PhpComment.withEmptyUserString("4", "3", "text", "1", "0"),
      PhpComment.withEmptyUserString("5", "3", "text", "1", "0"),
      PhpComment.withEmptyUserString("6", "4", "text", "1", "0"),
      PhpComment.withEmptyUserString("7", "3", "text", "1", "0"),
      PhpComment.withEmptyUserString("8", "2", "text", "1", "0")
    )

    val revertedComments = List(
      PhpComment.withEmptyUserString("1", "1", "text", "1", "0"),
      PhpComment.withEmptyUserString("8", "2", "text", "1", "0"),
      PhpComment.withEmptyUserString("3", "2", "text", "1", "0"),
      PhpComment.withEmptyUserString("7", "3", "text", "1", "0"),
      PhpComment.withEmptyUserString("5", "3", "text", "1", "0"),
      PhpComment.withEmptyUserString("6", "4", "text", "1", "0"),
      PhpComment.withEmptyUserString("4", "3", "text", "1", "0"),
      PhpComment.withEmptyUserString("2", "2", "text", "1", "0")
    )

    ReviewCommentsSortingUtil.revertComments(comments) shouldBe revertedComments
  }
}
