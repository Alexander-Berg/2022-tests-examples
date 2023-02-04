import Enzyme from 'enzyme';
import Adapter from 'enzyme-adapter-react-16';

import { PersonsPage } from './page';

import { getPersonCategoryList, getPersonList } from 'common/api/snout';

import { personCategories, history } from './history.data';
import { initializeDesktopRegistry } from 'common/__tests__/registry';

jest.mock('common/api/snout');

Enzyme.configure({ adapter: new Adapter() });

describe('admin', () => {
    beforeAll(initializeDesktopRegistry);

    describe('persons', () => {
        test('проверяет заполнение фильтра по URL', async () => {
            expect.assertions(5);
            const { perms, search, filter, personList } = history;

            const page = new PersonsPage({
                perms,
                mocks: {
                    snoutApi: {
                        getPersonCategoryList: personCategories,
                        getPersonList: personList
                    }
                },
                windowLocationSearch: search
            });

            await page.initializePage();

            expect(page.getFilterValues()).toStrictEqual(filter);

            expect(getPersonCategoryList).toHaveBeenCalledTimes(1);
            expect(getPersonCategoryList).toHaveBeenNthCalledWith(1, personCategories.request);

            expect(getPersonList).toHaveBeenCalledTimes(1);
            expect(getPersonList).toHaveBeenNthCalledWith(1, personList.request);
        });
    });
});
