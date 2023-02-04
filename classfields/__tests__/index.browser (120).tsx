import React from 'react';
import { render } from 'jest-puppeteer-react';
import { connect } from 'react-redux';
import noop from 'lodash/noop';

import { allure } from '@realty-front/jest-utils/puppeteer/tests-helpers/allure';
import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

// eslint-disable-next-line max-len
import { openJuridicalEGRNPaymentPopup } from 'realty-core/view/react/deskpad/actions/payment/juridical-egrn-paid-report';
import paymentReducer from 'realty-core/view/react/deskpad/reducers/payment';
import { AppProvider, createRootReducer } from 'realty-core/view/react/libs/test-helpers';
// eslint-disable-next-line max-len
import JuridicalPaymentModalMethodsScreenStyles from 'realty-core/view/react/common/components/Payment/PaymentWalletBalance/styles.module.css';
// eslint-disable-next-line max-len
import ExitScreenStyles from 'realty-core/view/react/common/components/Payment/BasePaymentModal/ExitScreen/styles.module.css';
// eslint-disable-next-line max-len
import PromocodesScreenStyles from 'realty-core/view/react/common/components/Payment/PromocodesScreen/styles.module.css';

import Button from 'vertis-react/components/Button';

import JuridicalEGRNPaymentModalContainer from '../container';

import {
    store,
    initPaidPaymentGateStub,
    initPaidPaymentPoorGateStub,
    createAddressInfoGateStub,
    getPaidReportCancelledGateStub,
    getPaidReportNotPaidGateStub,
    getPaidReportSuccessGateStub,
    timeoutStore,
    createAddressInfoWithFullPromocodesPaymentGateStub,
    createAddressInfoWithPartialPromocodesPaymentGateStub,
} from './stubs';

const OpenEgrnReportButton = connect(null, (dispatch) => ({
    onClick: () => dispatch(openJuridicalEGRNPaymentPopup({ offerId: '5437966522810970113' })),
}))(({ onClick }) => (
    <Button theme="realty" view="yellow" onClick={onClick} id="open_juridical_egrn_payment_button">
        Купить отчет
    </Button>
));

const renderOptions = { viewport: { width: 1000, height: 800 } };

const rootReducer = createRootReducer({ payment: paymentReducer });

const Component: React.FunctionComponent<{ store: Record<string, unknown>; Gate?: Record<string, unknown> }> = ({
    store,
    Gate,
}) => (
    <AppProvider rootReducer={rootReducer} initialState={store} Gate={Gate}>
        <OpenEgrnReportButton />
        <JuridicalEGRNPaymentModalContainer />
    </AppProvider>
);

const selectors = {
    openPaymentButton: '#open_juridical_egrn_payment_button',
    closePaymentModalButton: '.CloseModalButton',
    confirmClosePaymentModalButton: `.${ExitScreenStyles.confirmButton}`,
    cancelClosePaymentModalButton: `.${ExitScreenStyles.controls} .Button`,
    payButton: `.${JuridicalPaymentModalMethodsScreenStyles.button}`,
    promocodesButton: `.${PromocodesScreenStyles.button}`,
};

