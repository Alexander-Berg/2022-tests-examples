package ru.yandex.solomon.expression.analytics;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;

import ru.yandex.solomon.expression.NamedGraphData;
import ru.yandex.solomon.expression.value.SelValue;
import ru.yandex.solomon.model.timeseries.GraphData;
import ru.yandex.solomon.util.time.Interval;

public final class MultilineProgramTestSupport {
    private MultilineProgramTestSupport() {
    }

    public static ChainedProgram create() {
        return new ChainedProgram("");
    }

    public static ChainedProgram fromCode(String code) {
        return new ChainedProgram(code);
    }

    public static class ChainedProgram {
        private String source;
        private Instant from;
        private Instant to;
        private GraphDataLoaderStub loader = new GraphDataLoaderStub();

        public ChainedProgram(String code) {
            this.source = code;
        }

        public ChainedProgram addLine(String line) {
            source += "\n" + line;
            return this;
        }

        public ChainedProgram fromTime(String time) {
            from = Instant.parse(time);
            return this;
        }

        public ChainedProgram toTime(String time) {
            to = Instant.parse(time);
            return this;
        }

        public ChainedProgram forTimeInterval(Interval interval) {
            from = interval.getBegin();
            to = interval.getEnd();
            return this;
        }

        public ChainedProgram onData(GraphDataLoaderStub loader) {
            this.loader = loader;
            return this;
        }

        public ChainedProgram onData(String selector, GraphData... graphData) {
            loader.putSelectorValue(selector, graphData);
            return this;
        }

        public ChainedProgram onData(String selector, NamedGraphData... namedGraphData) {
            loader.putSelectorValue(selector, namedGraphData);
            return this;
        }

        public MultilineExecResult exec() {
            Program program = Program.fromSource(source).compile();
            return new MultilineExecResult(program
                .prepare(new Interval(from, to))
                .evaluate(loader, Collections.emptyMap()));
        }

        public ProgramTestSupport.ExecResult exec(String varName) {
            return exec().get(varName);
        }

        public static class MultilineExecResult {
            private final Map<String, SelValue> result;

            public MultilineExecResult(Map<String, SelValue> result) {
                this.result = result;
            }

            public ProgramTestSupport.ExecResult get(String varName) {
                return new ProgramTestSupport.ExecResult(result.get(varName));
            }
        }
    }
}
