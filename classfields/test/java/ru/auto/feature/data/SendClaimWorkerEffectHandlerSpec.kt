package ru.auto.feature.data

import io.kotest.assertions.forEachAsClue
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import ru.auto.experiments.Experiments
import ru.auto.experiments.ExperimentsManager
import ru.auto.experiments.sharkSendOnlyTinkoff
import ru.auto.feature.loans.cabinet.LoanCabinet
import ru.auto.feature.loans.cabinet.data.IClaimQueueRepository
import ru.auto.feature.loans.cabinet.data.SendClaimWorkerEffectHandler
import ru.auto.feature.loans.cabinet.data.SendClaimsInteractor
import ru.auto.feature.loans.common.data.ICreditApplicationRepository
import ru.auto.feature.loans.common.data.ILoansRepository
import ru.auto.feature.loans.common.presentation.CreditApplication
import ru.auto.feature.loans.common.presentation.CreditApplicationState
import ru.auto.feature.loans.common.presentation.UserSettings
import ru.auto.testdata.CREDIT_PRODUCT_GENERIC
import rx.Completable
import rx.Single

class SendClaimWorkerEffectHandlerSpec : FreeSpec() {
    init {
        val activeApplication = CreditApplication(
            id = "application",
            state = CreditApplicationState.ACTIVE
        )
        val creditApplicationRepository: ICreditApplicationRepository = mock()
        val claimQueue: IClaimQueueRepository = mock()
        val loansRepository: ILoansRepository = mock()
        val listener: (LoanCabinet.Msg) -> Unit = mock()
        val interactor = SendClaimsInteractor(claimQueue, creditApplicationRepository, loansRepository)
        val handlerUnderTest = SendClaimWorkerEffectHandler(interactor, claimQueue, loansRepository)
        val applicationId = "TEST_ID"
        val products = listOf(
            CREDIT_PRODUCT_GENERIC,
            CREDIT_PRODUCT_GENERIC.copy(
                id = "third id"
            ),
            CREDIT_PRODUCT_GENERIC.copy(
                id = "fourth id"
            ),
            CREDIT_PRODUCT_GENERIC.copy(
                id = "fifth id"
            )
        )
        val tinkoffProducts = listOf(
            CREDIT_PRODUCT_GENERIC.copy(
                id = "other_id",
                bank = CREDIT_PRODUCT_GENERIC.bank.copy(id = "tinkoff")
            ),
            CREDIT_PRODUCT_GENERIC.copy(
                id = "another_id",
                bank = CREDIT_PRODUCT_GENERIC.bank.copy(id = "tinkoff")
            ),
        )
        "describe sending claims after application publish" - {
            "context everything goes fine" - {
                val applicationIdCaptor = argumentCaptor<String>()
                val creditProductIdCaptor = argumentCaptor<List<String>>()
                whenever(claimQueue.queueSendClaim(applicationIdCaptor.capture(), creditProductIdCaptor.capture()))
                    .thenReturn(Completable.complete())
                val applicationCaptor = argumentCaptor<CreditApplication>()
                whenever(creditApplicationRepository.updateApplication(applicationCaptor.capture())).thenAnswer {
                    Single.just(applicationCaptor.lastValue)
                }
                whenever(loansRepository.loadActiveCreditApplication(any(), any())).thenAnswer {
                    Single.just(applicationCaptor.allValues.firstOrNull() ?: activeApplication)
                }
                "context no experiments enabled" - {
                    whenever(loansRepository.loadCreditProductsByCreditApplication(applicationIdCaptor.capture())).thenReturn(
                        Single.just(products + tinkoffProducts)
                    )
                    handlerUnderTest.invoke(LoanCabinet.Eff.SendClaimsForActiveApplication(applicationId, emptyList()), listener)
                    "it should send claims for first three products" {
                        creditProductIdCaptor.allValues.forEachAsClue {
                            it shouldContainExactly listOf(
                                products[0].id,
                                products[1].id,
                                products[2].id
                            )
                        }
                    }
                    "it should not change applicationId" {
                        applicationIdCaptor.allValues.forEachAsClue {
                            it shouldBe applicationId
                        }
                    }
                    "it should update application with tag" {
                        applicationCaptor.lastValue.userSettings shouldBe UserSettings(listOf("control_and_0921"))
                    }
                }
                "context experiment with tinkoff enabled" - {
                    val experimentsManager: Experiments = mock()
                    whenever(experimentsManager.sharkSendOnlyTinkoff()).thenReturn(true)
                    ExperimentsManager.setInstance(experimentsManager)
                    "context there are tinkoff products available" - {
                        whenever(loansRepository.loadCreditProductsByCreditApplication(applicationIdCaptor.capture())).thenReturn(
                            Single.just(products + tinkoffProducts)
                        )
                        handlerUnderTest.invoke(
                            LoanCabinet.Eff.SendClaimsForActiveApplication(applicationId, emptyList())
                            , listener
                        )
                        "it should send only claims with tinkoff bank" {
                            creditProductIdCaptor.allValues.forEachAsClue {
                                it shouldContainExactly tinkoffProducts.map { product -> product.id }
                            }
                        }
                        "it should update application with exp tag" {
                            applicationCaptor.lastValue.userSettings shouldBe UserSettings(listOf("exp_and_0921"))
                        }
                    }
                    "context no tinkoff products available" - {
                        whenever(loansRepository.loadCreditProductsByCreditApplication(applicationIdCaptor.capture())).thenReturn(
                            Single.just(products)
                        )
                        handlerUnderTest.invoke(
                            LoanCabinet.Eff.SendClaimsForActiveApplication(applicationId, emptyList()),
                            listener
                        )
                        "it should send first three products as usual" {
                            creditProductIdCaptor.allValues.forEachAsClue {
                                it shouldContainExactly listOf(
                                    products[0].id,
                                    products[1].id,
                                    products[2].id
                                )
                            }
                        }
                        "it should update application with control tag" {
                            applicationCaptor.lastValue.userSettings shouldBe UserSettings(listOf("control_and_0921"))
                        }
                    }
                }
            }
        }
    }
}
