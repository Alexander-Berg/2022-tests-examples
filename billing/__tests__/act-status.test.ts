import Enzyme from 'enzyme';
import Adapter from 'enzyme-adapter-react-16';
import { Permissions } from 'common/constants';
import { ActPage } from './act.page';
import { act124864551 } from './data';
import { initializeDesktopRegistry } from 'common/__tests__/registry';

jest.mock('common/utils/old-fetch');
jest.mock('common/utils/request');

Enzyme.configure({ adapter: new Adapter() });

const fullPerms = Object.values(Permissions);

describe('admin', () => {
    beforeAll(initializeDesktopRegistry);

    describe('act', () => {
        test('скрытие кнопки перевыгрузки без права OEBSReexportAct', async () => {
            expect.assertions(1);

            let page = new ActPage({
                perms: fullPerms.filter(perm => perm !== 'OEBSReexportAct'),
                mocks: { fetchGet: act124864551 },
                windowLocationSearch: '?act_id=124864551'
            });

            await page.initializePage();

            expect(page.wrapper.find('ExportState').prop('showReexport')).toBe(false);
        });
    });
});
