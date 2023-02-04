import Enzyme from 'enzyme';
import Adapter from 'enzyme-adapter-react-16';

import { ClientsPage } from './page';
import { history } from './history.data';
import { initializeDesktopRegistry } from 'common/__tests__/registry';

jest.mock('common/utils/old-fetch');
jest.mock('common/utils/request');

Enzyme.configure({ adapter: new Adapter() });

describe('admin', () => {
    beforeAll(initializeDesktopRegistry);

    describe('clients', () => {
        test('проверяет заполнение фильтра по URL', async () => {
            expect.assertions(6);

            const { perms, search, filter, firmIntercompanyList, manager, clientsList } = history;

            const page = new ClientsPage({
                perms,
                mocks: {
                    requestGet: [firmIntercompanyList, manager],
                    fetchGet: [clientsList]
                },
                windowLocationSearch: search
            });

            await page.initializePage();

            expect(page.getFilterValues()).toStrictEqual(filter);

            expect(page.fetchGet).toHaveBeenCalledTimes(1);
            expect(page.fetchGet).toHaveBeenNthCalledWith(1, ...clientsList.request);

            expect(page.request.get).toHaveBeenCalledTimes(2);
            expect(page.request.get).toHaveBeenNthCalledWith(1, ...firmIntercompanyList.request);
            expect(page.request.get).toHaveBeenNthCalledWith(2, ...manager.request);
        });
    });
});
