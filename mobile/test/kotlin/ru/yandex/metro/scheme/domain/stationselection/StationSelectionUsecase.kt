package ru.yandex.metro.scheme.domain.stationselection

import ru.yandex.metro.ClassSpek
import ru.yandex.metro.scheme.domain.AppState
import ru.yandex.metro.scheme.domain.SchemeStateRepository
import ru.yandex.metro.scheme.domain.model.ServiceStationId
import ru.yandex.metro.scheme.domain.model.StationId
import ru.yandex.metro.scheme.domain.stationselection.model.Accent
import ru.yandex.metro.scheme.domain.stationselection.model.FocusType
import ru.yandex.metro.scheme.domain.stationselection.model.SelectedStationLabel
import ru.yandex.metro.scheme.domain.stationselection.model.StationSelectionInfo
import ru.yandex.metro.scheme.domain.stationselection.model.StationsSelection

val SAME_SERVICE_STATION = ServiceStationId(StationId("1"), null)
val ANOTHER_SERVICE_STATION = ServiceStationId(StationId("2"), null)

class StationSelectionUseCaseSpec : ClassSpek(StationSelectionUseCase::class.java, {
    context("selection") {
        context("current selection contains no station") {
            val useCase by memoized { StationSelectionUseCase(SchemeStateRepository()) }

            context("select by id") {
                beforeEach {
                    useCase.select(SAME_SERVICE_STATION).subscribe()
                }

                it("should emit selection with filled departure") {
                    val selection = useCase.stationsSelection().test()
                    selection.assertValueCount(1)
                    selection.assertValue { s -> s.a == SAME_SERVICE_STATION && s.b == null }
                }
            }

            context("select by id and label A") {
                beforeEachTest {
                    useCase.select(StationSelectionInfo(SAME_SERVICE_STATION, SelectedStationLabel.A)).subscribe()
                }

                it("should emit selection with filled departure") {
                    val selection = useCase.stationsSelection().test()
                    selection.assertValueCount(1)
                    selection.assertValue { s -> s.a == SAME_SERVICE_STATION && s.b == null }
                }
            }

            context("select by id and label B") {
                beforeEachTest {
                    useCase.select(StationSelectionInfo(SAME_SERVICE_STATION, SelectedStationLabel.B)).subscribe()
                }

                it("should emit selection with filled destination") {
                    val selection = useCase.stationsSelection().test()
                    selection.assertValueCount(1)
                    selection.assertValue { s -> s.a == null && s.b == SAME_SERVICE_STATION }
                }
            }

            context("select by station selection") {
                beforeEachTest {
                    useCase.select(StationsSelection(SAME_SERVICE_STATION, ANOTHER_SERVICE_STATION)).subscribe()
                }

                it("should emit selection with filled departure and destination") {
                    val selection = useCase.stationsSelection().test()
                    selection.assertValueCount(1)
                    selection.assertValue { s -> s.a == SAME_SERVICE_STATION && s.b == ANOTHER_SERVICE_STATION }
                }
            }
        }

        context("current selection contains departure") {
            val useCase by memoized {
                StationSelectionUseCase(SchemeStateRepository()).also {
                    it.select(SAME_SERVICE_STATION).subscribe()
                }
            }
            context("select station with another id by id") {
                beforeEachTest {
                    useCase.select(ANOTHER_SERVICE_STATION).subscribe()
                }

                it("should emit selection with filled both departure and destination") {
                    val selection = useCase.stationsSelection().test()
                    selection.assertValueCount(1)
                    selection.assertValue { s -> s.a == SAME_SERVICE_STATION && s.b == ANOTHER_SERVICE_STATION }
                }
            }
            context("select station with same id by id") {
                beforeEachTest {
                    useCase.select(SAME_SERVICE_STATION).subscribe()
                }

                it("should emit selection only with filled departure") {
                    val selection = useCase.stationsSelection().test()
                    selection.assertValueCount(1)
                    selection.assertValue { s -> s.a == SAME_SERVICE_STATION && s.b == null }
                }
            }

            context("select by label with same label but another station id") {
                beforeEachTest {
                    useCase.select(StationSelectionInfo(ANOTHER_SERVICE_STATION, SelectedStationLabel.A)).subscribe()
                }

                it("should emit selection with updated departure") {
                    val selection = useCase.stationsSelection().test()
                    selection.assertValueCount(1)
                    selection.assertValue { s -> s.a == ANOTHER_SERVICE_STATION && s.b == null }
                }
            }
            context("select by label with another label but same station id") {
                beforeEachTest {
                    useCase.select(StationSelectionInfo(SAME_SERVICE_STATION, SelectedStationLabel.B)).subscribe()
                }

                it("should emit selection with filled destination and empty departure") {
                    val selection = useCase.stationsSelection().test()
                    selection.assertValueCount(1)
                    selection.assertValue { s -> s.a == null && s.b == SAME_SERVICE_STATION }
                }
            }
        }

        context("current selection has filled departure and destination") {
            val useCase by memoized {
                StationSelectionUseCase(SchemeStateRepository()).also {
                    it.select(SAME_SERVICE_STATION).subscribe()
                    it.select(ANOTHER_SERVICE_STATION).subscribe()
                }
            }
            context("select station by label A with station from field B") {
                beforeEachTest {
                    useCase.select(StationSelectionInfo(ANOTHER_SERVICE_STATION, SelectedStationLabel.A)).subscribe()
                }

                it("should emit swapped selection") {
                    val selection = useCase.stationsSelection().test()
                    selection.assertValueCount(1)
                    selection.assertValue { s -> s.a == ANOTHER_SERVICE_STATION && s.b == SAME_SERVICE_STATION }
                }
            }

            context("select station B with station from field A") {
                beforeEachTest {
                    useCase.select(StationSelectionInfo(SAME_SERVICE_STATION, SelectedStationLabel.B)).subscribe()
                }

                it("should emit swapped selection") {
                    val selection = useCase.stationsSelection().test()
                    selection.assertValueCount(1)
                    selection.assertValue { s -> s.a == ANOTHER_SERVICE_STATION && s.b == SAME_SERVICE_STATION }
                }
            }

            context("select new station as point A") {
                val newStation = ServiceStationId(StationId("3"), null)
                beforeEachTest {
                    useCase.select(StationSelectionInfo(newStation, SelectedStationLabel.A)).subscribe()
                }

                it("should emit selection with new station as point A") {
                    val selection = useCase.stationsSelection().test()
                    selection.assertValueCount(1)
                    selection.assertValue { s -> s.a == newStation && s.b == ANOTHER_SERVICE_STATION }
                }
            }

            context("select new station as point B") {
                val newStation = ServiceStationId(StationId("3"), null)
                beforeEachTest {
                    useCase.select(StationSelectionInfo(newStation, SelectedStationLabel.B)).subscribe()
                }
                it("should emit selection with new station as point B") {
                    val selection = useCase.stationsSelection().test()
                    selection.assertValueCount(1)
                    selection.assertValue { s -> s.a == SAME_SERVICE_STATION && s.b == newStation }
                }
            }
        }
    }

    context("selection with accent") {
        val schemeStateRepository by memoized { SchemeStateRepository() }

        context("current selection contains no station") {
            val useCase by memoized { StationSelectionUseCase(schemeStateRepository) }

            context("selecting `from` station") {
                val fromSelection = StationSelectionInfo(
                        SAME_SERVICE_STATION,
                        SelectedStationLabel.A
                )

                beforeEachTest {
                    useCase.selectWithAccent(fromSelection).subscribe()
                }

                it("should emit accent") {
                    val accentChanges = schemeStateRepository.observe(AppState::accent).test()
                    accentChanges.assertValueCount(1)
                    accentChanges.assertValue(Accent.Station(SAME_SERVICE_STATION, FocusType.CENTER))
                }
            }

            context("selecting `to` station") {
                val toSelection = StationSelectionInfo(
                        SAME_SERVICE_STATION,
                        SelectedStationLabel.A
                )

                beforeEachTest {
                    useCase.selectWithAccent(toSelection).subscribe()
                }

                it("should emit accent") {
                    val accentChanges = schemeStateRepository.observe(AppState::accent).test()
                    accentChanges.assertValueCount(1)
                    accentChanges.assertValue(Accent.Station(SAME_SERVICE_STATION, FocusType.CENTER))
                }
            }
        }

        context("current selection contains `from` station") {
            val useCase by memoized {
                StationSelectionUseCase(schemeStateRepository).apply {
                    select(SAME_SERVICE_STATION).subscribe()
                }
            }

            context("selecting `from` station with another id") {
                val fromSelection = StationSelectionInfo(
                        ANOTHER_SERVICE_STATION,
                        SelectedStationLabel.A
                )

                beforeEachTest {
                    useCase.selectWithAccent(fromSelection).subscribe()
                }

                it("should emit accent with new station") {
                    val accentChanges = schemeStateRepository.observe(AppState::accent).test()
                    accentChanges.assertValue(Accent.Station(ANOTHER_SERVICE_STATION, FocusType.CENTER))
                    accentChanges.assertValueCount(1)
                }
            }

            context("selecting 'to' station with the same id") {
                val toSelection = StationSelectionInfo(
                        SAME_SERVICE_STATION,
                        SelectedStationLabel.B
                )

                beforeEachTest {
                    useCase.selectWithAccent(toSelection).subscribe()
                }

                it("should emit new accent with the same station") {
                    val accentChanges = schemeStateRepository.observe(AppState::accent).test()
                    accentChanges.assertValue(Accent.Station(SAME_SERVICE_STATION, FocusType.CENTER))
                    accentChanges.assertValueCount(1)
                }

                it("new stations selection should contain only `to` station") {
                    val stationSelection = useCase.stationsSelection().test()
                    val expectedSelection = StationsSelection(b = SAME_SERVICE_STATION)
                    stationSelection.assertValue(expectedSelection)
                }
            }

            context("selecting `to` station with another id") {
                val toSelection = StationSelectionInfo(
                        ANOTHER_SERVICE_STATION,
                        SelectedStationLabel.B
                )

                beforeEachTest {
                    useCase.selectWithAccent(toSelection).subscribe()
                }

                it("should emit accent.none") {
                    val accentChanges = schemeStateRepository.observe(AppState::accent).test()
                    accentChanges.assertValueCount(1)
                    accentChanges.assertValue(Accent.None)
                }

                it("new stations selection should contain `from` and `to` stations") {
                    val stationSelection = useCase.stationsSelection().test()
                    val expectedSelection = StationsSelection(a = SAME_SERVICE_STATION, b = ANOTHER_SERVICE_STATION)
                    stationSelection.assertValue(expectedSelection)
                }
            }
        }
    }
})
