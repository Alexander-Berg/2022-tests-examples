import Enzyme from 'enzyme';
import Adapter from 'enzyme-adapter-react-16';

import { services, intercompanies, client42889276, client5028445, orders, history } from './data';
import { OrdersPage } from './page';
import { initializeDesktopRegistry } from 'common/__tests__/registry';

jest.mock('common/utils/old-fetch');
jest.mock('common/utils/request');

Enzyme.configure({ adapter: new Adapter() });

describe('admin', () => {
    beforeAll(initializeDesktopRegistry);

    describe('orders', () => {
        test('заполнение фильтра по URL', async () => {
            expect.assertions(8);

            const { perms, search, filter } = history;

            const page = new OrdersPage({
                perms,
                mocks: {
                    requestGet: [services, intercompanies],
                    fetchGet: [client42889276, client5028445, orders]
                },
                windowLocationSearch: search
            });

            await page.initializePageFromHistory();

            expect(page.getFilterValues()).toStrictEqual(filter);

            expect(page.request.get).toHaveBeenCalledTimes(2);
            expect(page.request.get).toHaveBeenNthCalledWith(1, services.request);
            expect(page.request.get).toHaveBeenNthCalledWith(2, intercompanies.request);

            expect(page.fetchGet).toHaveBeenCalledTimes(3);
            expect(page.fetchGet).toHaveBeenNthCalledWith(1, ...client42889276.request);
            expect(page.fetchGet).toHaveBeenNthCalledWith(2, ...client5028445.request);
            expect(page.fetchGet).toHaveBeenNthCalledWith(3, ...orders.request);
        });
    });
});
