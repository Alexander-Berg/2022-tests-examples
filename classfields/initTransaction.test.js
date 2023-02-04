jest.mock('auto-core/react/lib/gateApi', () => {
    return {
        getResource: jest.fn(),
    };
});

const initTransaction = require('./initTransaction');

const getResource = require('auto-core/react/lib/gateApi').getResource;
const mockStore = require('autoru-frontend/mocks/mockStore').default;

const params = { foo: 'bar' };

let store;
const successfulResponse = Promise.resolve({ ticket_id: 'foo' });
const unsuccessfulResponse = Promise.resolve();
const failedResponse = Promise.reject();

beforeEach(() => {
    store = mockStore();
});

afterEach(() => {
    getResource.mockReset();
});

it('вызовет правильный ресурс с переданными параметрами', () => {
    getResource.mockImplementationOnce(() => successfulResponse);
    store.dispatch(initTransaction(params));

    return successfulResponse
        .then(() => {
            expect(getResource).toHaveBeenCalledTimes(1);
            expect(getResource).toHaveBeenCalledWith('initTransaction', params);
        });
});

it('выбросит ошибку если в ответе нет тикета', () => {
    getResource.mockImplementationOnce(() => unsuccessfulResponse);
    const actionPromise = store.dispatch(initTransaction(params));

    return actionPromise.then(
        () => Promise.reject('UNEXPECTED_RESOLVE'),
        () => {
            expect(store.getActions()).toEqual([]);
        },
    );
});

it('выбросит ошибку если в бэк не ответил', () => {
    getResource.mockImplementationOnce(() => failedResponse);
    const actionPromise = store.dispatch(initTransaction(params));

    return actionPromise.then(
        () => Promise.reject('UNEXPECTED_RESOLVE'),
        () => {
            expect(store.getActions()).toEqual([]);
        },
    );
});

describe('если в ответе бэка есть тикет', () => {
    it('создаст правильные экшены для стора', () => {
        getResource.mockImplementationOnce(() => successfulResponse);
        store.dispatch(initTransaction(params));

        return successfulResponse
            .then(() => {
                expect(store.getActions()).toMatchSnapshot();
            });
    });

    it('прокинет тикет дальше по цепочке промисов', () => {
        getResource.mockImplementationOnce(() => successfulResponse);
        const actionPromise = store.dispatch(initTransaction(params));

        return actionPromise
            .then((ticketId) => {
                expect(ticketId).toBe('foo');
            });
    });
});
