import { moveTo, getChangedItem } from '../array';

describe('moveTo', () => {
    const arr = [ 1, 2, 3, 4, 5 ];

    it('moves elem to index 0', () => {
        const result = moveTo(arr, 5, 0);

        expect(result).toEqual([ 5, 1, 2, 3, 4 ]);
    });

    it('moves elem the last position', () => {
        const result = moveTo(arr, 2, 4);

        expect(result).toEqual([ 1, 3, 4, 5, 2 ]);
    });

    it('moves elem to the passed index', () => {
        const result = moveTo(arr, 2, 3);

        expect(result).toEqual([ 1, 3, 4, 2, 5 ]);
    });

    it('saves the same array if pass index equals to an index of elem', () => {
        const result = moveTo(arr, 2, 1);

        expect(result).toEqual([ 1, 2, 3, 4, 5 ]);
    });

    it('returns passed array if passed index is greater then length of array', () => {
        const result = moveTo(arr, 2, 5);

        expect(result).toEqual([ 1, 2, 3, 4, 5 ]);
    });
});

describe('getChangedItem', () => {
    it('determines added item', () => {
        expect(getChangedItem(
            [ 'a', 'b', 'c' ],
            [ 'a', 'b', 'c', 'd' ]
        )).toEqual({
            item: 'd',
            isAdded: true
        });
    });

    it('determines falsy added items', () => {
        expect(getChangedItem(
            [ 1, 2, 3 ],
            [ 0, 1, 2, 3 ]
        )).toEqual({
            item: 0,
            isAdded: true
        });

        expect(getChangedItem(
            [ 1, 2, 3 ],
            [ undefined, 1, 2, 3 ]
        )).toEqual({
            item: undefined,
            isAdded: true
        });

        expect(getChangedItem(
            [ 1, 2, 3 ],
            [ null, 1, 2, 3 ]
        )).toEqual({
            item: null,
            isAdded: true
        });
    });

    it('determines removed item', () => {
        expect(getChangedItem(
            [ 'a', 'b', 'c', 'd' ],
            [ 'a', 'c', 'd' ]
        )).toEqual({
            item: 'b',
            isAdded: false
        });
    });

    it('returns nothing if nothing was changed', () => {
        expect(getChangedItem(
            [ 'a', 'b', 'c' ],
            [ 'a', 'b', 'c' ]
        )).toEqual();
    });

    it('warns if there are more than 1 changed item', () => {
        const spyWarn = jest.spyOn(console, 'warn').mockImplementation(() => {});

        getChangedItem(
            [ 'a', 'b', 'c', 'd', 'e' ],
            [ 'a', 'b' ]
        );

        expect(spyWarn).toBeCalledWith('getChangedItem: found 3 changed items, returning only the first one');

        spyWarn.mockClear();

        getChangedItem(
            [ 'a', 'b', 'c' ],
            [ 'a', 'b', 'd' ]
        );

        expect(spyWarn).toBeCalledWith('getChangedItem: found 2 changed items, returning only the first one');

        spyWarn.mockRestore();
    });
});
