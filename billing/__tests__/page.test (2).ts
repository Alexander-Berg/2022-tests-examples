import Enzyme from 'enzyme';
import Adapter from 'enzyme-adapter-react-16';

import { PaymentStatuses } from 'common/constants';

import { services, intercompanies, clients, orders } from './data';
import { OrdersPage } from './page';
import { initializeDesktopRegistry } from 'common/__tests__/registry';

jest.mock('common/utils/old-fetch');
jest.mock('common/utils/request');

Enzyme.configure({ adapter: new Adapter() });

describe('admin - orders - containers - filter', () => {
    beforeAll(initializeDesktopRegistry);

    afterEach(() => {
        jest.resetAllMocks();
    });

    test('fill all filter fields and press submit button - should make proper request', async () => {
        expect.assertions(8);

        const page = new OrdersPage({
            mocks: {
                requestGet: [services, intercompanies],
                fetchGet: [clients, clients, orders]
            }
        });

        await page.initializePage();
        await fillPage(page);
        await page.submitFilter();

        expect(page.getListItems().length).toBe(orders.response.data.order_list.length);

        expect(page.request.get).toHaveBeenCalledTimes(2);
        expect(page.request.get).toHaveBeenNthCalledWith(1, services.request);
        expect(page.request.get).toHaveBeenNthCalledWith(2, intercompanies.request);

        expect(page.fetchGet).toHaveBeenCalledTimes(3);
        expect(page.fetchGet).toHaveBeenNthCalledWith(1, ...clients.request);
        expect(page.fetchGet).toHaveBeenNthCalledWith(2, ...clients.request);
        expect(page.fetchGet).toHaveBeenNthCalledWith(3, ...orders.request);
    });
});

async function fillPage(page: OrdersPage) {
    page.fillDateField('Дата от', '2018-06-12T00:00:00');
    page.fillDateField('и до', '2018-06-14T00:00:00');
    await page.fillClientSelector('Агентство', 'yb-adm', '5028445');
    await page.fillClientSelector('Клиент', 'yb-adm', '42889276');
    page.fillTextField('Заказ №', '7-35354856');
    page.fillSelect('Включение', PaymentStatuses.TURN_ON);
    page.fillSelect('Сервисы', 'adfox');
}
