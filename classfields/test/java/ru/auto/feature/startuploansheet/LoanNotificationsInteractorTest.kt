package ru.auto.feature.startuploansheet

import io.kotest.core.spec.style.FreeSpec
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import ru.auto.data.model.AutoruUserProfile
import ru.auto.data.model.User
import ru.auto.data.model.UserProfile
import ru.auto.data.model.VehicleCategory
import ru.auto.data.model.data.offer.ACTIVE
import ru.auto.data.model.data.offer.INACTIVE
import ru.auto.data.model.data.offer.Offer
import ru.auto.data.model.data.offer.SellerType
import ru.auto.data.repository.KeyValueRepository
import ru.auto.data.repository.user.IUserRepository
import ru.auto.feature.loans.common.data.ILoansRepository
import ru.auto.feature.loans.common.presentation.AutoruPayload
import ru.auto.feature.loans.common.presentation.CreditApplication
import ru.auto.feature.loans.common.presentation.CreditApplicationState
import ru.auto.feature.loans.startuploansheet.data.ILoanNotificationsInteractor
import ru.auto.feature.loans.startuploansheet.data.LoanNotificationsInteractor
import rx.Single

class LoanNotificationsInteractorTest : FreeSpec({
    val userRepository: IUserRepository = mock()
    val loansRepository: ILoansRepository = mock()
    val kvRepo: KeyValueRepository<String, Boolean> = mock()

    fun createInteractor(isExpEnabled: Boolean): ILoanNotificationsInteractor = LoanNotificationsInteractor(
        userRepository = userRepository,
        loansRepository = loansRepository,
        kvRepo = kvRepo,
        loanBrokerEnabled = { isExpEnabled },
    )

    "loan broker exp is enabled" - {
        val interactor = createInteractor(isExpEnabled = true)

        "user is authorized" - {
            whenever(userRepository.user).thenReturn(AUTHORIZED_USER)

            "credit application is not marked as seen" - {
                whenever(kvRepo.getAll()).thenReturn(Single.just(emptyMap()))

                "credit application in draft status and without offer" - {
                    val application = createCreditApplication(isDraft = true)
                    whenever(loansRepository.loadActiveCreditApplication(any(), any())).thenReturn(Single.just(application))

                    "it should return 'loan notification draft application' model" {
                        interactor.getLoanNotification()
                            .test()
                            .assertCompleted()
                            .assertValue(ILoanNotificationsInteractor.LoanNotification.DraftApplication(application))
                    }
                }

                "credit application in draft status and with default offer" - {
                    val application = createCreditApplication(isDraft = true, offer = DEFAULT_OFFER)
                    whenever(loansRepository.loadActiveCreditApplication(any(), any())).thenReturn(Single.just(application))

                    "it should return 'loan notification draft application' model" {
                        interactor.getLoanNotification()
                            .test()
                            .assertCompleted()
                            .assertValue(ILoanNotificationsInteractor.LoanNotification.DraftApplication(application))
                    }
                }

                "credit application in draft status with sold offer" - {
                    val application = createCreditApplication(isDraft = true, offer = SOLD_OFFER)
                    whenever(loansRepository.loadActiveCreditApplication(any(), any())).thenReturn(Single.just(application))

                    "it should return 'loan notification offer sold' model" {
                        interactor.getLoanNotification()
                            .test()
                            .assertCompleted()
                            .assertValue(ILoanNotificationsInteractor.LoanNotification.OfferSold(application))
                    }
                }

                "credit application in active status and without offer" - {
                    val application = createCreditApplication(isDraft = false)
                    whenever(loansRepository.loadActiveCreditApplication(any(), any())).thenReturn(Single.just(application))

                    "it should return 'loan notification draft application' model" {
                        interactor.getLoanNotification()
                            .test()
                            .assertCompleted()
                            .assertValue(ILoanNotificationsInteractor.LoanNotification.DraftApplication(application))
                    }
                }

                "credit application in active and with sold offer" - {
                    val application = createCreditApplication(isDraft = false, offer = SOLD_OFFER)
                    whenever(loansRepository.loadActiveCreditApplication(any(), any())).thenReturn(Single.just(application))

                    "it should return 'loan notification offer sold' model" {
                        interactor.getLoanNotification()
                            .test()
                            .assertCompleted()
                            .assertValue(ILoanNotificationsInteractor.LoanNotification.OfferSold(application))
                    }
                }

                "credit application in active and with default offer" - {
                    val application = createCreditApplication(isDraft = false, offer = DEFAULT_OFFER)
                    whenever(loansRepository.loadActiveCreditApplication(any(), any())).thenReturn(Single.just(application))

                    "it should return null" {
                        interactor.getLoanNotification()
                            .test()
                            .assertCompleted()
                            .assertValue(null)
                    }
                }
            }

            "credit application marked seen" - {
                whenever(loansRepository.loadActiveCreditApplication(any(), any()))
                    .thenReturn(Single.just(createCreditApplication()))
                whenever(kvRepo.getAll()).thenReturn(Single.just(CREDIT_PRODUCT_MARK_AS_SEEN))

                "it should return null" {
                    interactor.getLoanNotification()
                        .test()
                        .assertCompleted()
                        .assertValue(null)
                }
            }
        }

        "user is authorized and dealer" - {
            whenever(userRepository.user).thenReturn(AUTHORIZED_DEALER)

            "it should return null" {
                interactor.getLoanNotification()
                    .test()
                    .assertCompleted()
                    .assertValue(null)
            }
        }

        "user is not authorized" - {
            whenever(userRepository.user).thenReturn(User.Unauthorized)

            "it should return null" {
                interactor.getLoanNotification()
                    .test()
                    .assertCompleted()
                    .assertValue(null)
            }
        }
    }

    "loan broker exp is disabled" - {
        val interactor = createInteractor(isExpEnabled = false)

        "it should return null" {
            interactor.getLoanNotification()
                .test()
                .assertCompleted()
                .assertValue(null)
        }
    }



}) {
    companion object {
        private const val CREDIT_ID = "credit_id"
        private val AUTHORIZED_USER = User.Authorized(
            id = "user_id",
            userProfile = UserProfile(),
        )
        private val AUTHORIZED_DEALER = User.Authorized(
            id = "dealer_id",
            userProfile = UserProfile(
                autoruUserProfile = AutoruUserProfile(clientId = "client_id")
            )
        )
        private val CREDIT_PRODUCT_MARK_AS_SEEN = mapOf(CREDIT_ID to true)
        private val DEFAULT_OFFER = createOffer()
        private val SOLD_OFFER = createOffer(isSold = true)

        private fun createCreditApplication(
            isDraft: Boolean = false,
            offer: Offer? = null,
        ) = CreditApplication(
            id = CREDIT_ID,
            state = if (isDraft) CreditApplicationState.DRAFT else CreditApplicationState.ACTIVE,
            payload = AutoruPayload(offer = offer)
        )

        private fun createOffer(isSold: Boolean = false) = Offer(
            category = VehicleCategory.CARS,
            id = "offer_id",
            status = if (isSold) INACTIVE else ACTIVE,
            sellerType = SellerType.PRIVATE,
        )
    }
}
