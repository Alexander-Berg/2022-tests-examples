import Enzyme from 'enzyme';
import Adapter from 'enzyme-adapter-react-16';
import { Permissions } from 'common/constants';
import { ActPage } from './act.page';
import { act124863520, act124863523 } from './data';
import { initializeDesktopRegistry } from 'common/__tests__/registry';

jest.mock('common/utils/old-fetch');
jest.mock('common/utils/request');

Enzyme.configure({ adapter: new Adapter() });

const fullPerms = Object.values(Permissions);

describe('admin', () => {
    beforeAll(initializeDesktopRegistry);

    describe('act', () => {
        test('отображение блока print-form при trp у акта', async () => {
            expect.assertions(1);

            let page = new ActPage({
                perms: fullPerms,
                mocks: { fetchGet: act124863520 },
                windowLocationSearch: '?act_id=124863520'
            });

            await page.initializePage();
            expect(page.wrapper.find('PrintForm').prop('isVisible')).toBe(true);
        });

        test('отображение блока print-form при firm=4 у акта', async () => {
            expect.assertions(1);

            let page = new ActPage({
                perms: fullPerms,
                mocks: { fetchGet: act124863523 },
                windowLocationSearch: '?act_id=124863523'
            });

            await page.initializePage();
            expect(page.wrapper.find('PrintForm').prop('isVisible')).toBe(true);
        });
    });
});
