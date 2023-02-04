import { generateCombinations } from './generateCombinations';

const TEST_PARAMS_LIST = ['rgid', 'type', 'category', 'roomsTotal', 'newFlat'];

it('генерирует все комбинации длиной 1', () => {
    expect(generateCombinations(TEST_PARAMS_LIST, 1)).toEqual([
        ['rgid'],
        ['type'],
        ['category'],
        ['roomsTotal'],
        ['newFlat'],
    ]);
});

it('генерирует все комбинации длиной 2', () => {
    expect(generateCombinations(TEST_PARAMS_LIST, 2)).toEqual([
        ['rgid', 'type'],
        ['rgid', 'category'],
        ['rgid', 'roomsTotal'],
        ['rgid', 'newFlat'],
        ['type', 'category'],
        ['type', 'roomsTotal'],
        ['type', 'newFlat'],
        ['category', 'roomsTotal'],
        ['category', 'newFlat'],
        ['roomsTotal', 'newFlat'],
    ]);
});

it('генерирует все комбинации длиной 3', () => {
    expect(generateCombinations(TEST_PARAMS_LIST, 3)).toEqual([
        ['rgid', 'type', 'category'],
        ['rgid', 'type', 'roomsTotal'],
        ['rgid', 'type', 'newFlat'],
        ['rgid', 'category', 'roomsTotal'],
        ['rgid', 'category', 'newFlat'],
        ['rgid', 'roomsTotal', 'newFlat'],
        ['type', 'category', 'roomsTotal'],
        ['type', 'category', 'newFlat'],
        ['type', 'roomsTotal', 'newFlat'],
        ['category', 'roomsTotal', 'newFlat'],
    ]);
});

it('генерирует все комбинации длиной 4', () => {
    expect(generateCombinations(TEST_PARAMS_LIST, 4)).toEqual([
        ['rgid', 'type', 'category', 'roomsTotal'],
        ['rgid', 'type', 'category', 'newFlat'],
        ['rgid', 'type', 'roomsTotal', 'newFlat'],
        ['rgid', 'category', 'roomsTotal', 'newFlat'],
        ['type', 'category', 'roomsTotal', 'newFlat'],
    ]);
});
