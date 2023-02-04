import Enzyme from 'enzyme';
import Adapter from 'enzyme-adapter-react-16';

import {
    currencies,
    firms,
    services,
    intercompanies,
    personTypes,
    acts,
    client,
    person,
    manager,
    history
} from './data';
import { ActsPage } from './page';
import { initializeDesktopRegistry } from 'common/__tests__/registry';

jest.mock('common/utils/old-fetch');
jest.mock('common/utils/request');

Enzyme.configure({ adapter: new Adapter() });

describe('admin', () => {
    beforeAll(initializeDesktopRegistry);

    describe('acts', () => {
        test('заполнение фильтра по URL', async () => {
            expect.assertions(12);

            const { perms, search, filter } = history;

            const page = new ActsPage({
                perms,
                mocks: {
                    requestGet: [currencies, firms, services, intercompanies, personTypes, manager],
                    fetchGet: [acts, client, person]
                },
                windowLocationSearch: search
            });

            await page.initializePage();

            expect(page.getFilterValues()).toStrictEqual(filter);

            expect(page.request.get).toHaveBeenCalledTimes(6);
            expect(page.request.get).toHaveBeenNthCalledWith(1, currencies.request);
            expect(page.request.get).toHaveBeenNthCalledWith(2, firms.request);
            expect(page.request.get).toHaveBeenNthCalledWith(3, services.request);
            expect(page.request.get).toHaveBeenNthCalledWith(4, intercompanies.request);
            expect(page.request.get).toHaveBeenNthCalledWith(5, personTypes.request);
            expect(page.request.get).toHaveBeenNthCalledWith(6, manager.request);

            expect(page.fetchGet).toHaveBeenCalledTimes(3);
            expect(page.fetchGet).toHaveBeenNthCalledWith(1, ...acts.request);
            expect(page.fetchGet).toHaveBeenNthCalledWith(2, ...client.request);
            expect(page.fetchGet).toHaveBeenNthCalledWith(3, ...person.request);
        });
    });
});
