import Enzyme from 'enzyme';
import Adapter from 'enzyme-adapter-react-16';
import { ContractPage } from './page';

import { history } from './history.data';
import { initializeDesktopRegistry } from 'common/__tests__/registry';

jest.mock('common/utils/old-fetch');
jest.mock('common/utils/request');

Enzyme.configure({ adapter: new Adapter() });

describe('admin', () => {
    beforeAll(initializeDesktopRegistry);

    describe('contract', () => {
        test('проверяет заполнение фильтра по URL', async () => {
            expect.assertions(8);

            const {
                perms,
                search,
                contract,
                contractCollaterals,
                client,
                person,
                contractPermissions,
                oebs
            } = history;

            const page = new ContractPage({
                perms,
                mocks: {
                    requestGet: [contract, contractCollaterals, client, person],
                    fetchGet: [contractPermissions, oebs]
                },
                windowLocationSearch: search
            });

            await page.waitForLoad();

            expect(page.request.get).toHaveBeenCalledTimes(4);
            expect(page.request.get).toHaveBeenNthCalledWith(1, ...contract.request);
            expect(page.request.get).toHaveBeenNthCalledWith(2, ...contractCollaterals.request);
            expect(page.request.get).toHaveBeenNthCalledWith(3, ...client.request);
            expect(page.request.get).toHaveBeenNthCalledWith(4, ...person.request);

            expect(page.fetchGet).toHaveBeenCalledTimes(2);
            expect(page.fetchGet).toHaveBeenNthCalledWith(1, ...contractPermissions.request);
            expect(page.fetchGet).toHaveBeenNthCalledWith(2, ...oebs.request);
        });
    });
});
