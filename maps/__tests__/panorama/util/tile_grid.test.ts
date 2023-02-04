import {gridRange} from '../../../panorama/util/tile_grid';

describe('panorama utils', () => {
    it('gridRange', () => {
        expect([...gridRange(1, 3)]).toEqual([1, 2]);
        expect([...gridRange(1, 3.001)]).toEqual([1, 2, 3]);
        expect([...gridRange(0.99999, 3.001)]).toEqual([1, 2, 3]);
        expect([...gridRange(-3.5, -1.5)]).toEqual([-3, -2]);
        expect([...gridRange(-3.5, 1.5)]).toEqual([-3, -2, -1, 0, 1]);

        expect([...gridRange(2.5, 0.5)]).toEqual([2, 1]);
        expect([...gridRange(2.5, -1.5)]).toEqual([2, 1, 0, -1]);

        expect([...gridRange(1, 1)]).toEqual([]);
        expect([...gridRange(1, 1.0000001)]).toEqual([1]);
    });

});
