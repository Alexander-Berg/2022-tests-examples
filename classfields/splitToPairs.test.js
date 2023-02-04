const splitToPairs = require('./splitToPairs');

describe('splitToPairs', () => {
    it('разбивает массив на пары (нечетное количество)', () => {
        const items = [ 1, 2, 3, 4, 5 ];
        const expected = [ [ 1, 2 ], [ 3, 4 ], [ 5, undefined ] ];
        expect(splitToPairs(items)).toEqual(expected);
    });

    it('разбивает массив на пары (нечетное количество, эдж кейс)', () => {
        const items = [ 1 ];
        const expected = [ [ 1, undefined ] ];
        expect(splitToPairs(items)).toEqual(expected);
    });

    it('разбивает массив на пары (четное количество)', () => {
        const items = [ 1, 2 ];
        const expected = [ [ 1, 2 ] ];
        expect(splitToPairs(items)).toEqual(expected);
    });

    it('разбивает массив на пары (четное количество, но побольше)', () => {
        const items = [ 1, 2, 3, 4 ];
        const expected = [ [ 1, 2 ], [ 3, 4 ] ];
        expect(splitToPairs(items)).toEqual(expected);
    });

    it('не меняет пустой массив', () => {
        const items = [];
        const expected = [];
        expect(splitToPairs(items)).toEqual(expected);
    });
});
