import Enzyme from 'enzyme';
import Adapter from 'enzyme-adapter-react-16';
import { stringify } from 'qs';

import { PartnerContractsPage } from './page';
import { initializeDesktopRegistry } from 'common/__tests__/registry';

jest.mock('common/utils/request');

Enzyme.configure({ adapter: new Adapter() });

describe('admin - partner-contracts', () => {
    beforeAll(initializeDesktopRegistry);

    afterEach(() => {
        jest.resetAllMocks();
    });

    async function fillPage(page: PartnerContractsPage) {
        page.fillSelect('Вид договора', 'AFISHA');
        page.wrapper.update();

        page.fillSelect('Фирма', 'apiKeys');
        page.fillSelect('Дата', '2');
        page.fillDateField('от', '2020-06-02T00:00:00');
        page.fillDateField('и до', '2020-06-03T00:00:00');
        page.fillTextField('Договор №', '1234');
        page.fillCheckboxField('Нестрогий поиск по № договора', true);
        page.fillSelect('Подвид договора', 'ALL');
        page.fillSelect('Период актов', '1');
        page.fillSelect('Комплект документов', '1');
    }

    test('Получение договоров по запросу', async () => {
        const {
            contractClasses,
            firms,
            services,
            partnerContractTypes,
            distributionContractTypes,
            intercompanies,
            personTypes,
            docSets,
            billIntervals,
            platformTypes,
            partnerContracts
        } = await import('./data');

        let page = new PartnerContractsPage({
            mocks: {
                requestGet: [
                    contractClasses,
                    firms,
                    services,
                    partnerContractTypes,
                    distributionContractTypes,
                    intercompanies,
                    personTypes,
                    docSets,
                    billIntervals,
                    platformTypes,
                    partnerContracts
                ]
            }
        });

        await page.initializePage();
        await fillPage(page);
        await page.submitFilter();

        expect(page.getListItems().length).toBe(partnerContracts.response.items.length);

        expect(page.request.get).toHaveBeenCalledTimes(11);
        expect(page.request.get).nthCalledWith(1, contractClasses.request);
        expect(page.request.get).nthCalledWith(2, firms.request);
        expect(page.request.get).nthCalledWith(3, services.request);
        expect(page.request.get).nthCalledWith(4, partnerContractTypes.request);
        expect(page.request.get).nthCalledWith(5, distributionContractTypes.request);
        expect(page.request.get).nthCalledWith(6, intercompanies.request);
        expect(page.request.get).nthCalledWith(7, personTypes.request);
        expect(page.request.get).nthCalledWith(8, docSets.request);
        expect(page.request.get).nthCalledWith(9, billIntervals.request);
        expect(page.request.get).nthCalledWith(10, platformTypes.request);
        expect(page.request.get).nthCalledWith(11, partnerContracts.request);
    });

    describe('Заполнение фильтра по URL', () => {
        [
            'РСЯ',
            'Дистрибуция',
            'Справочник',
            'Афиша',
            'Афиша2',
            'Приоритетная сделка',
            'Расходный',
            'Эквайринг'
        ].forEach(type => {
            test(type, async () => {
                const {
                    contractClasses,
                    firms,
                    services,
                    partnerContractTypes,
                    distributionContractTypes,
                    intercompanies,
                    personTypes,
                    docSets,
                    billIntervals,
                    platformTypes
                } = await import('./data');

                const { variants } = await import('./history.data'),
                    { historyQS, historyState, partnerContracts } = variants[type];

                let page = new PartnerContractsPage({
                    mocks: {
                        requestGet: [
                            contractClasses,
                            firms,
                            services,
                            partnerContractTypes,
                            distributionContractTypes,
                            intercompanies,
                            personTypes,
                            docSets,
                            billIntervals,
                            platformTypes,
                            partnerContracts
                        ]
                    },
                    windowLocationSearch: `?${stringify(historyQS)}`
                });

                await page.initializePage();

                expect(page.getState()).toStrictEqual(historyState);

                expect(page.request.get).toHaveBeenCalledTimes(11);
                expect(page.request.get).nthCalledWith(1, contractClasses.request);
                expect(page.request.get).nthCalledWith(2, firms.request);
                expect(page.request.get).nthCalledWith(3, services.request);
                expect(page.request.get).nthCalledWith(4, partnerContractTypes.request);
                expect(page.request.get).nthCalledWith(5, distributionContractTypes.request);
                expect(page.request.get).nthCalledWith(6, intercompanies.request);
                expect(page.request.get).nthCalledWith(7, personTypes.request);
                expect(page.request.get).nthCalledWith(8, docSets.request);
                expect(page.request.get).nthCalledWith(9, billIntervals.request);
                expect(page.request.get).nthCalledWith(10, platformTypes.request);
                expect(page.request.get).nthCalledWith(11, partnerContracts.request);
            });
        });
    });
});
