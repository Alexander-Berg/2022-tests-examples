const comparePositionsForSortFunction = require('./comparePositionsForSortFunction');

describe('все -1 в конец массива', () => {
    it('сортирует обычный массив', () => {
        const data = [ -1, 1, -1, -1 ];
        const result = data.sort(comparePositionsForSortFunction);
        expect(result).toEqual([ 1, -1, -1, -1 ]);
    });
    it('сортирует массив из -1', () => {
        const data = [ -1, -1, -1, -1 ];
        const result = data.sort(comparePositionsForSortFunction);
        expect(result).toEqual([ -1, -1, -1, -1 ]);
    });
    it('сортирует массив без -1', () => {
        const data = [ 1, 5, 2, 4 ];
        const result = data.sort(comparePositionsForSortFunction);
        expect(result).toEqual([ 1, 2, 4, 5 ]);
    });
    it('сортирует пустой массив', () => {
        const data = [];
        const result = data.sort(comparePositionsForSortFunction);
        expect(result).toEqual([]);
    });
    it('просто на всякий случай 1', () => {
        const data = [ 0, -1, 0, 0 ];
        const result = data.sort(comparePositionsForSortFunction);
        expect(result).toEqual([ 0, 0, 0, -1 ]);
    });
    it('просто на всякий случай 2', () => {
        const data = [ -1, -1, 0, 0, 500 ];
        const result = data.sort(comparePositionsForSortFunction);
        expect(result).toEqual([ 0, 0, 500, -1, -1 ]);
    });
});
