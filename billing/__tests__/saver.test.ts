import Enzyme from 'enzyme';
import Adapter from 'enzyme-adapter-react-16';
import { Permissions } from 'common/constants';
import { ActPage } from './act.page';
import { act124856267, act124863507, act124863508 } from './data';
import { initializeDesktopRegistry } from 'common/__tests__/registry';

jest.mock('common/utils/old-fetch');
jest.mock('common/utils/request');

Enzyme.configure({ adapter: new Adapter() });

const fullPerms = Object.values(Permissions);

describe('admin', () => {
    beforeAll(initializeDesktopRegistry);

    describe('act', () => {
        test('скрытие блока saver при hidden у акта', async () => {
            expect.assertions(1);

            let page = new ActPage({
                perms: fullPerms,
                mocks: { fetchGet: act124856267 },
                windowLocationSearch: '?act_id=124856267'
            });

            await page.initializePage();
            expect(page.wrapper.exists('Saver')).toBe(false);
        });

        test('скрытие блока saver без BillingSupport', async () => {
            expect.assertions(1);

            let page = new ActPage({
                perms: fullPerms.filter(perm => perm !== 'BillingSupport'),
                mocks: { fetchGet: act124863507 },
                windowLocationSearch: '?act_id=124863507'
            });

            await page.initializePage();
            expect(page.wrapper.exists('Saver')).toBe(false);
        });

        test('скрытие блока saver при не oebs_exportable акте', async () => {
            expect.assertions(1);

            let page = new ActPage({
                perms: fullPerms,
                mocks: { fetchGet: act124863508 },
                windowLocationSearch: '?act_id=124863508'
            });

            await page.initializePage();
            expect(page.wrapper.exists('Saver')).toBe(false);
        });
    });
});
