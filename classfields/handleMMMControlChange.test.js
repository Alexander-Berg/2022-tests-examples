jest.mock('auto-core/react/lib/gateApi', () => {
    return {
        getResource: () => Promise.resolve([]),
    };
});

const handleMMMControlChange = require('./handleMMMControlChange');
const mockStore = require('autoru-frontend/mocks/mockStore').default;

let store;

it('должен добавить МММ', () => {
    store = mockStore({
        listing: {
            data: {
                search_parameters: {},
            },
        },
    });

    const result = store.dispatch(handleMMMControlChange([ 'AUDI' ], { name: 'mark' }, 0));
    expect(result).toEqual([ { mark: 'AUDI', models: [] } ]);
});
