import { StatsManager } from "../src/StatsManager";
import { first } from "../src/utils";
import promise from "../src/stat_helpers/promise";

const s = new StatsManager();

class A {
    constructor() {
        this.run = promise(s, this.run, "test");
        this.execute = promise(s, this.execute, "test");
    }

    run() {
        return new Promise(resolve => {
            setTimeout(resolve, 500);
        });
    }

    execute() {}
}

beforeEach(() => {
    s.reset();
});

it("should calcuate promise execution time", done => {
    const a = new A();

    a.run().then(() => {
        const [format, values] = s.getHistogram("test.time").getYasmFormat();
        expect(values.length).toEqual(1);
        expect(first(values) >= 500).toEqual(true);
        done();
    });
});

it("should ignore incorrect behavior", () => {
    const a = new A();
    a.execute();
    expect(s.getYasmSignals()).toEqual([]);
});
