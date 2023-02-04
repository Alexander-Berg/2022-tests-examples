const reducer = require('./reducer');

it('should return same state for unknown event', () => {
    const state = {};
    const nextState = reducer(state, { type: '@UNKNOWN_ACTION' });
    expect(nextState).toEqual(state);
});

describe('FIND_DEALERS_SUCCESS', () => {
    it('вернет старый стейт если карта двигается', () => {
        const state = {
            timestamp: 0,
            dealers: [
                { dealerId: '1', phones: { list: [ '111' ], isFetching: false } },
            ],
            shownPhones: [ '1' ],
            isMapMoving: true,
            isFetching: false,
        };
        const action = {
            type: 'FIND_DEALERS_SUCCESS',
            meta: {
                timestamp: 1,
            },
            payload: {
                dealersListing: {
                    dealers: [
                        { dealerId: '4', phones: { list: [], isFetching: false } },
                    ],
                },
            },
        };

        const nextState = reducer(state, action);

        expect(nextState).toEqual(state);
    });

    it('вернет старый стейт если это старый запрос', () => {
        const state = {
            timestamp: 2,
            dealers: [
                { dealerId: '1', phones: { list: [ '111' ], isFetching: false } },
            ],
            shownPhones: [ '1' ],
            isFetching: false,
        };
        const action = {
            type: 'FIND_DEALERS_SUCCESS',
            meta: {
                timestamp: 1,
            },
            payload: {
                dealersListing: {
                    dealers: [
                        { dealerId: '4', phones: { list: [], isFetching: false } },
                    ],
                },
            },
        };

        const nextState = reducer(state, action);

        expect(nextState).toEqual(state);
    });

    it('не будет трогать телефоны у записей где они уже были запрошены', () => {
        const state = {
            timestamp: 0,
            dealers: [
                { dealerId: '1', phones: { list: [ '111' ], isFetching: false } },
                { dealerId: '2', phones: { list: [ '222' ], isFetching: false } },
            ],
            shownPhones: [ '1', '2' ],
        };
        const action = {
            type: 'FIND_DEALERS_SUCCESS',
            meta: {
                timestamp: 1,
            },
            payload: {
                dealersListing: {
                    dealers: [
                        { dealerId: '1', phones: { list: [ '111' ], isFetching: false } },
                        { dealerId: '3', phones: { list: [], isFetching: false } },
                        { dealerId: '4', phones: { list: [], isFetching: false } },
                    ],
                },
            },
        };

        const nextState = reducer(state, action);
        const phones = nextState.dealers.map(({ phones }) => phones.list[0]);

        expect(phones).toEqual([ '111', undefined, undefined ]);
        expect(nextState.shownPhones).toEqual([ '1' ]);
    });

    it('не упадет если в экшене пришел хуй', () => {
        const state = {
            timestamp: 0,
            dealers: [
                { dealerId: '1', phones: { list: [ '111' ], isFetching: false } },
            ],
            shownPhones: [ '1' ],
        };
        const action = {
            type: 'FIND_DEALERS_SUCCESS',
            meta: {
                timestamp: 1,
            },
            payload: {
                dealersListing: {
                    hui: 'sam takoi',
                },
            },
        };

        const nextState = reducer(state, action);

        expect(nextState.dealers).toEqual([ ]);
        expect(nextState.shownPhones).toEqual([ ]);
    });
});
