const splitItemsIntoColumns = require('./splitItemsIntoColumns');

describe('splitItemsIntoColumns tests', () => {
    it('should returns correct array if items = []', () => {
        expect(splitItemsIntoColumns([])).toEqual([]);
    });

    it('should returns correct array if n = 1', () => {
        expect(splitItemsIntoColumns([ 1, 2, 3, 4, 5 ], 1)).toEqual([ [ 1, 2, 3, 4, 5 ] ]);
    });

    // 4 колонки - популярное значение на сайте
    it('should returns correct array if n = 4', () => {
        //  1    3    4    5
        //  2
        expect(splitItemsIntoColumns([ 1, 2, 3, 4, 5 ], 4)).toEqual([ [ 1, 2 ], [ 3 ], [ 4 ], [ 5 ] ]);
        //  1    3    5    6
        //  2    4
        expect(splitItemsIntoColumns([ 1, 2, 3, 4, 5, 6 ], 4)).toEqual([ [ 1, 2 ], [ 3, 4 ], [ 5 ], [ 6 ] ]);
        //  1    3    5    7
        //  2    4    6
        expect(splitItemsIntoColumns([ 1, 2, 3, 4, 5, 6, 7 ], 4)).toEqual([ [ 1, 2 ], [ 3, 4 ], [ 5, 6 ], [ 7 ] ]);
        //  1    3    5    7
        //  2    4    6    8
        expect(splitItemsIntoColumns([ 1, 2, 3, 4, 5, 6, 7, 8 ], 4)).toEqual([ [ 1, 2 ], [ 3, 4 ], [ 5, 6 ], [ 7, 8 ] ]);
    });
});
