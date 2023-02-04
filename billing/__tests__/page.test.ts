import Enzyme from 'enzyme';
import Adapter from 'enzyme-adapter-react-16';
import { ContractsPage } from './page';
import { initializeDesktopRegistry } from 'common/__tests__/registry';

jest.mock('common/utils/old-fetch');
jest.mock('common/utils/request');

Enzyme.configure({ adapter: new Adapter() });

describe('admin - contracts', () => {
    beforeAll(initializeDesktopRegistry);

    afterEach(() => {
        jest.resetAllMocks();
    });

    async function fillPage(page: ContractsPage) {
        page.fillDateField('от', '2020-06-02T00:00:00');
        page.fillDateField('и до', '2020-06-03T00:00:00');
        page.fillSelect('Дата', 'DT');
        page.fillTextField('Договор №', '1234');
        page.fillSelect('Тип договора', '1');
        page.fillSelect('Сервисы', 'apiKeys');
        await page.fillClientSelector('Агентство', 'yb-adm', '496740');
        await page.fillClientSelector('Клиент', 'yb-adm', '496740');
        await page.fillPersonSelector('Плательщик', '1337');
    }

    test('Получение договоров по запросу', async () => {
        const {
            services,
            enums,
            intercompanies,
            personTypes,
            clients,
            persons,
            contracts
        } = await import('./data');

        let page = new ContractsPage({
            mocks: {
                fetchGet: [clients, clients, persons],
                requestGet: [services, enums, intercompanies, personTypes, contracts]
            }
        });

        await page.initializePage();
        await fillPage(page);
        await page.submitFilter();

        expect(page.getListItems().length).toBe(contracts.response.items.length);

        expect(page.request.get).toHaveBeenCalledTimes(5);
        expect(page.request.get).nthCalledWith(1, services.request);
        expect(page.request.get).nthCalledWith(2, enums.request);
        expect(page.request.get).nthCalledWith(3, intercompanies.request);
        expect(page.request.get).nthCalledWith(4, personTypes.request);
        expect(page.request.get).nthCalledWith(5, contracts.request);

        expect(page.fetchGet).toHaveBeenCalledTimes(3);
        expect(page.fetchGet).nthCalledWith(1, ...clients.request);
        expect(page.fetchGet).nthCalledWith(2, ...clients.request);
        expect(page.fetchGet).nthCalledWith(3, ...persons.request);
    });

    test('Заполнение фильтра по URL', async () => {
        const { services, enums, intercompanies, personTypes } = await import('./data');
        const { search, filter, contracts } = await import('./history.data');

        let page = new ContractsPage({
            mocks: {
                fetchGet: [],
                requestGet: [services, enums, intercompanies, personTypes, contracts]
            },
            windowLocationSearch: search
        });

        await page.initializePage();

        expect(page.getFilterValues()).toStrictEqual(filter);

        expect(page.request.get).toHaveBeenCalledTimes(8);
        expect(page.request.get).nthCalledWith(1, services.request);
        expect(page.request.get).nthCalledWith(2, enums.request);
        expect(page.request.get).nthCalledWith(3, intercompanies.request);
        expect(page.request.get).nthCalledWith(4, personTypes.request);
        expect(page.request.get).nthCalledWith(8, contracts.request);

        expect(page.fetchGet).toHaveBeenCalledTimes(0);
    });
});
