jest.mock('auto-core/react/lib/gateApi', () => {
    return {
        getResource: jest.fn(),
    };
});

import mockStore from 'autoru-frontend/mocks/mockStore';

import { DAY } from 'auto-core/lib/consts';

import gateApi from 'auto-core/react/lib/gateApi';
import creditProductMock from 'auto-core/react/dataDomain/credit/mocks/creditProduct.mockchain';
import creditApplicationMock from 'auto-core/react/dataDomain/credit/mocks/creditApplication.mockchain';

import { CreditProductType, BankID } from 'auto-core/types/TCreditBroker';

import publishCreditApplication from './publishCreditApplication';

const now = 1626270089116;
const created = String(now - 5 * DAY);
const updated = String(now - 3 * DAY);
const scheduledAt = String(now - 2 * DAY);
const schedulerLastUpdate = String(now - Number(DAY));
const creditApplication = creditApplicationMock()
    .withDates({
        created,
        updated,
        scheduledAt,
        schedulerLastUpdate,
    })
    .value();

const getResource = gateApi.getResource as jest.MockedFunction<typeof gateApi.getResource>;

it('обновляет и активирует кредитную заявку, добавляет продукты', async() => {
    getResource.mockImplementation(
        (resourceName, resourceArgs) => {
            switch (resourceName) {
                case 'updateCreditApplication':
                    return Promise.resolve({
                        credit_application: resourceArgs?.body,
                        result: { ok: {} },
                    });

                case 'getCreditProductList':
                    return Promise.resolve({
                        credit_products: Array(5).fill(1).map((value, index) => {
                            return creditProductMock()
                                .withType(CreditProductType.CONSUMER)
                                .withID(String(index))
                                .value();
                        }),
                        result: { ok: {} },
                    });
                case 'addProductsToCreditApplication':
                    return Promise.resolve({
                        result: { ok: {} },
                    });

                default:
                    return Promise.reject();
            }
        },
    );

    const store = mockStore({});

    await store.dispatch(publishCreditApplication({
        creditApplication,
        creditApplicationID: creditApplication.id,
        withAutosend: true,
    }));

    const actions = store.getActions();
    const mappedActions = store.getActions().map(action => action.type);
    const lastUpdateAction = [ ...actions ].reverse().find(action => action.type === 'CREDIT_APPLICATION_UPDATE_SUCCESS');

    expect(lastUpdateAction.payload.credit_application.user_settings.tags).toContain('control_0821');
    expect(mappedActions).toMatchSnapshot();
});

it('обновляет и активирует кредитную заявку, добавляет только Тиньков в экспе эксклюзивности', async() => {
    getResource.mockImplementation(
        (resourceName, resourceArgs) => {
            switch (resourceName) {
                case 'updateCreditApplication':
                    return Promise.resolve({
                        credit_application: resourceArgs?.body,
                        result: { ok: {} },
                    });

                case 'getCreditProductList':
                    return Promise.resolve({
                        credit_products: [
                            creditProductMock()
                                .withType(CreditProductType.CONSUMER)
                                .withBankID(BankID.ALFABANK)
                                .withID('1')
                                .value(),
                            creditProductMock()
                                .withType(CreditProductType.CONSUMER)
                                .withBankID(BankID.TINKOFF)
                                .withID('2')
                                .value(),
                            creditProductMock()
                                .withType(CreditProductType.CONSUMER)
                                .withBankID(BankID.RAIFFEISEN)
                                .withID('3')
                                .value(),
                        ],
                        result: { ok: {} },
                    });
                case 'addProductsToCreditApplication':
                    return Promise.resolve({
                        result: { ok: {} },
                    });

                default:
                    return Promise.reject();
            }
        },
    );

    const store = mockStore({
        config: {
            data: {
                experimentsData: {
                    experiments: { 'AUTORUFRONT-19673_tinkoff_exclusive': true },
                },
            },
        },
    });

    await store.dispatch(publishCreditApplication({
        creditApplication,
        creditApplicationID: creditApplication.id,
        withAutosend: true,
    }));

    const actions = store.getActions();
    const lastUpdateAction = [ ...actions ].reverse().find(action => action.type === 'CREDIT_APPLICATION_UPDATE_SUCCESS');

    expect(lastUpdateAction.payload.credit_application.user_settings.tags).toContain('exp_0821');
    expect(getResource).toHaveBeenNthCalledWith(4, 'addProductsToCreditApplication', {
        creditProductIDs: [ '2' ],
        creditApplicationID: creditApplication.id,
    });
});

it('не кидает ошибку, если продуктов нет, и не пытается их добавить к заявке', async() => {
    getResource.mockImplementation(
        (resourceName, resourceArgs) => {
            switch (resourceName) {
                case 'updateCreditApplication':
                    return Promise.resolve({
                        credit_application: resourceArgs?.body,
                        result: { ok: {} },
                    });

                case 'getCreditProductList':
                    return Promise.resolve({
                        credit_products: [],
                        result: { ok: {} },
                    });
                case 'addProductsToCreditApplication':
                    return Promise.resolve({
                        result: { ok: {} },
                    });

                default:
                    return Promise.reject();
            }
        },
    );

    const store = mockStore({});
    await store.dispatch(publishCreditApplication({
        creditApplication,
        creditApplicationID: creditApplication.id,
    }));

    const actions = store.getActions().map(action => action.type);
    expect(actions).toMatchSnapshot();
});
