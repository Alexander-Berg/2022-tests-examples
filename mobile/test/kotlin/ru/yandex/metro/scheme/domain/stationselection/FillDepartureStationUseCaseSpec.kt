package ru.yandex.metro.scheme.domain.stationselection

import com.gojuno.koptional.Optional
import com.gojuno.koptional.toOptional
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doAnswer
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import io.reactivex.Completable
import io.reactivex.Maybe
import io.reactivex.Observable
import ru.yandex.metro.ClassSpek
import ru.yandex.metro.common.domain.model.GeoPointByData
import ru.yandex.metro.common.domain.model.GeoRectByData
import ru.yandex.metro.common.domain.model.LocalizedString
import ru.yandex.metro.metrics.MetricsLogging
import ru.yandex.metro.scheme.domain.PreferredSchemeComponentProvider
import ru.yandex.metro.scheme.domain.SchemeInfoService
import ru.yandex.metro.scheme.domain.SchemeStateRepository
import ru.yandex.metro.scheme.domain.accent.AccentUseCase
import ru.yandex.metro.scheme.domain.di.SchemeComponent
import ru.yandex.metro.scheme.domain.model.NearestStation
import ru.yandex.metro.scheme.domain.model.ServiceStationByData
import ru.yandex.metro.scheme.domain.model.ServiceStationId
import ru.yandex.metro.scheme.domain.model.Station
import ru.yandex.metro.scheme.domain.model.StationByData
import ru.yandex.metro.scheme.domain.model.StationDetails
import ru.yandex.metro.scheme.domain.model.StationDetailsByData
import ru.yandex.metro.scheme.domain.model.StationDisplayItem
import ru.yandex.metro.scheme.domain.model.StationFlag
import ru.yandex.metro.scheme.domain.model.StationId
import ru.yandex.metro.scheme.domain.model.id
import ru.yandex.metro.scheme.domain.stationselection.Data.CLOSED_STATION_DETAILS
import ru.yandex.metro.scheme.domain.stationselection.Data.DEFAULT_SERVICE_STATION
import ru.yandex.metro.scheme.domain.stationselection.Data.DEFAULT_SERVICE_STATION_ID
import ru.yandex.metro.scheme.domain.stationselection.Data.DEFAULT_STATION
import ru.yandex.metro.scheme.domain.stationselection.Data.DEFAULT_STATION_DETAILS
import ru.yandex.metro.scheme.domain.stationselection.Data.DEFAULT_STATION_ID
import ru.yandex.metro.scheme.domain.stationselection.model.Accent
import ru.yandex.metro.scheme.domain.stationselection.model.FocusType
import ru.yandex.metro.scheme.domain.stationselection.model.StationsSelection
import ru.yandex.metro.scheme.domain.user.location.UserLocationProvider
import ru.yandex.metro.station.ui.StationCardFragment
import ru.yandex.metro.utils.android.navigation.impl.fragment.FragmentBackStack
import ru.yandex.metro.utils.android.navigation.impl.fragment.FragmentTransaction
import ru.yandex.metro.utils.android.navigation.impl.fragment.FragmentTransactionHistory
import ru.yandex.metro.utils.android.uniqueTag

