package ru.auto.feature.cartinder

import io.qameta.allure.kotlin.Allure
import io.qameta.allure.kotlin.junit4.AllureRunner
import nl.qbusict.cupboard.DatabaseCompartment
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import ru.auto.feature.cartinder_uploader.data.CartinderReaction
import ru.auto.feature.cartinder_uploader.data.CartinderReactionConverter
import ru.auto.feature.cartinder_uploader.data.CupboardCartinderStorage
import ru.auto.feature.cartinder_uploader.data.DBCartinderReaction
import rx.observers.TestSubscriber

private const val TEST_SELF_OFFER_ID = "TEST_SELF_OFFER_ID"
private const val TEST_TARGET_OFFER_ID = "TEST_TARGET_OFFER_ID"

@RunWith(AllureRunner::class)
class CartinderUploaderStorageTest {
    private val mockDb: DatabaseCompartment = mock()

    private val instance by lazy {
        CupboardCartinderStorage(
            db = mockDb,
        )
    }

    @Test
    fun `should save like reaction`() {
        val likeReaction = CartinderReaction.Like(
            selfOfferId = TEST_SELF_OFFER_ID,
            targetOfferId = TEST_TARGET_OFFER_ID,
        )

        val dbLikeReaction = CartinderReactionConverter.toDb(likeReaction)

        val subscriber = TestSubscriber<Unit>()

        Allure.step("save like") {
            instance.saveReaction(likeReaction).subscribe(subscriber)
        }

        Allure.step("assert that write record was correct") {
            verify(mockDb, times(1)).put(dbLikeReaction)
        }
    }

    @Test
    fun `should save dislike reaction`() {
        val dislikeReaction = CartinderReaction.Dislike(
            selfOfferId = TEST_SELF_OFFER_ID,
            targetOfferId = TEST_TARGET_OFFER_ID,
        )

        val dbDislikeReaction = CartinderReactionConverter.toDb(dislikeReaction)

        val subscriber = TestSubscriber<Unit>()

        Allure.step("save like") {
            instance.saveReaction(dislikeReaction).subscribe(subscriber)
        }

        Allure.step("assert that write record was correct") {
            verify(mockDb, times(1)).put(dbDislikeReaction)
        }
    }

    @Test
    fun `should clear table`() {
        val subscriber = TestSubscriber<Unit>()

        Allure.step("clear db") {
            instance.clear().subscribe(subscriber)
        }

        Allure.step("assert was cleared in correct order") {
            verify(mockDb, times(1))
                .delete(DBCartinderReaction::class.java, null)
        }
    }

    @Test
    fun `should get values from table`() {
        val reactions = setOf(
            CartinderReaction.Dislike(
                selfOfferId = TEST_SELF_OFFER_ID,
                targetOfferId = "${TEST_TARGET_OFFER_ID}_1",
            ),
            CartinderReaction.Like(
                selfOfferId = TEST_SELF_OFFER_ID,
                targetOfferId = "${TEST_TARGET_OFFER_ID}_2",
            ),
            CartinderReaction.Dislike(
                selfOfferId = TEST_SELF_OFFER_ID,
                targetOfferId = "${TEST_TARGET_OFFER_ID}_3",
            )
        )

        val queryBuilderStub: DatabaseCompartment.QueryBuilder<DBCartinderReaction> = mock()
        whenever(mockDb.query(DBCartinderReaction::class.java)).thenReturn(queryBuilderStub)
        whenever(queryBuilderStub.list())
            .thenReturn(reactions.map(CartinderReactionConverter::toDb))

        val subscriber = TestSubscriber<Set<CartinderReaction>>()

        Allure.step("drain db") {
            instance.getReactions().subscribe(subscriber)
        }

        Allure.step("assert was expected items") {
            subscriber.assertValue(reactions)
        }

        Allure.step("assert call order") {
            verify(mockDb, times(1))
                .query(DBCartinderReaction::class.java)
        }
    }
}
