import Enzyme from 'enzyme';
import Adapter from 'enzyme-adapter-react-16';

import { services, products1, products2, products3 } from './data';
import { ProductCatalogPage } from './page';
import { initializeDesktopRegistry } from 'common/__tests__/registry';

jest.mock('common/utils/old-fetch');
jest.mock('common/utils/request');

Enzyme.configure({ adapter: new Adapter() });

describe('admin - product-list - containers - filter', () => {
    beforeAll(initializeDesktopRegistry);

    afterEach(() => {
        jest.resetAllMocks();
    });

    test('open page - should request initial data', async () => {
        expect.assertions(9);

        const page = new ProductCatalogPage({
            mocks: {
                requestGet: [services],
                fetchGet: [products1, products2, products3]
            }
        });

        await page.initializePage();

        page.fillSelect('Сервис', 102);
        await page.submitFilter();
        expect(page.getListItems().length).toBe(products1.response.data.items.length);

        page.fillTextField('Название продукта', 'Авансовый платеж');
        await page.submitFilter();
        expect(page.getListItems().length).toBe(products2.response.data.items.length);

        page.fillTextField('ID', 509143);
        await page.submitFilter();
        expect(page.getListItems().length).toBe(products3.response.data.items.length);

        expect(page.request.get).toHaveBeenCalledTimes(1);
        expect(page.request.get).toHaveBeenNthCalledWith(1, services.request);

        expect(page.fetchGet).toHaveBeenCalledTimes(3);
        expect(page.fetchGet).toHaveBeenNthCalledWith(1, ...products1.request);
        expect(page.fetchGet).toHaveBeenNthCalledWith(2, ...products2.request);
        expect(page.fetchGet).toHaveBeenNthCalledWith(3, ...products3.request);
    });
});
