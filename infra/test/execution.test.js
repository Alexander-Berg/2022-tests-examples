import { StatsManager } from "../src/StatsManager";
import { first } from "../src/utils";
import execution from "../src/stat_helpers/execution";

const s = new StatsManager();

const fibonacci = execution(
    s,
    function fib(n) {
        return n <= 1 ? n : fib(n - 1) + fib(n - 2);
    },
    "test"
);

beforeEach(() => {
    s.reset();
});

test("should calcuate execution time", () => {
    fibonacci(30);
    const [format, values] = s.getHistogram("test.time").getYasmFormat();
    expect(values.length).toEqual(1);
    expect(first(values) >= 10).toEqual(true);
});
