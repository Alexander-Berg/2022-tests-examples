import Enzyme from 'enzyme';
import Adapter from 'enzyme-adapter-react-16';
import { Page } from './page';
import { initializeDesktopRegistry } from 'common/__tests__/registry';

jest.mock('common/utils/old-fetch');
jest.mock('common/utils/request');

Enzyme.configure({ adapter: new Adapter() });

describe('admin - credits', () => {
    beforeAll(initializeDesktopRegistry);

    afterEach(() => {
        jest.resetAllMocks();
    });

    test('показывает кредиты клиента и ограничения', async () => {
        const data = await import('./data');
        const { contracts, activityTypes, restrictions } = data;

        let page = new Page(data);
        await page.initializePage();

        expect(page.getCreditItems().length).toBe(contracts.response[0].contractCreditLimit.length);
        expect(page.getRestrictionItems().length).toBe(
            restrictions.response[0].exceededInvoices.length
        );

        expect(page.request).toHaveBeenCalledTimes(3);
        expect(page.request).nthCalledWith(1, contracts.request);
        expect(page.request).nthCalledWith(2, activityTypes.request);
        expect(page.request).nthCalledWith(3, restrictions.request);
    });
});
