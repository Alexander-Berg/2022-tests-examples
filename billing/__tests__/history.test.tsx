import Enzyme from 'enzyme';
import Adapter from 'enzyme-adapter-react-16';
import { DeferpaysPage } from './page';

import { fullPerms, services, intercompanies } from './data';
import { history } from './history.data';
import { initializeDesktopRegistry } from 'common/__tests__/registry';

jest.mock('common/utils/old-fetch');
jest.mock('common/utils/request');

Enzyme.configure({ adapter: new Adapter() });

describe('admin', () => {
    beforeAll(initializeDesktopRegistry);

    describe('deferpays', () => {
        test('проверяет заполнение фильтра по URL', async () => {
            expect.assertions(8);
            const { search, filter, client, deferpayContracts, deferpayList } = history;

            const page = new DeferpaysPage({
                perms: fullPerms,
                mocks: {
                    requestGet: [services, intercompanies],
                    fetchGet: [client, deferpayContracts, deferpayList]
                },
                windowLocationSearch: search
            });

            await page.initializePage();

            expect(page.getFilterValues()).toStrictEqual(filter);

            expect(page.request.get).toHaveBeenCalledTimes(2);
            expect(page.request.get).toHaveBeenNthCalledWith(1, services.request);
            expect(page.request.get).toHaveBeenNthCalledWith(2, intercompanies.request);

            expect(page.fetchGet).toHaveBeenCalledTimes(3);
            expect(page.fetchGet).toHaveBeenNthCalledWith(1, ...client.request);
            expect(page.fetchGet).toHaveBeenNthCalledWith(2, ...deferpayContracts.request);
            expect(page.fetchGet).toHaveBeenNthCalledWith(3, ...deferpayList.request);
        });
    });
});
