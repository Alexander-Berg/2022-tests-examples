package ru.yandex.market.test.kakao.views

import android.view.View
import io.github.kakaocup.kakao.common.builders.ViewBuilder
import io.github.kakaocup.kakao.common.views.KBaseView
import io.github.kakaocup.kakao.image.KImageView
import io.github.kakaocup.kakao.recycler.KRecyclerView
import io.github.kakaocup.kakao.text.KTextView
import org.hamcrest.Matcher
import ru.beru.android.R
import ru.yandex.market.mocks.state.UsagePeriod
import ru.yandex.market.screen.reviews.ReviewPhotoRecyclerItem
import ru.yandex.market.test.kakao.matchers.ReviewFullTextMatcher
import ru.yandex.market.test.util.findRecyclerAndScrollTo

class KReviewView(parent: Matcher<View>, function: ViewBuilder.() -> Unit) : KBaseView<KReviewView>(parent, function) {
    private val name = KTextView(parent) { withId(R.id.nameView) }
    private val photo = KImageView(parent) { withId(R.id.avatarView) }
    private val usagePeriod = KTextView(parent) { withId(R.id.usagePeriodView) }
    private val ratingBriefView = KRatingBriefView(parent) { withId(R.id.ratingView) }
    private val commentary = KTextView(parent) { withId(R.id.textView) }
    private val reply = KTextView(parent) { withId(R.id.replyView) }
    private val answerDate = KTextView(parent) { withId(R.id.dateView) }
    private val showAnswers = KTextView(parent) { withId(R.id.showCommentsView) }
    private val photoList = KRecyclerView(
        parent,
        builder = { withId(R.id.photosRecyclerView) },
        itemTypeBuilder = { itemType(::ReviewPhotoRecyclerItem) }
    )
    private val menuImageButton = KImageView(parent) { withId(R.id.imageReviewItemMenu) }

    private val likeIcon = KImageView(parent) { withId(R.id.imageReviewLikeIcon) }

    private val likeCount = KTextView(parent) { withId(R.id.textReviewLikeCount) }

    private val dislikeIcon = KImageView(parent) { withId(R.id.imageReviewDislikeIcon) }

    private val dislikeCount = KTextView(parent) { withId(R.id.textReviewDislikeCount) }

    fun checkCommentName(value: String) {
        name {
            isVisible()
            hasText(value)
        }
    }

    fun checkCommentAvatar() {
        photo.isVisible()
    }

    fun clickPhoto(position: Int) {
        photoList.childAt<ReviewPhotoRecyclerItem>(position) {
            click()
        }
    }

    fun checkPhoto(position: Int) {
        photoList.childAt<ReviewPhotoRecyclerItem>(position) {
            isVisible()
        }
    }

    fun checkCommentUsagePeriod(value: UsagePeriod) {
        usagePeriod.isVisible()
        usagePeriod.hasText("Опыт использования:\n${value.displayValue.capitalize()}")
    }

    fun checkCommentRating(grade: Float) {
        with(ratingBriefView) {
            isVisible()
            checkHighlightedStarsCount(grade)
            checkText(grade)
        }
    }

    fun checkCommentText(pros: String = "", cons: String = "", reviewText: String = "") {
        commentary.matches { withMatcher(ReviewFullTextMatcher(pros, cons, reviewText)) }
    }

    fun checkAnswerText(text: String) {
        commentary {
            isVisible()
            hasText(text)
        }
    }

    fun checkCommentReplyButton() {
        reply {
            isVisible()
            hasText("Ответить")
        }
    }

    fun openFullCommentary() {
        commentary.click()
    }

    fun checkShowAnswersText(text: String) {
        showAnswers.containsText(text)
    }

    fun openAnswers() {
        showAnswers {
            findRecyclerAndScrollTo()
            click()
        }
    }

    fun checkDate(value: String) {
        answerDate.isVisible()
        answerDate.hasText(value)
    }

    fun openMenu() {
        menuImageButton.click()
    }

    fun clickLike() {
        likeIcon {
            findRecyclerAndScrollTo()
            click()
        }
    }

    fun checkLikeCount(count: Int) {
        likeCount.hasText(count.toString())
    }

    fun checkLikeColor(isActive: Boolean) {
        if (isActive) {
            likeIcon.hasDrawable(R.drawable.ic_like_clicked)
        } else {
            likeIcon.hasDrawable(R.drawable.ic_like)
        }
    }

    fun clickDislike() {
        dislikeIcon.findRecyclerAndScrollTo()
        dislikeIcon.click()
    }

    fun checkDislikeCount(count: Int) {
        dislikeCount.hasText(count.toString())
    }

    fun checkDislikeColor(isActive: Boolean) {
        if (isActive) {
            dislikeIcon.hasDrawable(R.drawable.ic_dislike_clicked)
        } else {
            dislikeIcon.hasDrawable(R.drawable.ic_dislike)
        }
    }

    fun clickReply() {
        reply.findRecyclerAndScrollTo()
        reply.click()
    }
}