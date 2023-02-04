/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

import mockStore from 'autoru-frontend/mocks/mockStore';

import gateApi from 'auto-core/react/lib/gateApi';

import writeClassifiedSecret from './writeClassifiedSecret';

it('должен вызвать writeMultipostingSecret c корретными параметрами ' +
    'и задиспатчить корректный набор actions', () => {
    (gateApi.getResource as any) = jest.fn(() => Promise.resolve({ status: 'SUCCESS' }));
    const classifiedSecret = {
        avito_client_id: '4',
        avito_client_secret: '4',
        avito_user_id: '4',
        login: undefined,
    };
    const store = mockStore({
        config: {
            client: {
                id: 20101,
            },
        },
    });

    return (store as any).dispatch(writeClassifiedSecret('avito', classifiedSecret))
        .then(() => {
            expect(gateApi.getResource).toHaveBeenCalledWith(
                'writeMultipostingSecret',
                {
                    classified: 'avito',
                    body: classifiedSecret,
                    dealer_id: 20101,
                });

            expect(store.getActions()).toEqual([
                { type: 'WRITE_CLASSIFIED_SECRET_PENDING' },
                {
                    payload: {
                        classified: 'avito',
                        classifiedSecret: {
                            avito_client_id: '4',
                            avito_client_secret: '4',
                            avito_user_id: '4',
                            login: undefined,
                        },
                    },
                    type: 'WRITE_CLASSIFIED_SECRET_RESOLVED',
                },
                {
                    payload: {
                        message: 'Настройки сохранены',
                        view: 'success',
                    },
                    type: 'NOTIFIER_SHOW_MESSAGE',
                },
            ]);
        });
});

it('должен задиспатчить корректный набор actions, если что-то пошло не так', () => {
    (gateApi.getResource as any) = jest.fn(() => Promise.reject());
    const classifiedSecret = {
        avito_client_id: '4',
        avito_client_secret: '4',
        avito_user_id: '4',
        login: undefined,
    };
    const store = mockStore({
        config: {
            client: {
                id: 20101,
            },
        },
    });

    return (store as any).dispatch(writeClassifiedSecret('avito', classifiedSecret))
        .then(() => {
            expect(store.getActions()).toEqual([
                { type: 'WRITE_CLASSIFIED_SECRET_PENDING' },
                {
                    type: 'WRITE_CLASSIFIED_SECRET_REJECTED',
                },
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
