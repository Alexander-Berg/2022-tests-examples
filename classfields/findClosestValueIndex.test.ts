import { findClosestValueIndex } from './findClosestValueIndex';

describe('findClosestValueIndex', () => {
    it('находит индекс ближайшего элемента', () => {

        const index = findClosestValueIndex([
            { value: 10 },
            { value: 20 },
            { value: 30 },
            { value: 40 },
        ], 24);

        expect(index).toBe(1);
    }) ;

    it('должен работать с нулем', () => {
        const index = findClosestValueIndex([
            { value: 0 },
            { value: 20 },
            { value: 30 },
            { value: 40 },
        ], 0);

        expect(index).toBe(0);
    }) ;
});
