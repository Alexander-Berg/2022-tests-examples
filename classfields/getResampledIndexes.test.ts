import getResampledIndexes from './getResampledIndexes';

it('возвращает неизмеменный набор индексов, если точек мало (меньше или равно 6) или не тот тип', () => {
    expect(getResampledIndexes(0, 6)).toEqual([]);
    expect(getResampledIndexes(1, 6)).toEqual([ 0 ]);
    expect(getResampledIndexes(6, 6)).toEqual([ 0, 1, 2, 3, 4, 5 ]);
});

it('правильно работает 6 -> 3', () => {
    const expected = [ 0, 1, 3, 4, 5 ];
    expect(getResampledIndexes(6, 3)).toEqual(expected);
});

it('правильно работает 6 -> 4', () => {
    const expected = [ 0, 1, 2, 3, 4, 5 ];
    expect(getResampledIndexes(6, 4)).toEqual(expected);
});

it('правильно работает 10 -> 6', () => {
    const expected = [ 0, 1, 3, 4, 6, 7, 8, 9 ];
    expect(getResampledIndexes(10, 6)).toEqual(expected);
});

it('правильно работает 10 -> 5', () => {
    const expected = [ 0, 1, 3, 5, 7, 8, 9 ];
    expect(getResampledIndexes(10, 5)).toEqual(expected);
});

it('правильно работает 14 -> 6', () => {
    const expected = [ 0, 1, 3, 6, 8, 11, 12, 13 ];
    expect(getResampledIndexes(14, 6)).toEqual(expected);
});

it('правильно работает 20 -> 6', () => {
    const expected = [ 0, 1, 5, 8, 12, 15, 18, 19 ];
    expect(getResampledIndexes(20, 6)).toEqual(expected);
});

it('правильно работает 100 -> 6', () => {
    const expected = [ 0, 1, 21, 40, 60, 79, 98, 99 ];
    expect(getResampledIndexes(100, 6)).toEqual(expected);
});
