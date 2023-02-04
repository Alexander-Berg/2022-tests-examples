import { StatsManager } from "../src/StatsManager";
import { first } from "../src/utils";
import interval from "../src/stat_helpers/interval";

const s = new StatsManager();

class A {
    constructor() {
        this.run = interval(s, this.run, "test");
    }

    run() {
        /*empty*/
    }
}

class B extends A {}

beforeEach(() => {
    s.reset();
});

it("should be empty on just one call", () => {
    const a = new A();
    a.run();

    expect(s.getYasmSignals()).toEqual([]);
});

it("should correct calculate iterval", done => {
    const a = new A();
    a.run();

    setTimeout(() => {
        a.run();
        const [format, values] = s.getHistogram("test.time").getYasmFormat();
        expect(values.length).toEqual(1);
        expect(first(values) >= 500).toEqual(true);
        done();
    }, 500);
}, 10000);

it("should correct calculate iterval with inheritance call", done => {
    const a = new A();
    const b = new B();

    a.run();
    b.run();

    setTimeout(() => {
        a.run();
        b.run();

        const [format, values] = s.getHistogram("test.time").getYasmFormat();
        expect(values.length).toEqual(2);
        expect(values.every(value => value >= 500)).toEqual(true);
        done();
    }, 500);
}, 10000);
