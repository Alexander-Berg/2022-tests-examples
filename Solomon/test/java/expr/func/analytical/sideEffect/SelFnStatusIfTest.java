package ru.yandex.solomon.expression.expr.func.analytical.sideEffect;

import java.util.Map;

import javax.annotation.Nonnull;

import org.junit.Test;

import ru.yandex.solomon.expression.analytics.GraphDataLoader;
import ru.yandex.solomon.expression.analytics.GraphDataLoaderStub;
import ru.yandex.solomon.expression.analytics.PrepareContext;
import ru.yandex.solomon.expression.analytics.PreparedProgram;
import ru.yandex.solomon.expression.analytics.Program;
import ru.yandex.solomon.expression.exceptions.CompilerException;
import ru.yandex.solomon.expression.exceptions.EvaluationException;
import ru.yandex.solomon.expression.expr.SideEffectProcessor;
import ru.yandex.solomon.expression.value.SelValue;
import ru.yandex.solomon.expression.value.SelValueSideEffect;
import ru.yandex.solomon.expression.value.SelValueStatus;
import ru.yandex.solomon.model.point.DataPoint;
import ru.yandex.solomon.model.timeseries.GraphData;
import ru.yandex.solomon.util.time.Interval;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;

/**
 * @author Ivan Tsybulin
 */
public class SelFnStatusIfTest {

    private static final Program SAMPLE_PROGRAM = Program.fromSource("" +
            "let data = data{};\n" +
            "no_data_if(count(data) == 0);\n" +
            "let val = avg(data);\n" +
            "alarm_if(val > 95);").compile();

    private GraphDataLoaderStub loader = new GraphDataLoaderStub();

    @Test(expected = EvaluationException.class)
    public void noSideProcessor() {
        PreparedProgram pp = SAMPLE_PROGRAM.prepare(Interval.seconds(0, 1000));
        loader.putSelectorValue("{sensor=='data'}", GraphData.empty);
        Map<String, SelValue> res = pp.evaluate(loader, Map.of());
    }

    @Test(expected = CompilerException.class)
    public void forbidAssignment() {
        Program.fromSource("let foo = no_data_if(true);")
                .compile()
                .prepare(Interval.seconds(0, 1000))
                .evaluate(GraphDataLoader.empty(), Map.of());
    }

    @Test
    public void terminateOnFirst() {
        StatusProcessor processor = new StatusProcessor();

        PreparedProgram pp = SAMPLE_PROGRAM
                .prepare(PrepareContext
                        .onInterval(Interval.seconds(0, 1000))
                        .withSideEffectsProcessor(processor)
                        .build());
        loader.putSelectorValue("{sensor=='data'}", GraphData.empty);
        Map<String, SelValue> res = pp.evaluate(loader, Map.of());
        assertThat(res, hasKey("data"));
        assertThat(res, not(hasKey("val")));
        assertThat(processor.getLast(), equalTo(SelValueStatus.NO_DATA));
    }

    @Test
    public void proceedToEnd() {
        StatusProcessor processor = new StatusProcessor();

        PreparedProgram pp = SAMPLE_PROGRAM
                .prepare(PrepareContext
                        .onInterval(Interval.seconds(0, 1000))
                        .withSideEffectsProcessor(processor)
                        .build());
        loader.putSelectorValue("{sensor=='data'}", GraphData.of(DataPoint.point(500_000, 42)));
        Map<String, SelValue> res = pp.evaluate(loader, Map.of());
        assertThat(res, hasKey("data"));
        assertThat(res, hasKey("val"));
        assertThat(processor.getLast(), equalTo(SelValueStatus.UNKNOWN));
    }

    @Test
    public void raiseAlarm() {
        StatusProcessor processor = new StatusProcessor();

        PreparedProgram pp = SAMPLE_PROGRAM
                .prepare(PrepareContext
                        .onInterval(Interval.seconds(0, 1000))
                        .withSideEffectsProcessor(processor)
                        .build());
        loader.putSelectorValue("{sensor=='data'}", GraphData.of(DataPoint.point(500_000, 142)));
        Map<String, SelValue> res = pp.evaluate(loader, Map.of());
        assertThat(res, hasKey("data"));
        assertThat(res, hasKey("val"));
        assertThat(processor.getLast(), equalTo(SelValueStatus.ALARM));
    }

    @Test
    public void constConditionInline() {
        Program code = Program.fromSource("warn_if(false); ok_if(true);").compile();

        assertThat(code.toString(), equalTo("SelValueStatus{status=UNKNOWN};\nSelValueStatus{status=OK};\n"));
    }

    @Test
    public void reuseCompiled() {

        loader.putSelectorValue("{sensor=='data'}", GraphData.of(
                DataPoint.point(500_000, 142),
                DataPoint.point(2500_000, 42)
        ));
        PreparedProgram pp1 = SAMPLE_PROGRAM.prepare(PrepareContext
                .onInterval(Interval.seconds(0, 1000))
                .withSideEffectsProcessor(new StatusProcessor())
                .build());
        pp1.evaluate(loader, Map.of());

        assertThat(((StatusProcessor)pp1.getSideEffectProcessor()).getLast(), equalTo(SelValueStatus.ALARM));

        PreparedProgram pp2 = SAMPLE_PROGRAM.prepare(PrepareContext
                .onInterval(Interval.seconds(1000, 2000))
                .withSideEffectsProcessor(new StatusProcessor())
                .build());
        pp2.evaluate(loader, Map.of());

        assertThat(((StatusProcessor)pp2.getSideEffectProcessor()).getLast(), equalTo(SelValueStatus.NO_DATA));

        PreparedProgram pp3 = SAMPLE_PROGRAM.prepare(PrepareContext
                .onInterval(Interval.seconds(2000, 3000))
                .withSideEffectsProcessor(new StatusProcessor())
                .build());
        pp3.evaluate(loader, Map.of());

        assertThat(((StatusProcessor)pp3.getSideEffectProcessor()).getLast(), equalTo(SelValueStatus.UNKNOWN));

    }

    private static class StatusProcessor implements SideEffectProcessor {
        private SelValueStatus last = SelValueStatus.UNKNOWN;

        SelValueStatus getLast() {
            return last;
        }

        @Override
        public void process(@Nonnull SelValueSideEffect sideEffect) {
            last = (SelValueStatus) sideEffect;
        }
    }

}
