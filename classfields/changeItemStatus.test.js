/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

jest.mock('auto-core/react/lib/gateApi');
const mockStore = require('autoru-frontend/mocks/mockStore').default;
const changeItemStatus = require('./changeItemStatus');

it('должен дождаться ответа от сервера, удалить элемент из списка ' +
    'и показать нотификашку об успешном выполнении операции', () => {
    const store = mockStore();
    const gateApi = require('auto-core/react/lib/gateApi');
    gateApi.getResource = () => Promise.resolve(true);

    return store.dispatch(changeItemStatus({ client_id: 'clientId', status: 'active' }))
        .then(() => {
            expect(store.getActions()).toMatchSnapshot();
        });
});

it('должен вызвать action показа ошибки, если что пошло не так', () => {
    const gateApi = require('auto-core/react/lib/gateApi');
    const store = mockStore();
    gateApi.getResource = () => Promise.reject();

    return store.dispatch(changeItemStatus({ client_id: 'clientId', status: 'active' }))
        .then(() => {
            expect(store.getActions()).toEqual([
                {
                    payload: {
                        message: 'Произошла ошибка, попробуйте ещё раз',
                        view: 'error',
                    },
                    type: 'NOTIFIER_SHOW_MESSAGE',
                },
            ]);
        });
});
