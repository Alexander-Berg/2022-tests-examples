jest.mock('auto-core/react/lib/gateApi', () => {
    return {
        getResourcePublicApi: jest.fn(() => Promise.resolve([])),
    };
});

const changeOutputType = require('./changeOutputType');
const mockStore = require('autoru-frontend/mocks/mockStore').default;

const getResourcePublicApiMock = require('auto-core/react/lib/gateApi').getResourcePublicApi;

const store = mockStore({
    listing: {
        data: {
            search_parameters: {},
        },
    },
});

describe('вызовет getResource с нужными параметрами', () => {
    it('когда прокидываем кастомный метод', () => {
        store.dispatch(changeOutputType('list', 'random_method'));
        expect(getResourcePublicApiMock).toHaveBeenCalledWith('random_method', {});
    });

    it('когда не прокидываем метод', () => {
        store.dispatch(changeOutputType('list'));
        expect(getResourcePublicApiMock).toHaveBeenCalledWith('listing', {});
    });
});
