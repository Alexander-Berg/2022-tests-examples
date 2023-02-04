import Enzyme from 'enzyme';
import Adapter from 'enzyme-adapter-react-16';

import { InvoicesPage } from './page';
import {
    firms,
    services,
    intercompanies,
    personCategories,
    manager,
    history,
    productServiceCode,
    paysysList,
    client,
    person,
    invoices
} from './history.data';
import { initializeDesktopRegistry } from 'common/__tests__/registry';

jest.mock('common/utils/old-fetch');
jest.mock('common/utils/request');

Enzyme.configure({ adapter: new Adapter() });

describe('admin', () => {
    beforeAll(initializeDesktopRegistry);

    describe('invoices', () => {
        test('проверяет заполнение фильтра по URL', async () => {
            expect.assertions(13);

            const { perms, search, filter } = history;

            const page = new InvoicesPage({
                perms,
                mocks: {
                    requestGet: [firms, services, intercompanies, personCategories, manager],
                    fetchGet: [productServiceCode, paysysList]
                },
                windowLocationSearch: search
            });

            await page.initializePage();

            expect(page.getFilterValues()).toStrictEqual(filter);

            expect(page.request.get).toHaveBeenCalledTimes(5);
            expect(page.request.get).toHaveBeenNthCalledWith(1, firms.request);
            expect(page.request.get).toHaveBeenNthCalledWith(2, services.request);
            expect(page.request.get).toHaveBeenNthCalledWith(3, intercompanies.request);
            expect(page.request.get).toHaveBeenNthCalledWith(4, personCategories.request);
            expect(page.request.get).toHaveBeenNthCalledWith(5, manager.request);

            expect(page.fetchGet).toHaveBeenCalledTimes(5);
            expect(page.fetchGet).toHaveBeenNthCalledWith(1, ...productServiceCode.request);
            expect(page.fetchGet).toHaveBeenNthCalledWith(2, ...paysysList.request);
            expect(page.fetchGet).toHaveBeenNthCalledWith(3, ...client.request);
            expect(page.fetchGet).toHaveBeenNthCalledWith(4, ...person.request);
            expect(page.fetchGet).toHaveBeenNthCalledWith(5, ...invoices.request);
        });
    });
});
