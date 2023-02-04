package ru.auto.ara.presentation.presenter.vas

import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.DescribeSpec
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import ru.auto.ara.billing.promo.ServicePriceToVasInfoConverter
import ru.auto.ara.billing.vas.VasEventSource
import ru.auto.ara.presentation.presenter.PresentationModelMocks
import ru.auto.ara.presentation.view.ViewModelView
import ru.auto.ara.util.statistics.event.vas.CommonVasEventLogger
import ru.auto.ara.viewmodel.vas.VASCatchContext
import ru.auto.ara.viewmodel.vas.VASCatchModel
import ru.auto.data.factory.vas.VASCatchFactoryProducer
import ru.auto.data.model.data.offer.Offer
import ru.auto.data.model.network.scala.offer.converter.OfferConverter
import ru.auto.data.model.vas.EventWithOffer
import ru.auto.data.model.vas.VASComparableItem
import ru.auto.data.network.scala.response.OfferListingResponse
import ru.auto.data.repository.IOffersRepository
import ru.auto.testextension.FileTestUtils

/**
 * @author dumchev on 05/12/2018.
 */
internal class VASCatchPresentationModelSpec : DescribeSpec({
    isolationMode = IsolationMode.SingleInstance // this means state is preserved between tests
    describe("VASCatchPresentationModel dependencies") {
        val mocks = PresentationModelMocks
        val vasContext: VASCatchContext by lazy {
            val nwOffersResponse = FileTestUtils.readJsonAsset(
                assetPath = "/assets/offers.json",
                classOfT = OfferListingResponse::class.java
            )

            val offers = nwOffersResponse.offers?.map { OfferConverter().fromNetwork(it) } as List<Offer>
            val firstOffer = offers[0]

            val items = VASCatchFactoryProducer(EventWithOffer.ADD, firstOffer).getFactory().build()

            VASCatchContext(
                true,
                firstOffer,
                items,
                EventWithOffer.ADD,
                VasEventSource.LK,
                null
            )
        }
        val testView: ViewModelView<VASCatchModel> = mock()
        val router = mocks.router
        val clearComponentMock: () -> Unit = mock()
        val offersRepository: IOffersRepository = mock()
        val vasEventLogger: CommonVasEventLogger = mock()

        val presentationModel = VASCatchPresentationModel(
            router = router,
            errorFactory = mocks.errorFactory,
            model = VASCatchModel(1, mock(), mock(), vasContext.items),
            context = vasContext,
            vasConverter = ServicePriceToVasInfoConverter(),
            clearComponent = clearComponentMock,
            offersRepository = offersRepository,
            vasEventLogger = vasEventLogger
        )

        context("on bind") {
            presentationModel.bind(testView)
            val captor = argumentCaptor<VASCatchModel>()
            val initialModelUpdatedCount = 1 // from first init

            it("check model updated just after bind") {
                verify(testView, times(initialModelUpdatedCount)).update(captor.capture())
            }

            context("check clicks") {

                fun singleExpandedCount(): Int = captor.lastValue.items
                    .asSequence()
                    .filter { it is VASComparableItem.Single && it.expanded }
                    .count()

                var alreadyExpanded = singleExpandedCount()
                var clicks = 0

                fun clickOn(condition: (VASComparableItem) -> Boolean) {
                    vasContext.items.firstOrNull { condition(it) }?.let { item ->
                        presentationModel.onItemClicked((item as VASComparableItem.Single).servicePrice)
                        clicks++
                    }
                }

                it("click on first collapsed VASComparableItem.Single should expand it") {
                    clickOn { it is VASComparableItem.Single && it.expanded.not() }
                    alreadyExpanded++
                    verify(testView, times(clicks + initialModelUpdatedCount)).update(captor.capture())
                    check(singleExpandedCount() == alreadyExpanded) {
                        "expected expanded to be $alreadyExpanded, but it was ${singleExpandedCount()}"
                    }
                }
            }
        }
        context("destroyed") {
            it("remember to clear component on destroy") {
                presentationModel.onDestroyed()
                verify(clearComponentMock, times(1)).invoke()
            }
        }
    }
})