class FillDepartureStationUseCaseSpec : ClassSpek(FillDepartureStationUseCase::class.java, {
    val metricsLogging by memoized {
        mock<MetricsLogging> {
            on { routePointFromNearby(DEFAULT_STATION) } doAnswer { mock() }
        }
    }

    val defaultNearestStation = mock<NearestStation> {
        val defaultServiceStation = mock<StationDisplayItem> {
            on { serviceStation } doReturn DEFAULT_SERVICE_STATION
        }
        on { stationDisplayItem } doReturn defaultServiceStation
    }

    context("station is open") {
        val schemeComponentProvider by memoized {
            mock<PreferredSchemeComponentProvider> {
                val schemeComponent = mock<SchemeComponent> {
                    val schemeInfoService = mock<SchemeInfoService> {
                        on { stationDetails(DEFAULT_STATION_ID) } doReturn Maybe.just(DEFAULT_STATION_DETAILS)
                    }
                    on { schemeInfoService() } doReturn schemeInfoService
                }
                on { preferredSchemeComponent() } doReturn Observable.just(schemeComponent)
            }
        }

        val fragmentBackStack by memoized {
            mock<FragmentBackStack> {
                on { changeEvents() } doReturn Observable.create { publisher ->
                    publisher.onNext(FragmentTransactionHistory(emptyList(), 1))
                }
            }
        }

        val schemeRepository by memoized { SchemeStateRepository() }

        val accentUseCase by memoized {
            mock<AccentUseCase> {
                on { applyAccent(any()) } doReturn Completable.complete()
            }
        }

        context("station selection is empty") {
            val selectionUseCase by memoized {
                mock<StationSelectionUseCase> {
                    on { stationsSelection() } doReturn
                            Observable.create<StationsSelection> { emitter -> emitter.onNext(StationsSelection.empty()) }

                    on { select(eq(DEFAULT_SERVICE_STATION_ID)) } doReturn Completable.complete()
                }
            }
            context("nearest station was retrieved") {
                val userLocationProvider by memoized {
                    mock<UserLocationProvider> {
                        on { nearestStation() } doReturn Observable.just(defaultNearestStation.toOptional())
                    }
                }

                context("usecase has been activated") {
                    val useCase by memoized {
                        FillDepartureStationUseCase(
                                schemeComponentProvider,
                                schemeRepository,
                                selectionUseCase,
                                userLocationProvider,
                                fragmentBackStack,
                                accentUseCase,
                                metricsLogging
                        )
                    }

                    it("should fill departure") {
                        val fillDeparture = useCase.fillDeparture().test()
                        verify(selectionUseCase, times(1)).select(DEFAULT_SERVICE_STATION_ID)
                        fillDeparture.assertComplete()
                        fillDeparture.assertNoErrors()
                    }

                    it("should update accent") {
                        useCase.fillDeparture().test().assertComplete()
                        verify(accentUseCase, times(1))
                                .applyAccent(eq(Accent.Station(DEFAULT_SERVICE_STATION_ID, FocusType.MINIMAL)))
                    }
                }
            }

            context("nearest station is unknown") {
                val userLocationProvider by memoized {
                    mock<UserLocationProvider> {
                        on { nearestStation() } doReturn Observable.never<Optional<NearestStation>>()
                    }
                }

                context("usecase has been activated") {
                    val useCase = FillDepartureStationUseCase(
                            schemeComponentProvider,
                            schemeRepository,
                            selectionUseCase,
                            userLocationProvider,
                            fragmentBackStack,
                            accentUseCase,
                            metricsLogging
                    )

                    it("shouldn't fill departure") {
                        val fillDeparture = useCase.fillDeparture().test()
                        verify(selectionUseCase, never()).select(any<ServiceStationId>())
                        fillDeparture.assertNotComplete()
                        fillDeparture.assertNoErrors()
                    }

                    it("should NOT update accent") {
                        verify(accentUseCase, never())
                                .applyAccent(eq(Accent.Station(DEFAULT_SERVICE_STATION_ID, FocusType.MINIMAL)))
                    }
                }
            }
        }

        context("station selection is NOT empty") {
            val selectionUseCase by memoized {
                mock<StationSelectionUseCase> {
                    on { stationsSelection() } doReturn
                            Observable.just(StationsSelection(ServiceStationId(StationId("2"), null)))
                }
            }
            context("nearest station was retrieved") {
                val userLocationProvider by memoized {
                    mock<UserLocationProvider> {
                        on { nearestStation() } doReturn Observable.just(defaultNearestStation.toOptional())
                    }
                }
                context("usecase has been activated") {
                    val useCase = FillDepartureStationUseCase(
                            schemeComponentProvider,
                            schemeRepository,
                            selectionUseCase,
                            userLocationProvider,
                            fragmentBackStack,
                            accentUseCase,
                            metricsLogging
                    )
                    val fillDeparture = useCase.fillDeparture().test()

                    it("shouldn't fill departure") {
                        verify(selectionUseCase, never()).select(DEFAULT_SERVICE_STATION_ID)

                        fillDeparture.assertComplete()
                        fillDeparture.assertNoErrors()
                    }

                    it("should NOT update accent") {
                        verify(accentUseCase, never())
                                .applyAccent(eq(Accent.Station(DEFAULT_SERVICE_STATION_ID, FocusType.MINIMAL)))
                    }
                }
            }
        }
    }

    context("station selection is empty") {

        val selectionUseCase by memoized {
            mock<StationSelectionUseCase> {
                on { stationsSelection() } doReturn
                        Observable.create<StationsSelection> { emitter -> emitter.onNext(StationsSelection.empty()) }

                on { select(eq(DEFAULT_SERVICE_STATION_ID)) } doReturn Completable.complete()
            }
        }

        val schemeComponentProvider by memoized {
            mock<PreferredSchemeComponentProvider> {
                val schemeComponent = mock<SchemeComponent> {
                    val schemeInfoService = mock<SchemeInfoService> {
                        on { stationDetails(DEFAULT_STATION_ID) } doReturn Maybe.just(DEFAULT_STATION_DETAILS)
                    }
                    on { schemeInfoService() } doReturn schemeInfoService
                }
                on { preferredSchemeComponent() } doReturn Observable.just(schemeComponent)
            }
        }

        val schemeRepository by memoized { SchemeStateRepository() }

        val accentUseCase by memoized { AccentUseCase(schemeRepository) }

        val userLocationProvider by memoized {
            mock<UserLocationProvider> {
                on { nearestStation() } doReturn Observable.just(defaultNearestStation.toOptional())
            }
        }

        context("fragment back stack contains prohibited fragments") {
            val transactionHistory = FragmentTransactionHistory(
                    listOf(
                            FragmentTransaction("id", StationCardFragment::class.uniqueTag(), null, null)
                    ),
                    1)

            val fragmentBackStack by memoized {
                mock<FragmentBackStack> {
                    on { changeEvents() } doReturn Observable.create { it.onNext(transactionHistory) }
                }
            }


            context("usecase has been activated") {
                val useCase = FillDepartureStationUseCase(
                        schemeComponentProvider,
                        schemeRepository,
                        selectionUseCase,
                        userLocationProvider,
                        fragmentBackStack,
                        accentUseCase,
                        metricsLogging
                )


                it("should NOT update accent") {
                    useCase.fillDeparture().test()

                    accentUseCase
                            .accentChanges()
                            .test()
                            .assertValue { it == Accent.None }
                }
            }
        }

        context("fragment back stack doesn't contain prohibited fragments") {
            val fragmentBackStack by memoized {
                mock<FragmentBackStack> {
                    on { changeEvents() } doReturn Observable.create { it.onNext(FragmentTransactionHistory(emptyList(), 1)) }
                }
            }

            context("scheme camera has been changed and usecase has been activated") {
                val useCase = FillDepartureStationUseCase(
                        schemeComponentProvider,
                        schemeRepository,
                        selectionUseCase,
                        userLocationProvider,
                        fragmentBackStack,
                        accentUseCase,
                        metricsLogging
                )

                useCase.notifySchemeCameraWasChanged().subscribe()

                useCase.fillDeparture().test()

                it("should NOT update accent") {
                    accentUseCase
                            .accentChanges()
                            .test()
                            .assertValue { it == Accent.None }
                }
            }
        }

    }

    context("nearest station has been retrieved") {
        val userLocationProvider by memoized {
            mock<UserLocationProvider> {
                on { nearestStation() } doReturn Observable.just(defaultNearestStation.toOptional())
            }
        }
        context("nearest station is closed") {
            val schemeComponentProvider by memoized {
                mock<PreferredSchemeComponentProvider> {
                    val schemeComponent = mock<SchemeComponent> {
                        val schemeInfoService = mock<SchemeInfoService> {
                            on { stationDetails(DEFAULT_STATION_ID) } doReturn Maybe.just(CLOSED_STATION_DETAILS)
                        }
                        on { schemeInfoService() } doReturn schemeInfoService
                    }
                    on { preferredSchemeComponent() } doReturn Observable.just(schemeComponent)
                }
            }

            context("selection is empty") {
                val selectionUseCase by memoized {
                    mock<StationSelectionUseCase> {
                        on { stationsSelection() } doReturn
                                Observable.create<StationsSelection> { emitter -> emitter.onNext(StationsSelection.empty()) }

                        on { select(eq(DEFAULT_SERVICE_STATION_ID)) } doReturn Completable.complete()
                    }
                }

                val schemeRepository by memoized { SchemeStateRepository() }

                val accentUseCase by memoized { AccentUseCase(schemeRepository) }

                val fragmentBackStack by memoized {
                    mock<FragmentBackStack> {
                        on { changeEvents() } doReturn Observable.create { emitter -> emitter.onNext(FragmentTransactionHistory(emptyList(), 1)) }
                    }
                }

                context("usecase has been activated") {
                    val useCase by memoized {
                        FillDepartureStationUseCase(
                                schemeComponentProvider,
                                schemeRepository,
                                selectionUseCase,
                                userLocationProvider,
                                fragmentBackStack,
                                accentUseCase,
                                metricsLogging
                        )
                    }

                    beforeEachTest {
                        useCase.fillDeparture().test()
                    }

                    it("should fill station selection") {
                        verify(selectionUseCase, times(1)).select(DEFAULT_SERVICE_STATION_ID)
                    }

                    it("should invoke metrics") {
                        verify(metricsLogging, times(1)).routePointFromNearby(DEFAULT_STATION)
                    }

                    it("should update accent") {
                        accentUseCase
                                .accentChanges()
                                .test()
                                .assertValue { it is Accent.Station }
                    }
                }
            }
        }
    }
})

