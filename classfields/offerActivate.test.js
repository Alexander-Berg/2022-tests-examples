jest.mock('auto-core/react/lib/gateApi', () => ({
    getResource: jest.fn(),
}));
jest.mock('auto-core/react/dataDomain/notifier/actions/notifier');

const offerActivate = require('./offerActivate');

const mockStore = require('autoru-frontend/mocks/mockStore').default;

const getResource = require('auto-core/react/lib/gateApi').getResource;
const { showAutoclosableMessage } = require('auto-core/react/dataDomain/notifier/actions/notifier');

const store = mockStore({
    user: { data: { profile: { autoru: { client_id: 777 } } } },
});

it('покажет правильную нотифайку, если не хватает денег и юзер - дилер', () => {
    const pr = Promise.resolve({ error: 'PAYMENT_NEEDED' });
    getResource.mockImplementation(jest.fn(() => pr));

    const showAutoclosableMessageMock = jest.fn(() => () => {});
    showAutoclosableMessage.mockImplementation(showAutoclosableMessageMock);

    const actionParams = {
        category: 'cars',
        from: 'card-vas',
        offerIdHash: '1101938134-8a2ge95b',
        returnUrl: 'ya.ru',
    };

    return store.dispatch(
        offerActivate(actionParams),
    )
        .then(() => {
            expect(showAutoclosableMessageMock).toHaveBeenCalledWith({
                message: 'Недостаточно средств для оплаты. Пополните кошелёк.',
                view: 'error',
            });
        });
});
