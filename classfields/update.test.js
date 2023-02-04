const update = require('./update');
const mockStore = require('autoru-frontend/mocks/mockStore').default;
jest.mock('auto-core/react/lib/gateApi', () => ({ getResource: jest.fn() }));

it('должен вызывать экшены изменения настройки', async() => {
    const store = mockStore({});
    const getResource = require('auto-core/react/lib/gateApi').getResource;
    getResource.mockImplementation(() => Promise.resolve({ status: 'SUCCESS' }));

    await store.dispatch(
        update({
            clientId: 16453,
            name: 'auto_activate_moto_offers',
            value: true,
        }),
    );

    expect(store.getActions()).toMatchSnapshot();
});

it('должен показать ошибку, если что-то пошло не так', async() => {
    const store = mockStore({});
    const getResource = require('auto-core/react/lib/gateApi').getResource;
    getResource.mockImplementation(() => Promise.resolve({ status: 'FAIL' }));

    await store.dispatch(
        update({
            clientId: 16453,
            name: 'auto_activate_moto_offers',
            value: true,
        }),
    );

    expect(store.getActions()).toMatchSnapshot();
});
