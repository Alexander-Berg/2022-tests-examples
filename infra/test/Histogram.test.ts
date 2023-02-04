import {Histogram} from '../src/Histogram';

test('add only numbers', () => {
    const h = new Histogram();
    expect(h.getYasmFormat()).toEqual(['hhhh', []]);

    h.add(42);
    expect(h.getYasmFormat()).toEqual(['hhhh', [42]]);

    h.add(null, undefined, Infinity, -Infinity, NaN);
    expect(h.getYasmFormat()).toEqual(['hhhh', [42]]);
});
