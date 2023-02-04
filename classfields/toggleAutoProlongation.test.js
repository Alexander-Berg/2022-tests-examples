jest.mock('auto-core/react/lib/gateApi', () => {
    return {
        getResource: jest.fn(),
    };
});
jest.mock('./updateOffer');
jest.mock('auto-core/react/dataDomain/notifier/actions/notifier');

const _ = require('lodash');
const toggleAutoProlongation = require('./toggleAutoProlongation');

const mockStore = require('autoru-frontend/mocks/mockStore').default;
const offerMock = require('auto-core/react/dataDomain/card/mocks/card.cars.mock');

const getResource = require('auto-core/react/lib/gateApi').getResource;
const updateOffer = require('./updateOffer');
const { showAutoclosableMessage } = require('auto-core/react/dataDomain/notifier/actions/notifier');

const product = 'all_sale_toplist';

let store;
beforeEach(() => {
    store = mockStore({
        card: _.cloneDeep(offerMock),
    });
});

it('вызовет правильный ресурс с правильными параметрами при включении автопродления', () => {
    const gateApiMock = jest.fn(() => Promise.resolve({ status: 'SUCCESS' }));
    getResource.mockImplementation(gateApiMock);
    showAutoclosableMessage.mockImplementationOnce(() => () => { });
    updateOffer.mockImplementationOnce(jest.fn(() => () => { }));

    store.dispatch(toggleAutoProlongation(true, { product }));

    expect(gateApiMock).toHaveBeenCalledTimes(1);
    expect(getResource).toHaveBeenCalledWith('addAutoProlong', {
        domain: 'autoru',
        category: 'cars',
        offerId: offerMock.saleId,
        product,
    });
});

it('вызовет правильный ресурс с правильными параметрами при выключении автопродления', () => {
    const gateApiMock = jest.fn(() => Promise.resolve({ status: 'SUCCESS' }));
    getResource.mockImplementation(gateApiMock);
    showAutoclosableMessage.mockImplementationOnce(() => () => { });
    updateOffer.mockImplementationOnce(jest.fn(() => () => { }));

    store.dispatch(toggleAutoProlongation(false, { product }));

    expect(gateApiMock).toHaveBeenCalledTimes(1);
    expect(getResource).toHaveBeenCalledWith('deleteAutoProlong', {
        domain: 'autoru',
        category: 'cars',
        offerId: offerMock.saleId,
        product,
    });
});

it('оптимистично проставит флаг недожидаюсь ответа бэка', () => {
    const gateApiMock = jest.fn(() => Promise.resolve({ status: 'SUCCESS' }));
    getResource.mockImplementation(gateApiMock);
    showAutoclosableMessage.mockImplementationOnce(() => () => { });

    store.dispatch(toggleAutoProlongation(true, { product }));

    expect(store.getActions()).toMatchSnapshot();
});

it('если запрос удачный то после покажет нотификацию', () => {
    const pr = Promise.resolve({ status: 'SUCCESS' });
    const showAutoclosableMessageMock = jest.fn(() => () => { });
    showAutoclosableMessageMock.mockClear();
    getResource.mockImplementation(jest.fn(() => pr));
    showAutoclosableMessage.mockImplementationOnce(showAutoclosableMessageMock);
    updateOffer.mockImplementation(jest.fn(() => () => { }));

    store.dispatch(toggleAutoProlongation(true, { product }));

    return pr.then(() => {
        expect(showAutoclosableMessageMock).toHaveBeenCalledTimes(1);
        expect(showAutoclosableMessageMock).toHaveBeenCalledWith({ message: 'Автопродление включено', view: 'success' });
    });
});

it('если запрос неудачный то после покажет нотификацию и вернет стейт обратно', () => {
    const pr = Promise.reject();
    getResource.mockImplementation(jest.fn(() => pr));

    const showAutoclosableMessageMock = jest.fn(() => () => { });
    showAutoclosableMessage.mockImplementation(showAutoclosableMessageMock);

    store.dispatch(toggleAutoProlongation(true, { product }));

    return pr.then(
        () => Promise.reject('UNEXPECTED_RESOLVE'),
        async() => {
            await new Promise((resolve) => setTimeout(resolve));
            expect(showAutoclosableMessageMock).toHaveBeenCalledTimes(1);
            expect(showAutoclosableMessageMock).toHaveBeenCalledWith({ message: 'Произошла ошибка', view: 'error' });

            expect(store.getActions()).toMatchSnapshot();
        },
    );
});

it('если запрос неудачный и ошибка должна торчать наружу, вернет реджекнутый промис', () => {
    const pr = Promise.reject();
    getResource.mockImplementation(jest.fn(() => pr));

    const showAutoclosableMessageMock = jest.fn(() => () => { });
    showAutoclosableMessage.mockImplementation(showAutoclosableMessageMock);

    return expect(store.dispatch(toggleAutoProlongation(true, { product }, { exposeError: true }))).rejects.toEqual();
});
