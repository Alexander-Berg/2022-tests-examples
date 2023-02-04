package ru.yandex.partner.core.filter.db;

import java.util.Set;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.jooq.Condition;
import org.junit.jupiter.api.Test;

import ru.yandex.partner.core.entity.custombkoptions.CustomBkOptionsMultistateGraph;
import ru.yandex.partner.core.entity.custombkoptions.model.CustomBkOptions;
import ru.yandex.partner.core.entity.dsp.model.Dsp;
import ru.yandex.partner.core.entity.dsp.multistate.DspActionChecksService;
import ru.yandex.partner.core.entity.dsp.multistate.DspMultistateGraph;
import ru.yandex.partner.core.entity.mirror.MirrorMultistateGraph;
import ru.yandex.partner.core.entity.mirror.model.BaseMirror;
import ru.yandex.partner.core.entity.page.model.ContextPage;
import ru.yandex.partner.core.entity.page.multistate.ContextPageMultistateGraph;
import ru.yandex.partner.core.filter.dbmeta.NumberFilter;
import ru.yandex.partner.core.filter.meta.MultistateMetaFilter;
import ru.yandex.partner.core.multistate.Multistate;
import ru.yandex.partner.core.multistate.custombkoptions.CustomBkOptionsStateFlag;
import ru.yandex.partner.core.multistate.dsp.DspStateFlag;
import ru.yandex.partner.core.multistate.mirror.MirrorStateFlag;
import ru.yandex.partner.core.multistate.page.PageStateFlag;

import static java.util.function.Predicate.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static ru.yandex.partner.core.filter.operator.FilterOperator.IN;
import static ru.yandex.partner.dbschema.partner.Tables.CONTEXT_ON_SITE_MIRRORS;
import static ru.yandex.partner.dbschema.partner.Tables.CUSTOM_BK_OPTIONS;
import static ru.yandex.partner.dbschema.partner.Tables.DSP;
import static ru.yandex.partner.dbschema.partner.tables.Pages.PAGES;
import static ru.yandex.partner.libs.multistate.MultistatePredicates.has;


class MultistateDbFilterTest {

    @Test
    public void dspTest() {
        var predicate = not(has(DspStateFlag.DELETED));
        DspMultistateGraph graph = new DspMultistateGraph(new DspActionChecksService());

        Set<Long> values = graph.getMultistatesForPredicate(predicate)
                .stream().map(Multistate::toMultistateValue)
                .collect(Collectors.toSet());

        NumberFilter<Dsp, Long> multistateNumberFilter = new NumberFilter<>("miltistate_number", Dsp.class,
                DSP.MULTISTATE);

        MultistateMetaFilter<Dsp, DspStateFlag> multistateMetaFilter =
                new MultistateMetaFilter<>("multistate", Dsp.class);

        MultistateDbFilter<Dsp, DspStateFlag> multistateFilter =
                new MultistateDbFilter<>(multistateMetaFilter, Dsp.class, graph, DSP.MULTISTATE);

        Condition expectedCondition = multistateNumberFilter.getCondition(IN, values);
        Condition actualCondition = multistateFilter.getCondition(IN, predicate);

        assertEquals(getValuesFromCondition(expectedCondition), getValuesFromCondition(actualCondition));
    }

    @Test
    public void customBkOptionsTest() {
        var predicate = has(CustomBkOptionsStateFlag.DELETED);

        CustomBkOptionsMultistateGraph graph = new CustomBkOptionsMultistateGraph();

        Set<Long> values = graph.getMultistatesForPredicate(predicate)
                .stream().map(Multistate::toMultistateValue)
                .collect(Collectors.toSet());

        NumberFilter<CustomBkOptions, Long> multistateNumberFilter = new NumberFilter<>("miltistate_number",
                CustomBkOptions.class, CUSTOM_BK_OPTIONS.MULTISTATE);

        MultistateMetaFilter<CustomBkOptions, CustomBkOptionsStateFlag> multistateMetaFilter =
                new MultistateMetaFilter<>("multistate", CustomBkOptions.class);

        MultistateDbFilter<CustomBkOptions, CustomBkOptionsStateFlag> multistateFilter =
                new MultistateDbFilter<>(multistateMetaFilter, CustomBkOptions.class,
                        graph, CUSTOM_BK_OPTIONS.MULTISTATE);

        Condition expectedCondition = multistateNumberFilter.getCondition(IN, values);
        Condition actualCondition = multistateFilter.getCondition(IN, predicate);

        assertEquals(getValuesFromCondition(expectedCondition), getValuesFromCondition(actualCondition));
    }

    @Test
    public void contextOnSiteMirrorsTest() {
        var predicate = has(MirrorStateFlag.REJECTED);
        MirrorMultistateGraph graph = new MirrorMultistateGraph();

        Set<Long> values = graph.getMultistatesForPredicate(predicate)
                .stream().map(Multistate::toMultistateValue)
                .collect(Collectors.toSet());

        NumberFilter<BaseMirror, Long> multistateNumberFilter = new NumberFilter<>("miltistate_number",
                BaseMirror.class,
                CONTEXT_ON_SITE_MIRRORS.MULTISTATE);

        MultistateMetaFilter<BaseMirror, MirrorStateFlag> multistateMetaFilter =
                new MultistateMetaFilter<>("multistate", BaseMirror.class);

        MultistateDbFilter<BaseMirror, MirrorStateFlag> multistateFilter =
                new MultistateDbFilter<>(multistateMetaFilter, BaseMirror.class,
                        graph, CONTEXT_ON_SITE_MIRRORS.MULTISTATE);

        Condition expectedCondition = multistateNumberFilter.getCondition(IN, values);
        Condition actualCondition = multistateFilter.getCondition(IN, predicate);

        assertEquals(getValuesFromCondition(expectedCondition), getValuesFromCondition(actualCondition));
    }

    @Test
    public void pageTest() {
        var predicate = not(has(PageStateFlag.DELETED));
        ContextPageMultistateGraph graph = new ContextPageMultistateGraph();

        Set<Long> values = graph.getMultistatesForPredicate(predicate)
                .stream().map(Multistate::toMultistateValue)
                .collect(Collectors.toSet());

        NumberFilter<ContextPage, Long> multistateNumberFilter = new NumberFilter<>("miltistate_number",
                ContextPage.class,
                PAGES.MULTISTATE);

        MultistateMetaFilter<ContextPage, PageStateFlag> multistateMetaFilter =
                new MultistateMetaFilter<>("multistate", ContextPage.class);

        MultistateDbFilter<ContextPage, PageStateFlag> multistateFilter =
                new MultistateDbFilter<>(multistateMetaFilter, ContextPage.class,
                        graph, PAGES.MULTISTATE);

        Condition expectedCondition = multistateNumberFilter.getCondition(IN, values);
        Condition actualCondition = multistateFilter.getCondition(IN, predicate);

        assertEquals(getValuesFromCondition(expectedCondition), getValuesFromCondition(actualCondition));
    }

    // helper method to extract values from SQL query part
    private Set<Long> getValuesFromCondition(Condition condition) {
        return Pattern.compile("\\d+").matcher(condition.toString()).results()
                .map(MatchResult::group)
                .map(Long::parseLong)
                .collect(Collectors.toSet());
    }
}
