const rotate = require('./rotate');

describe('rotate перемешивает элементы чтобы выстроить их в колонки', () => {
    it('корректно для пустого массива', () => {
        const items = [];
        const expected = [];
        expect(rotate(items)).toEqual(expected);
    });

    it('корректно для массива из 1 элемента', () => {
        const items = [ 1 ];
        const expected = [ 1 ];
        expect(rotate(items)).toEqual(expected);
    });

    it('корректно для массива из 2 элементов', () => {
        const items = [ 1, 2 ];
        const expected = [ 1, 2 ];
        expect(rotate(items)).toEqual(expected);
    });

    it('корректно для массива из 3 элементов', () => {
        const items = [ 1, 2, 3 ];
        const expected = [ 1, 3, 2 ];
        expect(rotate(items)).toEqual(expected);
    });

    it('корректно для массива из 4 элементов', () => {
        const items = [ 1, 2, 3, 4 ];
        const expected = [ 1, 3, 2, 4 ];
        expect(rotate(items)).toEqual(expected);
    });

    it('корректно для массива из 5 элементов', () => {
        const items = [ 1, 2, 3, 4, 5 ];
        const expected = [ 1, 4, 2, 5, 3 ];
        expect(rotate(items)).toEqual(expected);
    });
});
