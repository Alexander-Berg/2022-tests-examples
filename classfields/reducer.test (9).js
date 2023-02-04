const reducer = require('./reducer');

const {
    LISTING_LOCATOR_PHONES_PENDING,
    LISTING_LOCATOR_PHONES_REJECTED,
    LISTING_LOCATOR_PHONES_RESOLVED,
} = require('../offerPhones/actionTypes');

const mockOffer = {
    seller: { phones: [ { phone: '+7 999 999 99 99' } ], redirect_phones: [], phones_pending: false },
};

describe('передается id несуществующего оффера', () => {
    const state = {
        data: [
            {
                radius: 300,
                count: 2,
                offers: [ { ...mockOffer, id: '1', hash: '1' }, { ...mockOffer, id: '1', hash: '2' } ],
            },
            {
                radius: 500,
                count: 2,
                offers: [ { ...mockOffer, id: '2', hash: '1' }, { ...mockOffer, id: '2', hash: '2' } ],
            },
        ],
    };

    it('pending', () => {
        const action = {
            type: LISTING_LOCATOR_PHONES_PENDING,
            payload: { offerIdHash: '3-1' },
        };

        expect(reducer(state, action)).toEqual(state);
    });

    it('reject', () => {
        const action = {
            type: LISTING_LOCATOR_PHONES_REJECTED,
            payload: { offerIdHash: '3-1' },
        };

        expect(reducer(state, action)).toEqual(state);
    });

    it('resolve', () => {
        const action = {
            type: LISTING_LOCATOR_PHONES_RESOLVED,
            payload: { offerIdHash: '3-1', phones: [], redirectPhones: [] },
        };

        expect(reducer(state, action)).toEqual(state);
    });
});

describe('передается id существующего оффера', () => {
    let state;
    beforeEach(() => {
        state = {
            data: [
                {
                    radius: 300,
                    count: 2,
                    offers: [ { ...mockOffer, id: '1', hash: '1' }, { ...mockOffer, id: '1', hash: '2' } ],
                },
                {
                    radius: 500,
                    count: 2,
                    offers: [ { ...mockOffer, id: '2', hash: '1' }, { ...mockOffer, id: '2', hash: '2' } ],
                },
            ],
        };
    });

    it('pending', () => {
        const action = {
            type: LISTING_LOCATOR_PHONES_PENDING,
            payload: { offerIdHash: '2-1' },
        };

        const newState = reducer(state, action);
        expect(newState.data[1].offers[0].seller.phones_pending).toEqual(true);
    });

    it('reject', () => {
        state.data[1].offers[0].seller.phones_pending = true;
        const action = {
            type: LISTING_LOCATOR_PHONES_REJECTED,
            payload: { offerIdHash: '2-1' },
        };

        const newState = reducer(state, action);
        expect(newState.data[1].offers[0].seller.phones_pending).toEqual(false);
    });

    it('resolve', () => {
        state.data[1].offers[0].seller.phones_pending = true;
        const action = {
            type: LISTING_LOCATOR_PHONES_RESOLVED,
            payload: { offerIdHash: '2-1', phones: [ { phone: 'newPhone' } ], redirect_phones: [ { phone: 'newRedirectPhone' } ] },
        };

        const newState = reducer(state, action);
        expect(newState.data[1].offers[0].seller.phones_pending).toEqual(false);
        expect(newState.data[1].offers[0].seller.phones).toEqual([ { phone: 'newPhone' } ]);
        expect(newState.data[1].offers[0].seller.redirect_phones).toEqual([ { phone: 'newRedirectPhone' } ]);
    });
});
