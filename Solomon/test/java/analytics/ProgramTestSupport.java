package ru.yandex.solomon.expression.analytics;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Assert;

import ru.yandex.monlib.metrics.MetricType;
import ru.yandex.solomon.expression.NamedGraphData;
import ru.yandex.solomon.expression.exceptions.SelException;
import ru.yandex.solomon.expression.type.SelType;
import ru.yandex.solomon.expression.type.SelTypes;
import ru.yandex.solomon.expression.value.SelValue;
import ru.yandex.solomon.expression.value.SelValueGraphData;
import ru.yandex.solomon.expression.value.SelValueVector;
import ru.yandex.solomon.expression.version.SelVersion;
import ru.yandex.solomon.model.protobuf.MetricTypeConverter;
import ru.yandex.solomon.model.timeseries.AggrGraphDataArrayList;
import ru.yandex.solomon.model.timeseries.AggrGraphDataArrayListOrView;
import ru.yandex.solomon.model.timeseries.GraphData;
import ru.yandex.solomon.util.collection.array.LongArrayView;
import ru.yandex.solomon.util.time.Interval;

/**
 * @author Vladimir Gordiychuk
 */
public final class ProgramTestSupport {
    private ProgramTestSupport() {
    }

    public static GraphData execExprOnSingleLine(String expr, GraphData source) {
        return expression(expr)
            .onSingleLine(source)
            .exec()
            .getAsSingleLine();
    }

    public static GraphData execExprOnSingleLine(String expr, Interval interval, GraphData source) {
        return expression(expr)
            .forTimeInterval(interval)
            .onSingleLine(source)
            .exec()
            .getAsSingleLine();
    }

    public static ChainedProgram expression(String exp) {
        return new ChainedProgram(exp);
    }

    public static class Prepared {
        private final PreparedProgram prepared;
        private final GraphDataLoader loader;

        public Prepared(PreparedProgram prepared, GraphDataLoader loader) {
            this.prepared = prepared;
            this.loader = loader;
        }

        public ExecResult exec() {
            Map<String, SelValue> evaluationResult = prepared.evaluate(loader, Collections.emptyMap());
            SelValue expReturnValue = evaluationResult.get("data");
            return new ExecResult(expReturnValue);
        }

        public String explain(SelException e) {
            return prepared.explainError(e);
        }

        public PreparedProgram getPrepared() {
            return prepared;
        }
    }

    public static class ExecResult {
        private final SelValue result;

        ExecResult(SelValue result) {
            this.result = result;
        }

        public SelValue getAsSelValue() {
            return result;
        }

        public SelValueVector getAsVector() {
            return result.castToVector();
        }

        public GraphData getAsSingleLine() {
            GraphData[] multiLines = getAsMultipleLines();
            if (multiLines.length == 0) {
                return GraphData.empty;
            }
            if (multiLines.length > 1) {
                throw new IllegalArgumentException("Result has more than one line");
            }

            return multiLines[0];
        }

        public NamedGraphData getAsNamedSingleLine() {
            NamedGraphData[] multiLines = getAsNamedMultipleLines();
            if (multiLines.length == 0) {
                return NamedGraphData.of(GraphData.empty);
            }

            return multiLines[0];
        }

        public GraphData[] getAsMultipleLines() {
            return Stream.of(getAsNamedMultipleLines())
                .map(NamedGraphData::getGraphData)
                .toArray(GraphData[]::new);
        }

        public Interval getAsInterval() {
            return getAsSelValue()
                    .castToInterval()
                    .getInterval();
        }

        public NamedGraphData[] getAsNamedMultipleLines() {
            SelType type = result.type();
            if (type == SelTypes.GRAPH_DATA) {
                return new NamedGraphData[]{result.castToGraphData().getNamedGraphData()};
            }

            return Stream.of(result.castToVector().valueArray())
                .map(SelValue::castToGraphData)
                .map(SelValueGraphData::getNamedGraphData)
                .toArray(NamedGraphData[]::new);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            ExecResult that = (ExecResult) o;
            return result.equals(that.result);
        }

        @Override
        public int hashCode() {
            return Objects.hash(result);
        }

        @Override
        public String toString() {
            return "ExecResult{" +
                    "result=" + result +
                    '}';
        }
    }

    public static class ChainedProgram {
        private final String expr;
        private String variables = "";
        private NamedGraphData[] source = new NamedGraphData[0];
        private SelType sourceType = SelTypes.GRAPH_DATA_VECTOR;
        private Instant from;
        private Instant to;

        public ChainedProgram(String expr) {
            this.expr = expr;
        }

        public ChainedProgram fromTime(String time) {
            from = Instant.parse(time);
            return this;
        }

        public ChainedProgram toTime(String time) {
            to = Instant.parse(time);
            return this;
        }

        public ChainedProgram forVariables(String variables) {
            this.variables = variables;
            return this;
        }

        public ChainedProgram forTimeInterval(Interval interval) {
            from = interval.getBegin();
            to = interval.getEnd();
            return this;
        }

        public ChainedProgram onMultipleLines(GraphData... source) {
            this.source = Stream.of(source).map(NamedGraphData::of).toArray(NamedGraphData[]::new);
            this.sourceType = SelTypes.GRAPH_DATA_VECTOR;
            return this;
        }

