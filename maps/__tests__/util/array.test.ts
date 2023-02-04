import {array} from '../../util/array';

describe('util/array', () => {
    it('swap', () => {
        const arr = [1, 2, 3];

        array.swap(arr, 1, 2);
        expect(arr).toEqual([1, 3, 2]);

        array.swap(arr, 1, 1);
        expect(arr).toEqual([1, 3, 2]);
    });
});