describe('JuridicalEGRNPaymentModal', () => {
    it('Попап открывается и закрывается', async () => {
        const Gate = {
            create: (path: string) => {
                switch (path) {
                    case 'juridical-egrn-paid-report.initPaidReportPayment': {
                        return Promise.resolve(initPaidPaymentGateStub);
                    }
                    case 'egrn-paid-report.createAddressInfo': {
                        return Promise.resolve(createAddressInfoGateStub);
                    }
                }
            },
        };

        await render(<Component store={store} Gate={Gate} />, renderOptions);

        expect(await takeScreenshot()).toMatchImageSnapshot();

        await page.click(selectors.openPaymentButton);

        expect(await takeScreenshot()).toMatchImageSnapshot();

        await page.click(selectors.closePaymentModalButton);

        expect(await takeScreenshot()).toMatchImageSnapshot();

        await page.click(selectors.confirmClosePaymentModalButton);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Попап открывается и при подтверждении закрытия продолжаем покупку', async () => {
        const Gate = {
            create: (path: string) => {
                switch (path) {
                    case 'juridical-egrn-paid-report.initPaidReportPayment': {
                        return Promise.resolve(initPaidPaymentGateStub);
                    }
                    case 'egrn-paid-report.createAddressInfo': {
                        return Promise.resolve(createAddressInfoGateStub);
                    }
                }
            },
        };

        await render(<Component store={store} Gate={Gate} />, renderOptions);

        expect(await takeScreenshot()).toMatchImageSnapshot();

        await page.click(selectors.openPaymentButton);

        expect(await takeScreenshot()).toMatchImageSnapshot();

        await page.click(selectors.closePaymentModalButton);

        expect(await takeScreenshot()).toMatchImageSnapshot();

        await page.click(selectors.cancelClosePaymentModalButton);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Не удалось создать информацию об адресе при инициализации', async () => {
        allure.descriptionHtml('/gate/egrn-paid-report/createAddressInfo -> ERROR');

        const Gate = {
            create: (path: string) => {
                switch (path) {
                    case 'egrn-paid-report.createAddressInfo': {
                        return Promise.reject();
                    }
                }
            },
        };

        await render(<Component store={store} Gate={Gate} />, renderOptions);

        await page.click(selectors.openPaymentButton);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('В процессе создания информации об адресе при инициализации', async () => {
        allure.descriptionHtml('/gate/egrn-paid-report/createAddressInfo -> PENDING');

        const Gate = {
            create: (path: string) => {
                switch (path) {
                    case 'egrn-paid-report.createAddressInfo': {
                        return new Promise(noop);
                    }
                }
            },
        };

        await render(<Component store={store} Gate={Gate} />, renderOptions);

        await page.click(selectors.openPaymentButton);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Не удалось инициализировать платеж', async () => {
        allure.descriptionHtml('/gate/juridical-egrn-paid-report/initPaidReportPayment -> ERROR');

        const Gate = {
            create: (path: string) => {
                switch (path) {
                    case 'egrn-paid-report.createAddressInfo': {
                        return Promise.resolve(createAddressInfoGateStub);
                    }
                    case 'juridical-egrn-paid-report.initPaidReportPayment': {
                        return Promise.reject();
                    }
                }
            },
        };

        await render(<Component store={store} Gate={Gate} />, renderOptions);

        await page.click(selectors.openPaymentButton);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('В процессе инициализации платежа', async () => {
        allure.descriptionHtml('/gate/juridical-egrn-paid-report/initPaidReportPayment -> PENDING');

        const Gate = {
            create: (path: string) => {
                switch (path) {
                    case 'egrn-paid-report.createAddressInfo': {
                        return Promise.resolve(createAddressInfoGateStub);
                    }
                    case 'juridical-egrn-paid-report.initPaidReportPayment': {
                        return new Promise(noop);
                    }
                }
            },
        };

        await render(<Component store={store} Gate={Gate} />, renderOptions);

        await page.click(selectors.openPaymentButton);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Успешно проинициализировали платеж', async () => {
        const Gate = {
            create: (path: string) => {
                switch (path) {
                    case 'egrn-paid-report.createAddressInfo': {
                        return Promise.resolve(createAddressInfoGateStub);
                    }
                    case 'juridical-egrn-paid-report.initPaidReportPayment': {
                        return Promise.resolve(initPaidPaymentGateStub);
                    }
                }
            },
        };

        await render(<Component store={store} Gate={Gate} />, renderOptions);

        await page.click(selectors.openPaymentButton);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Недостаточно денег для покупки', async () => {
        const Gate = {
            create: (path: string) => {
                switch (path) {
                    case 'egrn-paid-report.createAddressInfo': {
                        return Promise.resolve(createAddressInfoGateStub);
                    }
                    case 'juridical-egrn-paid-report.initPaidReportPayment': {
                        return Promise.resolve(initPaidPaymentPoorGateStub);
                    }
                }
            },
        };

        await render(<Component store={store} Gate={Gate} />, renderOptions);

        await page.click(selectors.openPaymentButton);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Не удалось создать платеж', async () => {
        allure.descriptionHtml('/gate/products/update-juridical-products -> ERROR');

        const Gate = {
            create: (path: string) => {
                switch (path) {
                    case 'egrn-paid-report.createAddressInfo': {
                        return Promise.resolve(createAddressInfoGateStub);
                    }
                    case 'juridical-egrn-paid-report.initPaidReportPayment': {
                        return Promise.resolve(initPaidPaymentGateStub);
                    }
                    case 'products.update-juridical-products': {
                        return Promise.reject();
                    }
                }
            },
        };

        await render(<Component store={store} Gate={Gate} />, renderOptions);

        await page.click(selectors.openPaymentButton);

        expect(await takeScreenshot()).toMatchImageSnapshot();

        await page.click(selectors.payButton);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('В процессе создания платежа', async () => {
        allure.descriptionHtml('/gate/products/update-juridical-products -> PENDING');

        const Gate = {
            create: (path: string) => {
                switch (path) {
                    case 'egrn-paid-report.createAddressInfo': {
                        return Promise.resolve(createAddressInfoGateStub);
                    }
                    case 'juridical-egrn-paid-report.initPaidReportPayment': {
                        return Promise.resolve(initPaidPaymentGateStub);
                    }
                    case 'products.update-juridical-products': {
                        return new Promise(noop);
                    }
                }
            },
        };

        await render(<Component store={store} Gate={Gate} />, renderOptions);

        await page.click(selectors.openPaymentButton);

        expect(await takeScreenshot()).toMatchImageSnapshot();

        await page.click(selectors.payButton);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Успешно создали платеж, поллим статус', async () => {
        allure.descriptionHtml('/gate/egrn-paid-report/getPaidReport -> PENDING');

        const Gate = {
            create: (path: string) => {
                switch (path) {
                    case 'egrn-paid-report.createAddressInfo': {
                        return Promise.resolve(createAddressInfoGateStub);
                    }
                    case 'juridical-egrn-paid-report.initPaidReportPayment': {
                        return Promise.resolve(initPaidPaymentGateStub);
                    }
                    case 'products.update-juridical-products': {
                        return Promise.resolve();
                    }
                    case 'egrn-paid-report.getPaidReport': {
                        return new Promise(noop);
                    }
                }
            },
        };

        await render(<Component store={store} Gate={Gate} />, renderOptions);

        await page.click(selectors.openPaymentButton);

        expect(await takeScreenshot()).toMatchImageSnapshot();

        await page.click(selectors.payButton);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Успешно завершили оплату', async () => {
        const Gate = {
            create: (path: string) => {
                switch (path) {
                    case 'egrn-paid-report.createAddressInfo': {
                        return Promise.resolve(createAddressInfoGateStub);
                    }
                    case 'juridical-egrn-paid-report.initPaidReportPayment': {
                        return Promise.resolve(initPaidPaymentGateStub);
                    }
                    case 'products.update-juridical-products': {
                        return Promise.resolve();
                    }
                    case 'egrn-paid-report.getPaidReport': {
                        return Promise.resolve(getPaidReportSuccessGateStub);
                    }
                }
            },
        };

        await render(<Component store={store} Gate={Gate} />, renderOptions);

        await page.click(selectors.openPaymentButton);

        expect(await takeScreenshot()).toMatchImageSnapshot();

        await page.click(selectors.payButton);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Не удалось получить статус оплаты, продолжаем его поллить', async () => {
        allure.descriptionHtml('/gate/egrn-paid-report/getPaidReport -> ERROR');

        const Gate = {
            create: (path: string) => {
                switch (path) {
                    case 'egrn-paid-report.createAddressInfo': {
                        return Promise.resolve(createAddressInfoGateStub);
                    }
                    case 'juridical-egrn-paid-report.initPaidReportPayment': {
                        return Promise.resolve(initPaidPaymentGateStub);
                    }
                    case 'products.update-juridical-products': {
                        return Promise.resolve();
                    }
                    case 'egrn-paid-report.getPaidReport': {
                        return Promise.reject();
                    }
                }
            },
        };

        await render(<Component store={store} Gate={Gate} />, renderOptions);

        await page.click(selectors.openPaymentButton);

        expect(await takeScreenshot()).toMatchImageSnapshot();

        await page.click(selectors.payButton);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Завершили оплату с CANCELLED статусом', async () => {
        const Gate = {
            create: (path: string) => {
                switch (path) {
                    case 'egrn-paid-report.createAddressInfo': {
                        return Promise.resolve(createAddressInfoGateStub);
                    }
                    case 'juridical-egrn-paid-report.initPaidReportPayment': {
                        return Promise.resolve(initPaidPaymentGateStub);
                    }
                    case 'products.update-juridical-products': {
                        return Promise.resolve();
                    }
                    case 'egrn-paid-report.getPaidReport': {
                        return Promise.resolve(getPaidReportCancelledGateStub);
                    }
                }
            },
        };

        await render(<Component store={store} Gate={Gate} />, renderOptions);

        await page.click(selectors.openPaymentButton);

        expect(await takeScreenshot()).toMatchImageSnapshot();

        await page.click(selectors.payButton);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Завершили оплату с NOT_PAID статусом', async () => {
        const Gate = {
            create: (path: string) => {
                switch (path) {
                    case 'egrn-paid-report.createAddressInfo': {
                        return Promise.resolve(createAddressInfoGateStub);
                    }
                    case 'juridical-egrn-paid-report.initPaidReportPayment': {
                        return Promise.resolve(initPaidPaymentGateStub);
                    }
                    case 'products.update-juridical-products': {
                        return Promise.resolve();
                    }
                    case 'egrn-paid-report.getPaidReport': {
                        return Promise.resolve(getPaidReportNotPaidGateStub);
                    }
                }
            },
        };

        await render(<Component store={store} Gate={Gate} />, renderOptions);

        await page.click(selectors.openPaymentButton);

        expect(await takeScreenshot()).toMatchImageSnapshot();

        await page.click(selectors.payButton);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    // при рендере модалки идет инициализации платежа, это стор затирает
    // надо придумать как таймаут скипнуть
    it.skip('Завершили оплату с таймаутом', async () => {
        await render(<Component store={timeoutStore} />, renderOptions);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Оплата полностью промокодами', async () => {
        const Gate = {
            create: (path: string) => {
                switch (path) {
                    case 'egrn-paid-report.createAddressInfo': {
                        return Promise.resolve(createAddressInfoWithFullPromocodesPaymentGateStub);
                    }
                    case 'juridical-egrn-paid-report.initPaidReportPayment': {
                        return Promise.resolve(initPaidPaymentGateStub);
                    }
                    case 'products.update-juridical-products': {
                        return Promise.resolve();
                    }
                    case 'egrn-paid-report.getPaidReport': {
                        return Promise.resolve(getPaidReportSuccessGateStub);
                    }
                }
            },
        };

        await render(<Component store={store} Gate={Gate} />, renderOptions);

        await page.click(selectors.openPaymentButton);

        expect(await takeScreenshot()).toMatchImageSnapshot();

        await page.click(selectors.promocodesButton);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Оплата частично промокодами', async () => {
        const Gate = {
            create: (path: string) => {
                switch (path) {
                    case 'egrn-paid-report.createAddressInfo': {
                        return Promise.resolve(createAddressInfoWithPartialPromocodesPaymentGateStub);
                    }
                    case 'juridical-egrn-paid-report.initPaidReportPayment': {
                        return Promise.resolve(initPaidPaymentGateStub);
                    }
                    case 'products.update-juridical-products': {
                        return Promise.resolve();
                    }
                    case 'egrn-paid-report.getPaidReport': {
                        return Promise.resolve(getPaidReportSuccessGateStub);
                    }
                }
            },
        };

        await render(<Component store={store} Gate={Gate} />, renderOptions);

        await page.click(selectors.openPaymentButton);

        expect(await takeScreenshot()).toMatchImageSnapshot();

        await page.click(selectors.promocodesButton);

        expect(await takeScreenshot()).toMatchImageSnapshot();

        await page.click(selectors.payButton);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