        public ChainedProgram onMultipleLines(AggrGraphDataArrayList... sources) {
            return onMultipleLines(MetricType.DGAUGE, sources);
        }

        public ChainedProgram onMultipleLines(MetricType type, AggrGraphDataArrayList... sources) {
            var typeProto = MetricTypeConverter.toNotNullProto(type);
            this.source = Stream.of(sources).map(s -> NamedGraphData.of(typeProto, s)).toArray(NamedGraphData[]::new);
            this.sourceType = SelTypes.GRAPH_DATA_VECTOR;
            return this;
        }

        public ChainedProgram onMultipleLines(NamedGraphData... source) {
            this.source = source;
            this.sourceType = SelTypes.GRAPH_DATA_VECTOR;
            return this;
        }

        public ChainedProgram onSingleLine(GraphData source) {
            this.source = new NamedGraphData[]{NamedGraphData.of(source)};
            this.sourceType = SelTypes.GRAPH_DATA;
            return this;
        }

        public ChainedProgram onSingleLine(AggrGraphDataArrayList source) {
            return onSingleLine(MetricType.DGAUGE, source);
        }

        public ChainedProgram onSingleLine(MetricType type, AggrGraphDataArrayList source) {
            var typeProto = MetricTypeConverter.toNotNullProto(type);
            this.source = new NamedGraphData[]{NamedGraphData.of(typeProto, source)};
            this.sourceType = SelTypes.GRAPH_DATA;
            return this;
        }

        public ChainedProgram onSingleLine(NamedGraphData source) {
            this.source = new NamedGraphData[]{source};
            this.sourceType = SelTypes.GRAPH_DATA;
            return this;
        }

        public ExecResult exec() {
            return exec(Function.identity());
        }

        public ExecResult exec(Predicate<SelVersion> onlyVersions) {
            return exec(Function.identity(), onlyVersions);
        }

        public <U> U exec(Function<ExecResult, U> finisher) {
            return exec(finisher, version -> true);
        }

        // Exec for each sel version and compare finalized results
        public <U> U exec(Function<ExecResult, U> finisher, Predicate<SelVersion> onlyVersions) {
            SelVersion[] versions = Arrays.stream(SelVersion.values()).filter(onlyVersions).toArray(SelVersion[]::new);
            List<U> resultByVersion = Arrays.stream(versions)
                    .map(this::exec)
                    .map(finisher)
                    .collect(Collectors.toList());

            for (int i = 0; i < resultByVersion.size() - 1; i++) {
                U firstResult = resultByVersion.get(i);
                U secondResult = resultByVersion.get(i + 1);
                if (firstResult.getClass().isArray()) {
                    Assert.assertArrayEquals(versions[i] + " vs " + versions[i + 1],
                            (Object[])firstResult, (Object[])secondResult);
                } else {
                    Assert.assertEquals(versions[i] + " vs " + versions[i + 1], firstResult, secondResult);
                }
            }

            return resultByVersion.get(0);
        }

        public ExecResult exec(SelVersion version) {
            var prepared = prepare(version);
            try {
                return prepared.exec();
            } catch (SelException e) {
                System.err.println("On version: " + version);
                System.err.println(prepared.explain(e));
                throw e;
            }
        }

        public Prepared prepare(SelVersion version) {
            String fullProgram = variables + prepareProgramSrc(expr);
            try {
                System.out.println("Prepare program:\n" + fullProgram);
                Program program = Program.fromSource(version, fullProgram).compile();
                PreparedProgram preparedProgram = program.prepare(getInterval());

                GraphDataLoaderStub loaderStub = new GraphDataLoaderStub();
                loaderStub.putSelectorValue("name=testData", source);
                return new Prepared(preparedProgram, loaderStub);
            } catch (SelException e) {
                System.err.println("On version: " + version);
                System.err.println(Program.explainError(fullProgram, e));
                throw e;
            }
        }

        private Interval getInterval() {
            Interval dataInterval = Arrays.stream(source)
                    .map(NamedGraphData::getAggrGraphDataArrayList)
                    .filter(gd -> !gd.isEmpty())
                    .map(AggrGraphDataArrayListOrView::getTimestamps)
                    .map(ProgramTestSupport.ChainedProgram::interval)
                    .reduce(Interval::convexHull)
                    .orElseGet(() -> Interval.before(Instant.now(), Duration.ofDays(1)));

            if (from == null) {
                from = dataInterval.getBegin();
            }

            if (to == null) {
                to = dataInterval.getEnd();
            }

            return new Interval(from, to);
        }

        private static Interval interval(LongArrayView timestamps) {
            if (timestamps.isEmpty()) {
                return Interval.EMPTY;
            }

            return Interval.millis(timestamps.first(), timestamps.last());
        }

        private String prepareProgramSrc(String expression) {
            // Separate variable use to avoid pushdown of downsampling
            if (sourceType == SelTypes.GRAPH_DATA_VECTOR) {
                return "let graphData = {name='testData'};\n"
                        + "let data = " + expression;
            } else {
                return "let graphData = single({name='testData'});\n"
                        + "let data = " + expression;
            }
        }
    }
}