private object Data {
    val DEFAULT_STATION_ID = StationId("1")

    val DEFAULT_STATION: Station = StationByData(
            DEFAULT_STATION_ID,
            LocalizedString(com.yandex.metrokit.LocalizedString.fromNonlocalizedValue("Park Kultur")), listOf(), listOf(), null, null)

    val DEFAULT_STATION_DETAILS: StationDetails = StationDetailsByData(
            DEFAULT_STATION,
            emptyList(),
            emptyList(),
            null,
            emptyList(),
            emptyList(),
            GeoRectByData(GeoPointByData(.0, .0), GeoPointByData(.0, .0)),
            GeoPointByData(.0, .0)
    )

    val CLOSED_STATION_DETAILS: StationDetails = StationDetailsByData(
            DEFAULT_STATION,
            emptyList(),
            emptyList(),
            null,
            emptyList(),
            listOf(StationFlag.NOTICE_CLOSED),
            GeoRectByData(GeoPointByData(.0, .0), GeoPointByData(.0, .0)),
            GeoPointByData(.0, .0)
    )

    val DEFAULT_SERVICE_STATION = ServiceStationByData(
            station = DEFAULT_STATION,
            displayService = null,
            services = DEFAULT_STATION_DETAILS.services
    )

    val DEFAULT_SERVICE_STATION_ID = DEFAULT_SERVICE_STATION.id
}
