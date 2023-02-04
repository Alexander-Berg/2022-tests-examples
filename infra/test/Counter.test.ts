import {Counter} from '../src/Counter';

test('default values', () => {
    const c = new Counter();
    expect(c.getYasmFormat()).toEqual(['summ', 0]);
});

test('aggregation type', () => {
    const c = new Counter('mmmm');
    expect(c.getYasmFormat()).toEqual(['mmmm', 0]);
});

test('add numbers', () => {
    const c = new Counter();

    c.add(41);
    expect(c.getYasmFormat()).toEqual(['summ', 41]);

    c.add(null, undefined, NaN, Infinity, -Infinity, 1);
    expect(c.getYasmFormat()).toEqual(['summ', 42]);
});
