jest.mock('auto-core/router/auto.ru/react/link');
const link = require('auto-core/router/auto.ru/react/link');

const createLinkMock = require('autoru-frontend/mocks/createLinkMock').default;

link.mockImplementation(createLinkMock('link'));

const getClientUrls = require('./getClientUrls');

const TEST_CASES = [
    {
        name: 'Не должен вернуть ссылки, когда нет доступных категорий',
        dealerCampaigns: [],
        result: [],
    },
    {
        name: 'Должен вернуть одну ссылку, если доступна одна категория и одна секция',
        dealerCampaigns: [
            {
                category: 'CARS',
                section: [ 'USED' ],
            },
        ],
        result: [ { name: 'Легковые с пробегом', tag: 'cars_used', url: 'link/form/?category=cars&section=used&form_type=add' } ]
        ,
    },
    {
        name: 'Должен вернуть дву ссылки, если доступна одна категория и несколько секций (cars)',
        dealerCampaigns: [
            {
                category: 'CARS',
                section: [ 'USED', 'NEW' ],
            },
        ],
        result: [
            { name: 'Легковые новые', tag: 'cars_new', url: 'link/form/?category=cars&section=new&form_type=add' },
            { name: 'Легковые с пробегом', tag: 'cars_used', url: 'link/form/?category=cars&section=used&form_type=add' },
        ],
    },
    {
        name: 'Должен вернуть одну ссылки, если доступна одна категория и несколько секций (moto)',
        dealerCampaigns: [
            {
                category: 'MOTO',
                section: [ 'USED', 'NEW' ],
            },
        ],
        result: [ { name: 'Мото', tag: 'moto', url: 'link/form/?category=moto&form_type=add' } ],
    },
    {
        name: 'Не должен вернуть ссылку на несуществующую категорию.',
        dealerCampaigns: [
            {
                category: 'CARS',
                section: [ 'NEW' ],
            },
            {
                category: 'MOTO',
                section: [ 'USED', 'NEW' ],
            },
            {
                category: 'HUI',
                section: [ 'NEW' ],
            },
        ],
        result: [
            { name: 'Легковые новые', tag: 'cars_new', url: 'link/form/?category=cars&section=new&form_type=add' },
            { name: 'Мото', tag: 'moto', url: 'link/form/?category=moto&form_type=add' },
        ]
        ,
    },
];

TEST_CASES.forEach((testCase) => {
    it(testCase.name, () => {
        const urls = getClientUrls(testCase);
        expect(urls).toEqual(testCase.result);
    });
});
