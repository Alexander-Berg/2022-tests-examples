import Enzyme from 'enzyme';
import Adapter from 'enzyme-adapter-react-16';

import { initializeDesktopRegistry } from 'common/__tests__/registry';
import { ContractType } from 'common/constants/print-form-rules';

import { PrintFormRulesPage } from './page';

jest.mock('common/utils/request');

Enzyme.configure({ adapter: new Adapter() });

describe('admin - print-form-rules', () => {
    beforeAll(initializeDesktopRegistry);

    afterEach(() => {
        jest.resetAllMocks();
    });

    test('Переключение вида договора', async () => {
        const data = await import('./data');
        const { attributeReference } = data;

        const page = new PrintFormRulesPage({
            mocks: {
                requestGet: [attributeReference, attributeReference]
            }
        });

        await page.initializePage();
        await page.changeContractType(ContractType.DISTRIBUTION);
        expect(page.getContractType()).toEqual(ContractType.DISTRIBUTION);
    });

    test('Получение правил ПФ', async () => {
        const data = await import('./data');
        const { attributeReference, printFormRules } = data;

        const page = new PrintFormRulesPage({
            mocks: {
                requestGet: [attributeReference, printFormRules]
            }
        });

        await page.initializePage();
        await page.submitFilter();

        expect(page.getListItems().length).toBe(printFormRules.response.items.length);

        expect(page.request.get).toHaveBeenCalledTimes(2);
        expect(page.request.get).nthCalledWith(1, attributeReference.request);
        expect(page.request.get).nthCalledWith(2, printFormRules.request);
    });
});
