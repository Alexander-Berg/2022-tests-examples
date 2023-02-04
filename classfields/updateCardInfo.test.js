jest.mock('auto-core/react/lib/gateApi', () => {
    return {
        getResource: jest.fn(() => Promise.resolve()),
    };
});

const getResource = require('auto-core/react/lib/gateApi').getResource;
const actionTypes = require('../actionTypes');

const mockStore = require('autoru-frontend/mocks/mockStore').default;
const updateCardInfo = require('./updateCardInfo');

const params = {
    card_id: '123456|4321',
    preferred: true,
    payment_system_id: 'yandexkassa_v3',
};

let store;

beforeEach(() => {
    store = mockStore();
});

it('вызовет правильный ресурс с переданными параметрами', () => {
    store.dispatch(updateCardInfo(params));
    expect(getResource).toHaveBeenCalledTimes(1);
    expect(getResource).toHaveBeenCalledWith('updateCardInfoWithBillingFormat', params);
});

it('при успешном ответе создаст экшн "BILLING_UPDATE_TIED_CARD" и передаст в него результат', () => {
    const response = { foo: 'bar' };
    const gateApiPromise = Promise.resolve(response);
    getResource.mockImplementation(() => gateApiPromise);
    const expectedAction = { type: actionTypes.BILLING_UPDATE_TIED_CARD, payload: response };

    store.dispatch(updateCardInfo(params));

    return gateApiPromise
        .then(() => {
            expect(store.getActions()).toEqual(expect.arrayContaining([ expect.objectContaining(expectedAction) ]));
        });
});
