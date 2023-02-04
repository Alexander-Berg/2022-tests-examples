import React from 'react';
import noop from 'lodash/noop';
import { render } from 'jest-puppeteer-react';
import { connect } from 'react-redux';
import { Frame } from 'puppeteer';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';
import { allure } from '@realty-front/jest-utils/puppeteer/tests-helpers/allure';

import { openEGRNPaymentPopup } from 'realty-core/view/react/deskpad/actions/payment/egrn-paid-report';
import createRootReducer from 'realty-core/view/react/libs/create-page-root-reducer';

import Button from 'vertis-react/components/Button';

import { AppProvider } from 'view/libs/test-helpers';
import { paymentReducer } from 'view/reducers/payment';

import paymentMethodsStyles from '../../PaymentModal/stages/PaymentModalMethodSelecting/styles.module.css';
import paymentMethodsDetailsStyles from '../../PaymentModal/stages/PaymentModalSelectedMethodDetails/styles.module.css';
import paymentButtonStyles from '../../PaymentModalPaymentButton/styles.module.css';
import paymentMethodStyles from '../../PaymentModalPaymentMethod/styles.module.css';
import paymentModalBaseStyles from '../../PaymentModalBase/styles.module.css';
import { PaymentEGRNModal } from '../PaymentEGRNModal';

import {
    getAddressInfoGateStub,
    initPaymentGateStub,
    storeMock,
    getPaymentStatusClosedGateStub,
    initPaymentFullPromocodesPaymentGateStub,
    initPaymentPartialPromocodesPaymentGateStub,
    initPaymentWithPreferWalletGateStub,
    initPaymentWithManyMethodsGateStub,
    performPaymentWithConfirmationGateStub,
    performPaymentWithoutConfirmationGateStub,
    getPaymentStatusErrorGateStub,
    getPaymentStatusTimeoutGateStub,
} from './mocks';

const reducer = createRootReducer({
    payment: paymentReducer,
});

const OpenEgrnReportButton = connect(null, (dispatch) => ({
    // @ts-expect-error replace ts-ingore
    onClick: () => dispatch(openEGRNPaymentPopup({ offerId: '5437966522810970113' })),
}))(({ onClick }) => (
    <Button theme="realty" view="yellow" onClick={onClick} id="open_egrn_payment_button">
        Купить бабушкины помидоры
    </Button>
));

const Component: React.FunctionComponent<{ store: Record<string, unknown>; Gate?: Record<string, unknown> }> = ({
    store,
    Gate,
}) => (
    <AppProvider initialState={store} Gate={Gate} rootReducer={reducer}>
        <OpenEgrnReportButton />
        <PaymentEGRNModal />
    </AppProvider>
);

enum SIZES {
    SMALL_PHONE,
    MEDIUM_PHONE,
    TABLET,
}

const WIDTH = {
    [SIZES.SMALL_PHONE]: 320,
    [SIZES.MEDIUM_PHONE]: 375,
    [SIZES.TABLET]: 670,
};

const HEIGHT = {
    [SIZES.SMALL_PHONE]: 568,
    [SIZES.MEDIUM_PHONE]: 712,
    [SIZES.TABLET]: 800,
};

const sizes = [SIZES.SMALL_PHONE, SIZES.MEDIUM_PHONE, SIZES.TABLET];

const sizesDescriptions = {
    [SIZES.SMALL_PHONE]: 'Маленький телефон',
    [SIZES.MEDIUM_PHONE]: 'Средний телефон',
    [SIZES.TABLET]: 'Планшет',
};

const getResolution = (size: SIZES) => {
    return { viewport: { width: WIDTH[size], height: HEIGHT[size] } };
};

const selectors = {
    openEgrnPopupButton: '#open_egrn_payment_button',
    closeEgrnPopupButton: `.${paymentModalBaseStyles.cross}`,
    paymentMethods: {
        getMethod: (n: number) => `.${paymentMethodsStyles.methods} > div:nth-child(${n})`,
        changeButton: `.${paymentMethodStyles.change}`,
    },
    bankDetails: {
        cardNumberInput: '.yoomoney-checkout-input_type_bank-card input',
        cardMonthInput: '.yoomoney-checkout-bank-card__validity-input_type_date input',
        cardYearInput:
            '.yoomoney-checkout-bank-card__validity-devider + .yoomoney-checkout-bank-card__validity-input input',
        cardCVCInput: '.yoomoney-checkout-bank-card__cvc-input input',
        allowBindCheckbox: `.${paymentMethodsDetailsStyles.container} .Checkbox`,
        emailInput: `.${paymentMethodsDetailsStyles.container} .TextInput__control`,
    },
    mainButton: `.${paymentButtonStyles.container} .Button`,
};

