package ru.yandex.realty.search.site;

import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import ru.yandex.realty.model.offer.BuildingState;
import ru.yandex.realty.model.sites.ExtendedSiteStatistics;
import ru.yandex.realty.model.sites.ExtendedSiteStatisticsAtom;
import ru.yandex.realty.model.sites.Phase;
import ru.yandex.realty.model.sites.Site;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Comparator.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class DeliveryDatesBuilderTest {
    private static final Comparator<DeliveryDatesBuilder.DeliveryDate> FINISHED_FIRST_COMPARATOR =
        comparing(DeliveryDatesBuilder.DeliveryDate::getFinished, nullsFirst(naturalOrder())).reversed();

    @Test
    public void testPhasesToDeliveryDates() throws Exception {
        IntStream.rangeClosed(0, PhaseCoverageEnum.values().length).boxed()
                .forEach(this::testPhasesToDeliveryDatesHelper);
    }

    @Test
    public void testNoPhasesToDeliveryDates() throws Exception {
        Site site = genSite();
        List<DeliveryDatesBuilder.DeliveryDate> dates = DeliveryDatesBuilder.build(site,
                new ExtendedSiteStatistics(ExtendedSiteStatisticsAtom.EMPTY, false));
        printDeliveryDates(site, dates);
        assertTrue(dates.isEmpty());
    }

    private static List<Phase> newPhases(Site site, long id, PhaseCoverageEnum pc) {
        List<Phase> results = newArrayList();
        if (pc.year == null) {
            return Collections.nCopies(3, newPhase(site, id, pc, null));
        }
        Random random = ThreadLocalRandom.current();
        LocalDate today = LocalDate.now();
        int currentYear = today.getYear();
        results.add(newPhase(site, random.nextLong(), pc, currentYear - 1));
        results.add(newPhase(site, random.nextLong(), pc, currentYear));
        results.add(newPhase(site, random.nextLong(), pc, currentYear + 1));
        return results;
    }

    private static Phase newPhase(Site site, long id, PhaseCoverageEnum pc, Integer year) {
        Phase phase = new Phase(id);
        phase.setHouses(Collections.emptyList());
        phase.setCode("blablabla");
        phase.setDescription("description");
        Instant thatYear = year == null
                         ? null
                         : ZonedDateTime.of(year, 1, 1, 0, 0, 0, 0, ZoneId.of("Europe/Moscow")).toInstant();
        Instant thatQuarter = year == null
                            ? null
                            : pc.quarter == null
                            ? null
                            : ZonedDateTime.of(year, 3 * pc.quarter, 30, 0, 0, 0, 0, ZoneId.of("Europe/Moscow")).toInstant();
        phase.setState(BuildingState.UNKNOWN);
        switch (pc) {
            case NEITHER_YEAR_NOR_QUARTER_NOR_FINISHED:
                phase.setFinishDate(null);
                break;
            case YEAR_NO_QUARTER_NOT_FINISHED:
                assert thatYear != null;
                phase.setFinishDate(Date.from(thatYear));
                break;
            case YEAR_AND_QUARTER_NO_FINISHED:
                assert thatQuarter != null;
                phase.setFinishDate(Date.from(thatQuarter));
                break;
            case YEAR_AND_QUARTER_NOT_FINISHED:
                assert thatQuarter != null;
                phase.setFinishDate(Date.from(thatQuarter));
                break;
            case YEAR_AND_QUARTER_FINISHED:
                assert thatQuarter != null;
                phase.setFinishDate(Date.from(thatQuarter));
                phase.setState(BuildingState.HAND_OVER);
                break;
            default:
                throw new IllegalStateException("unhandled test case");
        }
        phase.setSite(site);
        return phase;
    }

    private void testPhasesToDeliveryDatesHelper(int numberOfPhases) {
        Site site = genSite();
        List<Phase> phases = getNConsecutivePhases(site, 3 * numberOfPhases);
        site.setPhases(phases);
        List<DeliveryDatesBuilder.DeliveryDate> dates = DeliveryDatesBuilder.build(site,
                new ExtendedSiteStatistics(ExtendedSiteStatisticsAtom.EMPTY, false));
//        printDeliveryDates(site, dates);
        assertEquals(phases.size(), dates.size());

        for (int i = 0; i < dates.size() - 1; i++) {
            assertTrue(FINISHED_FIRST_COMPARATOR.compare(dates.get(i), dates.get(i + 1)) <= 0);
        }
    }

    @NotNull
    private static Site genSite() {
        return new Site(424242L);
    }

    private void printDeliveryDates(Site site, List<DeliveryDatesBuilder.DeliveryDate> dates) {
        System.out.println(String.format("Handled the case when phases=%s", site.getPhases()));
        System.out.println(dates);
    }

    @NotNull
    private List<Phase> getNConsecutivePhases(Site site, int numberOfPhases) {
        List<Phase> phases = getPhasesUniverse(site).subList(0, numberOfPhases);
//        System.out.println(String.format("About to handle cases when phases=%s", phases));
        return phases;
    }

    @NotNull
    private List<Phase> getPhasesUniverse(Site site) {
        List<Phase> result = newArrayList();
        result.addAll(newPhases(site, 12345, PhaseCoverageEnum.NEITHER_YEAR_NOR_QUARTER_NOR_FINISHED));
        result.addAll(newPhases(site, 2345, PhaseCoverageEnum.YEAR_NO_QUARTER_NOT_FINISHED));
        result.addAll(newPhases(site, 3456, PhaseCoverageEnum.YEAR_AND_QUARTER_NO_FINISHED));
        result.addAll(newPhases(site, 871234, PhaseCoverageEnum.YEAR_AND_QUARTER_NOT_FINISHED));
        result.addAll(newPhases(site, 981247, PhaseCoverageEnum.YEAR_AND_QUARTER_FINISHED));
        return result;
    }

    private enum PhaseCoverageEnum {
        NEITHER_YEAR_NOR_QUARTER_NOR_FINISHED(null, null, null),
        YEAR_NO_QUARTER_NOT_FINISHED(Integer.MIN_VALUE, null, false),
        YEAR_AND_QUARTER_NO_FINISHED(Integer.MIN_VALUE, 2, null),
        YEAR_AND_QUARTER_NOT_FINISHED(Integer.MIN_VALUE, 2, false),
        YEAR_AND_QUARTER_FINISHED(Integer.MIN_VALUE, 2, true),
        ;

        public final Integer year, quarter;
        public final Boolean finished;

        PhaseCoverageEnum(Integer year, Integer quarter, Boolean finished) {
            this.year = year;
            this.quarter = quarter;
            this.finished = finished;
        }
    }
}
