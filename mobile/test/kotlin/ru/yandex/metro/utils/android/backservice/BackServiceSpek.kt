package ru.yandex.metro.utils.android.backservice

import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyZeroInteractions
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldBeFalse
import ru.yandex.metro.ClassSpek

class BackServiceSpek : ClassSpek(BackService::class.java, {
    val backService by memoized { BackServiceImpl() }

    context("perform back when there are no consumers") {
        context("no consumers were added") {
            it("performBack() should return false") {
                backService.performBack().shouldBeFalse()
            }
        }

        context("consumer subscription was disposed") {
            val testConsumer by memoized {
                mock<BackConsumer> {
                    onGeneric { invoke() } doReturn true
                }
            }

            beforeEachTest {
                val subscription = backService.backEventsWithConsumer(testConsumer).subscribe()
                subscription.dispose()
            }

            it("performBack() should return false") {
                backService.performBack().shouldBe(false)
            }

            it("shouldn't invoke consumer") {
                backService.performBack()
                verifyZeroInteractions(testConsumer)
            }

        }
    }

    context("perform back when there is one consumer") {
        val testConsumer by memoized {
            mock<BackConsumer> {
                onGeneric { invoke() } doReturn true
            }
        }

        beforeEachTest {
            backService.backEventsWithConsumer(testConsumer).subscribe()
        }

        context("on perform back") {
            it("performBack should return true") {
                backService.performBack().shouldBe(true)
            }

            it("should invoke consumer once ") {
                backService.performBack()
                verify(testConsumer, times(1)).invoke()
            }
        }
    }

    context("perform back when there are two consumers") {
        val firstConsumer by memoized {
            mock<BackConsumer> {
                onGeneric { invoke() } doReturn true
            }
        }

        val secondConsumer by memoized {
            mock<BackConsumer> {
                onGeneric { invoke() } doReturn true
            }
        }

        beforeEachTest {
            backService.backEventsWithConsumer(firstConsumer).subscribe()
            backService.backEventsWithConsumer(secondConsumer).subscribe()
        }

        context("on perform back") {

            it("shouldn't invoke 1st consumer") {
                backService.performBack()
                verifyZeroInteractions(firstConsumer)
            }

            it("should invoke 2nd consumer once ") {
                backService.performBack()
                verify(secondConsumer, times(1)).invoke()
            }
        }
    }

    context("add 1 working consumer and 1 passing consumer") {
        val workingConsumer by memoized {
            mock<BackConsumer> {
                onGeneric { invoke() } doReturn true
            }
        }

        val passingConsumer by memoized {
            mock<BackConsumer> {
                onGeneric { invoke() } doReturn false
            }
        }

        beforeEachTest {
            backService.backEventsWithConsumer(workingConsumer).subscribe()
            backService.backEventsWithConsumer(passingConsumer).subscribe()
        }
        context("on perform back") {

            it("should invoke working consumer once") {
                backService.performBack()
                verify(workingConsumer, times(1)).invoke()
            }

            it("should invoke passing consumer once ") {
                backService.performBack()
                verify(passingConsumer, times(1)).invoke()
            }
        }
    }
})