describe('EGRNModal', () => {
    sizes.forEach((size) => {
        describe(sizesDescriptions[size], () => {
            it('Попап открыт / закрыт', async () => {
                const Gate = {
                    create: (path: string) => {
                        switch (path) {
                            case 'egrn-paid-report.createAddressInfo': {
                                return Promise.resolve(getAddressInfoGateStub);
                            }
                            case 'egrn-paid-report.initPaidReportPayment': {
                                return Promise.resolve(initPaymentGateStub);
                            }
                        }
                    },
                };

                await render(<Component store={storeMock} Gate={Gate} />, getResolution(size));

                expect(await takeScreenshot()).toMatchImageSnapshot();

                await page.click(selectors.openEgrnPopupButton);

                expect(await takeScreenshot()).toMatchImageSnapshot();

                await page.click(selectors.closeEgrnPopupButton);

                expect(await takeScreenshot()).toMatchImageSnapshot();
            });

            describe('Промежуточные состояния, оплата', () => {
                it('Экран загрузки, ждем информации об адресе', async () => {
                    allure.descriptionHtml('/gate/egrn-paid-report/createAddressInfo');

                    const Gate = {
                        create: (path: string) => {
                            switch (path) {
                                case 'egrn-paid-report.createAddressInfo': {
                                    return new Promise(noop);
                                }
                            }
                        },
                    };

                    await render(<Component store={storeMock} Gate={Gate} />, getResolution(size));

                    await page.click(selectors.openEgrnPopupButton);

                    expect(await takeScreenshot()).toMatchImageSnapshot();
                });

                it('Экран ошибки, упал запрос информации об адресе', async () => {
                    allure.descriptionHtml('/gate/egrn-paid-report/createAddressInfo');

                    const Gate = {
                        create: (path: string) => {
                            switch (path) {
                                case 'egrn-paid-report.createAddressInfo': {
                                    return Promise.reject();
                                }
                            }
                        },
                    };

                    await render(<Component store={storeMock} Gate={Gate} />, getResolution(size));

                    await page.click(selectors.openEgrnPopupButton);

                    expect(await takeScreenshot()).toMatchImageSnapshot();
                });

                it('Экран загрузки, загрузка инициализации оплаты', async () => {
                    allure.descriptionHtml('/gate/egrn-paid-report/initPaidReportPayment');

                    const Gate = {
                        create: (path: string) => {
                            switch (path) {
                                case 'egrn-paid-report.createAddressInfo': {
                                    return Promise.resolve(getAddressInfoGateStub);
                                }
                                case 'egrn-paid-report.initPaidReportPayment': {
                                    return new Promise(noop);
                                }
                            }
                        },
                    };

                    await render(<Component store={storeMock} Gate={Gate} />, getResolution(size));

                    await page.click(selectors.openEgrnPopupButton);

                    expect(await takeScreenshot()).toMatchImageSnapshot();
                });

                it('Экран ошибки, упал запрос инициализации оплаты', async () => {
                    allure.descriptionHtml('/gate/egrn-paid-report/initPaidReportPayment');

                    const Gate = {
                        create: (path: string) => {
                            switch (path) {
                                case 'egrn-paid-report.createAddressInfo': {
                                    return Promise.resolve(getAddressInfoGateStub);
                                }
                                case 'egrn-paid-report.initPaidReportPayment': {
                                    return Promise.reject();
                                }
                            }
                        },
                    };

                    await render(<Component store={storeMock} Gate={Gate} />, getResolution(size));

                    await page.click(selectors.openEgrnPopupButton);

                    expect(await takeScreenshot()).toMatchImageSnapshot();
                });

                it('Ждем ответа ручки инициализации покупки', async () => {
                    allure.descriptionHtml('/gate/payment/perform-payment');

                    const Gate = {
                        create: (path: string) => {
                            switch (path) {
                                case 'egrn-paid-report.createAddressInfo': {
                                    return Promise.resolve(getAddressInfoGateStub);
                                }
                                case 'egrn-paid-report.initPaidReportPayment': {
                                    return Promise.resolve(initPaymentGateStub);
                                }
                                case 'payment.perform-payment': {
                                    return new Promise(noop);
                                }
                            }
                        },
                    };

                    await render(<Component store={storeMock} Gate={Gate} />, getResolution(size));

                    await page.click(selectors.openEgrnPopupButton);

                    expect(await takeScreenshot()).toMatchImageSnapshot();

                    await page.click(selectors.mainButton);

                    expect(await takeScreenshot()).toMatchImageSnapshot();
                });

                it('Ручка инициализации платежа упала', async () => {
                    allure.descriptionHtml('/gate/payment/perform-payment');

                    const Gate = {
                        create: (path: string) => {
                            switch (path) {
                                case 'egrn-paid-report.createAddressInfo': {
                                    return Promise.resolve(getAddressInfoGateStub);
                                }
                                case 'egrn-paid-report.initPaidReportPayment': {
                                    return Promise.resolve(initPaymentGateStub);
                                }
                                case 'payment.perform-payment': {
                                    return Promise.reject();
                                }
                            }
                        },
                    };

                    await render(<Component store={storeMock} Gate={Gate} />, getResolution(size));

                    await page.click(selectors.openEgrnPopupButton);

                    expect(await takeScreenshot()).toMatchImageSnapshot();

                    await page.click(selectors.mainButton);

                    expect(await takeScreenshot()).toMatchImageSnapshot();
                });

                it('Ручка инициализации - платеж нужно подтвердить', async () => {
                    allure.descriptionHtml('/gate/payment/perform-payment -> { needConfirmation: true }');

                    const Gate = {
                        create: (path: string) => {
                            switch (path) {
                                case 'egrn-paid-report.createAddressInfo': {
                                    return Promise.resolve(getAddressInfoGateStub);
                                }
                                case 'egrn-paid-report.initPaidReportPayment': {
                                    return Promise.resolve(initPaymentGateStub);
                                }
                                case 'payment.perform-payment': {
                                    return Promise.resolve(performPaymentWithConfirmationGateStub);
                                }
                                case 'payment.get-payment-status': {
                                    return new Promise(noop);
                                }
                            }
                        },
                    };

                    await render(<Component store={storeMock} Gate={Gate} />, getResolution(size));

                    await page.click(selectors.openEgrnPopupButton);

                    expect(await takeScreenshot()).toMatchImageSnapshot();

                    await page.click(selectors.mainButton);

                    expect(await takeScreenshot()).toMatchImageSnapshot();
                });

                it('Ручка инициализации - без платеж подтверждения', async () => {
                    allure.descriptionHtml('/gate/payment/perform-payment -> { needConfirmation: false }');

                    const Gate = {
                        create: (path: string) => {
                            switch (path) {
                                case 'egrn-paid-report.createAddressInfo': {
                                    return Promise.resolve(getAddressInfoGateStub);
                                }
                                case 'egrn-paid-report.initPaidReportPayment': {
                                    return Promise.resolve(initPaymentGateStub);
                                }
                                case 'payment.perform-payment': {
                                    return Promise.resolve(performPaymentWithoutConfirmationGateStub);
                                }
                                case 'payment.get-payment-status': {
                                    return new Promise(noop);
                                }
                            }
                        },
                    };

                    await render(<Component store={storeMock} Gate={Gate} />, getResolution(size));

                    await page.click(selectors.openEgrnPopupButton);

                    expect(await takeScreenshot()).toMatchImageSnapshot();

                    await page.click(selectors.mainButton);

                    expect(await takeScreenshot()).toMatchImageSnapshot();
                });

                it('Платеж не прошел', async () => {
                    allure.descriptionHtml('/gate/payment/get-payment-status -> { status: "ERROR" }');

                    const Gate = {
                        create: (path: string) => {
                            switch (path) {
                                case 'egrn-paid-report.createAddressInfo': {
                                    return Promise.resolve(getAddressInfoGateStub);
                                }
                                case 'egrn-paid-report.initPaidReportPayment': {
                                    return Promise.resolve(initPaymentGateStub);
                                }
                                case 'payment.perform-payment': {
                                    return Promise.resolve(performPaymentWithoutConfirmationGateStub);
                                }
                                case 'payment.get-payment-status': {
                                    return Promise.resolve(getPaymentStatusErrorGateStub);
                                }
                            }
                        },
                    };

                    await render(<Component store={storeMock} Gate={Gate} />, getResolution(size));

                    await page.click(selectors.openEgrnPopupButton);

                    expect(await takeScreenshot()).toMatchImageSnapshot();

                    await page.click(selectors.mainButton);

                    expect(await takeScreenshot()).toMatchImageSnapshot();
                });

                it('Таймаут получения статуса платежа', async () => {
                    allure.descriptionHtml('/gate/payment/get-payment-status -> { status: "TIMEOUT" }');

                    const Gate = {
                        create: (path: string) => {
                            switch (path) {
                                case 'egrn-paid-report.createAddressInfo': {
                                    return Promise.resolve(getAddressInfoGateStub);
                                }
                                case 'egrn-paid-report.initPaidReportPayment': {
                                    return Promise.resolve(initPaymentGateStub);
                                }
                                case 'payment.perform-payment': {
                                    return Promise.resolve(performPaymentWithoutConfirmationGateStub);
                                }
                                case 'payment.get-payment-status': {
                                    return Promise.resolve(getPaymentStatusTimeoutGateStub);
                                }
                            }
                        },
                    };

                    await render(<Component store={storeMock} Gate={Gate} />, getResolution(size));

                    await page.click(selectors.openEgrnPopupButton);

                    expect(await takeScreenshot()).toMatchImageSnapshot();

                    await page.click(selectors.mainButton);

                    expect(await takeScreenshot()).toMatchImageSnapshot();
                });
            });

            describe('Экран методов оплаты', () => {
                it('По дефолту выбирается первый метод оплаты', async () => {
                    const Gate = {
                        create: (path: string) => {
                            switch (path) {
                                case 'egrn-paid-report.createAddressInfo': {
                                    return Promise.resolve(getAddressInfoGateStub);
                                }
                                case 'egrn-paid-report.initPaidReportPayment': {
                                    return Promise.resolve(initPaymentGateStub);
                                }
                            }
                        },
                    };

                    await render(<Component store={storeMock} Gate={Gate} />, getResolution(size));

                    await page.click(selectors.openEgrnPopupButton);

                    expect(await takeScreenshot()).toMatchImageSnapshot();
                });

                it('Пришел признак "Оплачивать всегда из кошелька"', async () => {
                    const Gate = {
                        create: (path: string) => {
                            switch (path) {
                                case 'egrn-paid-report.createAddressInfo': {
                                    return Promise.resolve(getAddressInfoGateStub);
                                }
                                case 'egrn-paid-report.initPaidReportPayment': {
                                    return Promise.resolve(initPaymentWithPreferWalletGateStub);
                                }
                            }
                        },
                    };

                    await render(<Component store={storeMock} Gate={Gate} />, getResolution(size));

                    await page.click(selectors.openEgrnPopupButton);

                    expect(await takeScreenshot()).toMatchImageSnapshot();
                });

                it('Переключаем методы оплаты', async () => {
                    const Gate = {
                        create: (path: string) => {
                            switch (path) {
                                case 'egrn-paid-report.createAddressInfo': {
                                    return Promise.resolve(getAddressInfoGateStub);
                                }
                                case 'egrn-paid-report.initPaidReportPayment': {
                                    return Promise.resolve(initPaymentGateStub);
                                }
                            }
                        },
                    };

                    await render(<Component store={storeMock} Gate={Gate} />, getResolution(size));

                    await page.click(selectors.openEgrnPopupButton);

                    expect(await takeScreenshot()).toMatchImageSnapshot();

                    await page.click(selectors.paymentMethods.getMethod(2));

                    expect(await takeScreenshot()).toMatchImageSnapshot();

                    await page.click(selectors.paymentMethods.getMethod(3));

                    expect(await takeScreenshot()).toMatchImageSnapshot();

                    await page.click(selectors.paymentMethods.getMethod(4));

                    expect(await takeScreenshot()).toMatchImageSnapshot();
                });

                it('Пришло много методов оплаты', async () => {
                    const Gate = {
                        create: (path: string) => {
                            switch (path) {
                                case 'egrn-paid-report.createAddressInfo': {
                                    return Promise.resolve(getAddressInfoGateStub);
                                }
                                case 'egrn-paid-report.initPaidReportPayment': {
                                    return Promise.resolve(initPaymentWithManyMethodsGateStub);
                                }
                            }
                        },
                    };

                    await render(<Component store={storeMock} Gate={Gate} />, getResolution(size));

                    await page.click(selectors.openEgrnPopupButton);

                    expect(await takeScreenshot()).toMatchImageSnapshot();

                    await page.$eval(selectors.paymentMethods.getMethod(9), (el) => el.scrollIntoView());

                    expect(await takeScreenshot()).toMatchImageSnapshot();
                });
            });

            describe('Оплачиваем покупку промокодами', () => {
                it('только промокоды', async () => {
                    allure.descriptionHtml('price.effective = 0, price.availableMoneyFeaturesPrice > 0');

                    const Gate = {
                        create: (path: string) => {
                            switch (path) {
                                case 'egrn-paid-report.createAddressInfo': {
                                    return Promise.resolve(getAddressInfoGateStub);
                                }
                                case 'egrn-paid-report.initPaidReportPayment': {
                                    return Promise.resolve(initPaymentFullPromocodesPaymentGateStub);
                                }
                                case 'payment.perform-payment': {
                                    return Promise.resolve(performPaymentWithoutConfirmationGateStub);
                                }
                                case 'payment.get-payment-status': {
                                    return Promise.resolve(getPaymentStatusClosedGateStub);
                                }
                            }
                        },
                    };

                    await render(<Component store={storeMock} Gate={Gate} />, getResolution(size));

                    await page.click(selectors.openEgrnPopupButton);

                    expect(await takeScreenshot()).toMatchImageSnapshot();

                    await page.click(selectors.mainButton);

                    // unstable
                    // expect(await takeScreenshot()).toMatchImageSnapshot();
                });

                it('Частично промокоды', async () => {
                    allure.descriptionHtml('price.effective > 0, price.availableMoneyFeaturesPrice > 0');

                    const Gate = {
                        create: (path: string) => {
                            switch (path) {
                                case 'egrn-paid-report.createAddressInfo': {
                                    return Promise.resolve(getAddressInfoGateStub);
                                }
                                case 'egrn-paid-report.initPaidReportPayment': {
                                    return Promise.resolve(initPaymentPartialPromocodesPaymentGateStub);
                                }
                                case 'payment.perform-payment': {
                                    return Promise.resolve(performPaymentWithoutConfirmationGateStub);
                                }
                                case 'payment.get-payment-status': {
                                    return Promise.resolve(getPaymentStatusClosedGateStub);
                                }
                            }
                        },
                    };

                    await render(<Component store={storeMock} Gate={Gate} />, getResolution(size));

                    await page.click(selectors.openEgrnPopupButton);

                    expect(await takeScreenshot()).toMatchImageSnapshot();

                    await page.click(selectors.mainButton);

                    expect(await takeScreenshot()).toMatchImageSnapshot();

                    await page.click(selectors.mainButton);

                    // unstable
                    // expect(await takeScreenshot()).toMatchImageSnapshot();
                });
            });

            describe('Покупку привязанной картой', () => {
                it.skip('Платеж успешно прошел', async () => {
                    allure.descriptionHtml('/gate/payment/get-payment-status -> { status: "CLOSED" }');

                    const Gate = {
                        create: (path: string) => {
                            switch (path) {
                                case 'egrn-paid-report.createAddressInfo': {
                                    return Promise.resolve(getAddressInfoGateStub);
                                }
                                case 'egrn-paid-report.initPaidReportPayment': {
                                    return Promise.resolve(initPaymentGateStub);
                                }
                                case 'payment.perform-payment': {
                                    return Promise.resolve(performPaymentWithoutConfirmationGateStub);
                                }
                                case 'payment.get-payment-status': {
                                    return Promise.resolve(getPaymentStatusClosedGateStub);
                                }
                            }
                        },
                    };

                    await render(<Component store={storeMock} Gate={Gate} />, getResolution(size));

                    await page.click(selectors.openEgrnPopupButton);

                    expect(await takeScreenshot()).toMatchImageSnapshot();

                    await page.click(selectors.mainButton);

                    // unstable
                    // expect(await takeScreenshot()).toMatchImageSnapshot();
                });
            });

            describe('Покупка яндекс.деньгами', () => {
                it.skip('Платеж успешно прошел', async () => {
                    allure.descriptionHtml('/gate/payment/get-payment-status -> { status: "CLOSED" }');

                    const Gate = {
                        create: (path: string) => {
                            switch (path) {
                                case 'egrn-paid-report.createAddressInfo': {
                                    return Promise.resolve(getAddressInfoGateStub);
                                }
                                case 'egrn-paid-report.initPaidReportPayment': {
                                    return Promise.resolve(initPaymentGateStub);
                                }
                                case 'payment.perform-payment': {
                                    return Promise.resolve(performPaymentWithConfirmationGateStub);
                                }
                                case 'payment.get-payment-status': {
                                    return Promise.resolve(getPaymentStatusClosedGateStub);
                                }
                            }
                        },
                    };

                    await render(<Component store={storeMock} Gate={Gate} />, getResolution(size));

                    await page.click(selectors.openEgrnPopupButton);

                    expect(await takeScreenshot()).toMatchImageSnapshot();

                    await page.click(selectors.paymentMethods.getMethod(4));

                    expect(await takeScreenshot()).toMatchImageSnapshot();

                    await page.click(selectors.mainButton);

                    // unstable
                    // expect(await takeScreenshot()).toMatchImageSnapshot();
                });
            });

            describe('Покупка кошельком', () => {
                it.skip('Платеж успешно прошел', async () => {
                    allure.descriptionHtml('/gate/payment/get-payment-status -> { status: "CLOSED" }');

                    const Gate = {
                        create: (path: string) => {
                            switch (path) {
                                case 'egrn-paid-report.createAddressInfo': {
                                    return Promise.resolve(getAddressInfoGateStub);
                                }
                                case 'egrn-paid-report.initPaidReportPayment': {
                                    return Promise.resolve(initPaymentGateStub);
                                }
                                case 'payment.perform-payment': {
                                    return Promise.resolve(performPaymentWithoutConfirmationGateStub);
                                }
                                case 'payment.get-payment-status': {
                                    return Promise.resolve(getPaymentStatusClosedGateStub);
                                }
                            }
                        },
                    };

                    await render(<Component store={storeMock} Gate={Gate} />, getResolution(size));

                    await page.click(selectors.openEgrnPopupButton);

                    expect(await takeScreenshot()).toMatchImageSnapshot();

                    await page.click(selectors.paymentMethods.getMethod(3));

                    expect(await takeScreenshot()).toMatchImageSnapshot();

                    await page.click(selectors.mainButton);

                    // unstable
                    // expect(await takeScreenshot()).toMatchImageSnapshot();
                });
            });

            describe('Покупка новой картой', () => {
                it('Успешно переключается в дополнительный экран и обратно', async () => {
                    const Gate = {
                        create: (path: string) => {
                            switch (path) {
                                case 'egrn-paid-report.createAddressInfo': {
                                    return Promise.resolve(getAddressInfoGateStub);
                                }
                                case 'egrn-paid-report.initPaidReportPayment': {
                                    return Promise.resolve(initPaymentGateStub);
                                }
                            }
                        },
                    };

                    await render(<Component store={storeMock} Gate={Gate} />, getResolution(size));

                    await page.click(selectors.openEgrnPopupButton);

                    expect(await takeScreenshot()).toMatchImageSnapshot();

                    await page.click(selectors.paymentMethods.getMethod(2));

                    expect(await takeScreenshot()).toMatchImageSnapshot();

                    await page.click(selectors.mainButton);

                    await customPage.getYKassaCardFormFrame();

                    expect(await takeScreenshot()).toMatchImageSnapshot();

                    await page.click(selectors.paymentMethods.changeButton);

                    expect(await takeScreenshot()).toMatchImageSnapshot();
                });

                it('Ошибки заполнения формы карты при попытке оплатить', async () => {
                    const Gate = {
                        create: (path: string) => {
                            switch (path) {
                                case 'egrn-paid-report.createAddressInfo': {
                                    return Promise.resolve(getAddressInfoGateStub);
                                }
                                case 'egrn-paid-report.initPaidReportPayment': {
                                    return Promise.resolve(initPaymentGateStub);
                                }
                            }
                        },
                    };

                    await render(<Component store={storeMock} Gate={Gate} />, getResolution(size));

                    await page.click(selectors.openEgrnPopupButton);

                    await page.click(selectors.paymentMethods.getMethod(2));

                    await page.click(selectors.mainButton);

                    await customPage.getYKassaCardFormFrame();

                    expect(await takeScreenshot()).toMatchImageSnapshot();

                    await page.click(selectors.mainButton);

                    expect(await takeScreenshot()).toMatchImageSnapshot();
                });

                it('Можем изменить почту и убрать галочку с "Запомнить карту"', async () => {
                    const Gate = {
                        create: (path: string) => {
                            switch (path) {
                                case 'egrn-paid-report.createAddressInfo': {
                                    return Promise.resolve(getAddressInfoGateStub);
                                }
                                case 'egrn-paid-report.initPaidReportPayment': {
                                    return Promise.resolve(initPaymentGateStub);
                                }
                            }
                        },
                    };

                    await render(<Component store={storeMock} Gate={Gate} />, getResolution(size));

                    await page.click(selectors.openEgrnPopupButton);

                    await page.click(selectors.paymentMethods.getMethod(2));

                    await page.click(selectors.mainButton);

                    await customPage.getYKassaCardFormFrame();

                    await page.$eval(selectors.bankDetails.allowBindCheckbox, (el) => el.scrollIntoView());

                    expect(await takeScreenshot()).toMatchImageSnapshot();

                    await page.click(selectors.bankDetails.allowBindCheckbox);

                    expect(await takeScreenshot()).toMatchImageSnapshot();

                    await page.click(selectors.bankDetails.allowBindCheckbox);

                    expect(await takeScreenshot()).toMatchImageSnapshot();

                    await page.focus(selectors.bankDetails.emailInput);

                    await page.keyboard.press('Backspace');
                    await page.keyboard.press('Backspace');
                    await page.keyboard.press('Backspace');

                    expect(await takeScreenshot()).toMatchImageSnapshot();

                    await page.click(selectors.mainButton);

                    expect(await takeScreenshot()).toMatchImageSnapshot();

                    await page.type(selectors.bankDetails.emailInput, '.eu');

                    await page.click(selectors.mainButton);

                    expect(await takeScreenshot()).toMatchImageSnapshot();
                });

                // нестабильный тест
                it.skip('Заполняем данные карты и успешно оплачиваем', async () => {
                    const Gate = {
                        create: (path: string) => {
                            switch (path) {
                                case 'egrn-paid-report.createAddressInfo': {
                                    return Promise.resolve(getAddressInfoGateStub);
                                }
                                case 'egrn-paid-report.initPaidReportPayment': {
                                    return Promise.resolve(initPaymentGateStub);
                                }
                                case 'payment.perform-payment': {
                                    return Promise.resolve(performPaymentWithoutConfirmationGateStub);
                                }
                                case 'payment.get-payment-status': {
                                    return Promise.resolve(getPaymentStatusClosedGateStub);
                                }
                            }
                        },
                    };

                    await render(<Component store={storeMock} Gate={Gate} />, getResolution(size));

                    await page.click(selectors.openEgrnPopupButton);

                    await page.click(selectors.paymentMethods.getMethod(2));

                    await page.click(selectors.mainButton);

                    const frame: Frame = await customPage.getYKassaCardFormFrame();

                    expect(await takeScreenshot()).toMatchImageSnapshot();

                    // не всегда стабильно заполняет форму, связано с автопереносом фокуса после ввода номера карты
                    // иногда он срабатывает раньше
                    await frame.type(selectors.bankDetails.cardNumberInput, '4111111111111111');

                    await frame.type(selectors.bankDetails.cardMonthInput, '10');
                    await frame.type(selectors.bankDetails.cardYearInput, '25');
                    await frame.type(selectors.bankDetails.cardCVCInput, '123');

                    expect(await takeScreenshot()).toMatchImageSnapshot();

                    // этот клик не проходит, нужно понять почему
                    await page.click(selectors.mainButton);

                    expect(await takeScreenshot()).toMatchImageSnapshot();
                });
            });
        });
    });
});
