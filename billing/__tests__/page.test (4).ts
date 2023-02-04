import Enzyme from 'enzyme';
import Adapter from 'enzyme-adapter-react-16';
import { filterValues } from './constants';

import { PaymentsPage } from './page';
import { initializeDesktopRegistry } from 'common/__tests__/registry';

jest.mock('common/utils/old-fetch');
jest.mock('common/utils/request');

Enzyme.configure({ adapter: new Adapter() });

async function fillPage(page: PaymentsPage) {
    page.fillSelect('Дата', filterValues.DATE_TYPE);
    page.fillDateField('От', filterValues.FROM_DT);
    page.fillDateField('По', filterValues.TO_DT);
    page.fillTextField('Счёт №', filterValues.INVOICE_EID);
    page.fillTextField('Внутренний ID платежа', filterValues.PAYMENT_ID);
    page.fillTextField('Внешний ID платежа', filterValues.TRANSACTION_ID);
    page.fillTextField('Trust Payment ID', filterValues.TRUST_PAYMENT_ID);
    page.fillTextField('Purchase Token', filterValues.PURCHASE_TOKEN);
    page.fillTextField('Паспорт', filterValues.PASSPORT_ID);
    page.fillTextField('Номер карты', filterValues.CARD_NUMBER);
    page.fillTextField('Код авторизации', filterValues.APPROVAL_CODE);
    page.fillTextField('RRN', filterValues.RRN);
    page.fillTextField('Номер терминала', filterValues.TERMINAL_ID);
    page.fillTextField('Номер реестра', filterValues.REGISTER_ID);
    page.fillSelect('Статус платежа', filterValues.PAYMENT_STATUS);
    page.fillSelect('Фирма', filterValues.FIRM_ID);
    page.fillSelect('Процессинг', filterValues.PROCESSING_CC);
    page.fillSelect('Сервис', filterValues.SERVICE_ID);
    page.fillSelect('Способ оплаты', filterValues.PAYMENT_METHOD);
    page.fillSelect('Валюта', filterValues.CURRENCY_CODE);
    page.fillTextField('Сумма', filterValues.AMOUNT);
}

describe('admin - payments', () => {
    beforeAll(initializeDesktopRegistry);

    afterEach(() => {
        jest.resetAllMocks();
    });

    test('Получение списка платежей по запросу', async () => {
        const {
            services,
            firms,
            processings,
            paymentMethods,
            intercompanies,
            currencies,
            payments
        } = await import('./data');

        let page = new PaymentsPage({
            mocks: {
                requestGet: [
                    services,
                    firms,
                    processings,
                    paymentMethods,
                    intercompanies,
                    currencies
                ],
                fetchGet: [payments]
            }
        });

        await page.initializePage();
        await fillPage(page);
        await page.submitFilter();

        expect(page.getListItems().length).toBe(payments.response.items.length);

        expect(page.request.get).toHaveBeenCalledTimes(6);
        expect(page.request.get).nthCalledWith(1, services.request);
        expect(page.request.get).nthCalledWith(2, firms.request);
        expect(page.request.get).nthCalledWith(3, processings.request);
        expect(page.request.get).nthCalledWith(4, paymentMethods.request);
        expect(page.request.get).nthCalledWith(5, intercompanies.request);
        expect(page.request.get).nthCalledWith(6, currencies.request);

        expect(page.fetchGet).toHaveBeenCalledTimes(1);
        expect(page.fetchGet).nthCalledWith(1, ...payments.request);
    });
});
