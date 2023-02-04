import { updatePropByOfferId } from '../update-offer-prop';

describe('updatePropByOfferId', () => {
    it('should update existing prop', () => {
        const offers = [ {
            id: '1',
            photo: 'a'
        }, {
            id: '2',
            photo: 'b'
        } ];

        const result = offers.map(updatePropByOfferId('2', 'photo', 'z'));

        expect(result).toEqual([ {
            id: '1',
            photo: 'a'
        }, {
            id: '2',
            photo: 'z'
        } ]);
    });

    it('should add new prop', () => {
        const offers = [ {
            id: '1',
            photo: 'a'
        }, {
            id: '2'
        } ];

        const result = offers.map(updatePropByOfferId('2', 'photo', 'z'));

        expect(result).toEqual([ {
            id: '1',
            photo: 'a'
        }, {
            id: '2',
            photo: 'z'
        } ]);
    });

    it('should not update anything if suitable offer not found', () => {
        const offers = [ {
            id: '1',
            photo: 'a'
        }, {
            id: '2',
            photo: 'b'
        } ];

        const result = offers.map(updatePropByOfferId('3', 'photo', 'z'));

        expect(result).toEqual(offers);
    });
});
