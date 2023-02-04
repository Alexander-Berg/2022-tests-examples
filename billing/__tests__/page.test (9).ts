import Enzyme from 'enzyme';
import Adapter from 'enzyme-adapter-react-16';

import { intercompanies, clients, requests } from './data';
import { RequestsPage } from './page';
import { initializeDesktopRegistry } from 'common/__tests__/registry';

jest.mock('common/utils/old-fetch');
jest.mock('common/utils/request');

Enzyme.configure({ adapter: new Adapter() });

describe('admin - requests - containers - filter', () => {
    beforeAll(initializeDesktopRegistry);

    afterEach(() => {
        jest.resetAllMocks();
    });

    test('fill all filter fields and press submit button - should make proper request', async () => {
        const page = new RequestsPage({
            mocks: {
                requestGet: [intercompanies],
                fetchGet: [clients, requests]
            }
        });

        await page.initializePage();
        await fillPage(page);
        await page.submitFilter();

        expect(page.getListItems().length).toBe(requests.response.data.items.length + 1);

        expect(page.request.get).toHaveBeenCalledTimes(1);
        expect(page.request.get).toHaveBeenNthCalledWith(1, intercompanies.request);

        expect(page.fetchGet).toHaveBeenCalledTimes(2);
        expect(page.fetchGet).toHaveBeenNthCalledWith(1, ...clients.request);
        expect(page.fetchGet).toHaveBeenNthCalledWith(2, ...requests.request);
    });
});

async function fillPage(page: RequestsPage) {
    page.fillDateField('Дата от', '2019-07-24T00:00:00');
    page.fillDateField('и до', '2019-07-25T00:00:00');
    page.fillTextField('Недо.счет №', '123');
    await page.fillClientSelector('Владелец', 'yb-adm', '496738');
}
