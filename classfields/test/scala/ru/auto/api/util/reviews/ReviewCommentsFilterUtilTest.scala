package ru.auto.api.util.reviews

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers._
import ru.auto.api.services.phpapi.model.PhpComment
import ru.auto.api.services.review.util.ReviewCommentsFilterUtil

class ReviewCommentsFilterUtilTest extends AnyFunSuite {

  test("filter comments") {
    val comments = List(
      PhpComment.withEmptyUserString("1", "1", "text", "1", "0"),
      PhpComment.withEmptyUserString("2", "1", "text", "1", "1"),
      PhpComment.withEmptyUserString("3", "1", "text", "1", "1"),
      PhpComment.withEmptyUserString("4", "2", "text", "1", "1"),
      PhpComment.withEmptyUserString("5", "2", "text", "1", "0"),
      PhpComment.withEmptyUserString("6", "3", "text", "1", "1"),
      PhpComment.withEmptyUserString("7", "1", "text", "1", "0")
    )

    val filteredComments = List(
      PhpComment.withEmptyUserString("1", "1", "text", "1", "0"),
      PhpComment.withEmptyUserString("3", "1", "text", "1", "1"),
      PhpComment.withEmptyUserString("5", "2", "text", "1", "0"),
      PhpComment.withEmptyUserString("7", "1", "text", "1", "0")
    )

    ReviewCommentsFilterUtil.filterComments(comments) shouldBe filteredComments
  }
}
