package ru.auto.ara.screenshotTests.onboarding

import ru.auto.data.util.TEST_SHOW_ONBOARDING
import ru.auto.feature.onboarding.data.OnboardingAction
import ru.auto.feature.onboarding.data.OnboardingFinalAction
import ru.auto.feature.onboarding.data.OnboardingVariant
import ru.auto.feature.onboarding.data.repository.IOnboardingShowingRepository
import rx.Completable
import rx.Single

class TestOnboardingShowingRepository(
    private val repositoryDelegate: IOnboardingShowingRepository,
) : IOnboardingShowingRepository {

    override val onboardingVariant: OnboardingVariant
        get() = if (TEST_SHOW_ONBOARDING) {
            repositoryDelegate.onboardingVariant
        } else {
            OnboardingVariant.NOT_SHOW
        }

    override fun getSelectedAction(): Single<OnboardingAction> = throw IllegalAccessError()

    override fun getSelectedFinalAction(): Single<OnboardingFinalAction> = throw IllegalAccessError()

    override fun saveOnboardingShown(): Completable = Completable.complete()

    override fun saveOnboardingActionSelected(action: OnboardingAction): Completable = Completable.complete()

    override fun saveOnboardingFinalActionSelected(action: OnboardingFinalAction): Completable = Completable.complete()

    override fun hasOnboardingShown(): Single<Boolean> = Single.just(!TEST_SHOW_ONBOARDING)
}
