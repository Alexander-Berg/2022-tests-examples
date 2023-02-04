jest.mock('auto-core/react/lib/gateApi', () => {
    return {
        getResource: jest.fn(),
    };
});

const getTags = require('./getTags');
const gateApi = require('auto-core/react/lib/gateApi');

const mockStore = require('autoru-frontend/mocks/mockStore').default;

beforeEach(() => {
    gateApi.getResource.mockImplementation(() => Promise.resolve({
        suggested: [
            {
                value: 'кредит',
            },
            {
                value: 'трейдин',
            },
        ],
    }));
});

it('должен вызывать ресурс с префиксом в параметрах', () => {
    expect.assertions(1);

    const store = mockStore({});

    store.dispatch(
        getTags('кре'),
    );

    expect(gateApi.getResource).toHaveBeenCalledWith('getCallTags', { dealer_id: undefined, prefix: 'кре' });
});
