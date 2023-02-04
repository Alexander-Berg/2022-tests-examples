import Enzyme from 'enzyme';
import Adapter from 'enzyme-adapter-react-16';

import { initializeDesktopRegistry } from 'common/__tests__/registry';
import { registry } from 'common/components/registry';
import { Details as DetailsUser } from 'common/components/PersonWithHistory/Details.user';

import { PersonsPersonPage } from './subpage';
import { perms, mocks } from './subpage.data';

jest.mock('common/monitoring/metrika', () => ({ GOALS: {}, reachGoal: () => {} }));
jest.mock('common/utils/old-fetch');
jest.mock('common/utils/request');

Enzyme.configure({ adapter: new Adapter() });

registry.set('Details', DetailsUser);

describe('user - new-persons - person', () => {
    beforeAll(initializeDesktopRegistry);

    describe('archive', () => {
        afterEach(() => {
            jest.clearAllMocks();
        });

        test('проверка архивирования плательщика', async () => {
            expect.assertions(9);

            const page = new PersonsPersonPage({
                perms,
                fetchGetMocks: [mocks.personforms, mocks.ytPersonform],
                requestGetMocks: [mocks.person, mocks.banks, mocks.regions]
            });

            await page.initializePage();
            await page.archivePerson();

            expect(page.fetchGet).toHaveBeenCalledTimes(2);
            expect(page.fetchGet).toHaveBeenNthCalledWith(1, ...mocks.personforms.request);
            expect(page.fetchGet).toHaveBeenNthCalledWith(2, ...mocks.ytPersonform.request);

            expect(page.request.get).toHaveBeenCalledTimes(3);
            expect(page.request.get).toHaveBeenNthCalledWith(1, ...mocks.person.request);
            expect(page.request.get).toHaveBeenNthCalledWith(2, ...mocks.banks.request);
            expect(page.request.get).toHaveBeenNthCalledWith(3, ...mocks.regions.request);

            expect(page.request.post).toHaveBeenCalledTimes(1);
            expect(page.request.post).toHaveBeenNthCalledWith(1, ...mocks.hidePerson.request);
        });
    });
});
