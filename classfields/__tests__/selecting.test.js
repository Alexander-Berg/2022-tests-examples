import { agencyOffer } from 'view/__fixtures__/offer';
import reducer from '../selecting';

const secondAgencyOffer = {
    ...agencyOffer,
    id: `${agencyOffer.id}-2`
};

const emptyLoadedState = {
    status: 'loaded',
    loadingMoreStatus: 'loaded',
    offers: [ agencyOffer, secondAgencyOffer ],
    counts: {},
    feedsList: [],
    availableCurrencies: [],
    pager: {
        page: 1,
        pageSize: 20,
        totalPages: 1,
        totalItems: 0
    }
};

describe('selecting reducers', () => {
    it('should do nothing if offers not loaded yet', () => {
        const initialState = { status: 'pending', offers: [], availableCurrencies: [] };
        const state = reducer(initialState, { type: 'offers-new/selecting/set', ids: [ '123' ], state: true });

        expect(state).toBe(initialState);
    });

    it('should do nothing if status is error', () => {
        const initialState = { status: 'errored', error: 'Error!', availableCurrencies: [] };
        const state = reducer(initialState, { type: 'offers-new/selecting/set', ids: [ '123' ], state: true });

        expect(state).toBe(initialState);
    });

    it('should select offer', () => {
        const initialState = emptyLoadedState;
        const state = reducer(initialState, { type: 'offers-new/selecting/set', ids: [ agencyOffer.id ], state: true });

        expect(state).toEqual({
            ...emptyLoadedState,
            offers: [ { ...agencyOffer, selected: true }, secondAgencyOffer ]
        });
    });

    it('should select multiple offers', () => {
        const initialState = emptyLoadedState;
        const state = reducer(initialState, {
            type: 'offers-new/selecting/set',
            ids: [ agencyOffer.id, secondAgencyOffer.id ],
            state: true
        });

        expect(state).toEqual({ ...emptyLoadedState, offers: [
            { ...agencyOffer, selected: true },
            { ...secondAgencyOffer, selected: true }
        ] });
    });

    it('should not unselect selected offers', () => {
        const initialState = { ...emptyLoadedState, offers: [
            { ...agencyOffer, selected: false },
            { ...secondAgencyOffer, selected: true }
        ] };
        const state = reducer(initialState, {
            type: 'offers-new/selecting/set',
            ids: [ agencyOffer.id, secondAgencyOffer.id ],
            state: true
        });

        expect(state).toEqual({ ...emptyLoadedState, offers: [
            { ...agencyOffer, selected: true },
            { ...secondAgencyOffer, selected: true }
        ] });
    });

    it('should unselect offer', () => {
        const initialState = { ...emptyLoadedState, offers: [
            { ...agencyOffer, selected: true },
            { ...secondAgencyOffer, selected: true }
        ] };
        const state = reducer(
            initialState,
            {
                type: 'offers-new/selecting/set',
                ids: [ agencyOffer.id ],
                state: false
            }
        );

        expect(state).toEqual({ ...emptyLoadedState, offers: [
            { ...agencyOffer, selected: false },
            { ...secondAgencyOffer, selected: true }
        ] });
    });
});
