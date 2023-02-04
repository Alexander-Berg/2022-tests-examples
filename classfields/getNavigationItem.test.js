const getNavigationItem = require('./getNavigationItem');

const TESTS_NO_TAB = [
    {
        name: 'Не должен возвращать таб статистики, если в параметрах нет марки',
        tab: 'stats',
        searchParams: { category: 'cars' },
    },
    {
        name: 'Не должен возвращать таб статистики, если в параметрах нет марки',
        tab: 'stats',
        searchParams: { category: 'cars', catalog_filter: [ {} ] },
    },
    {
        name: 'Не должен возвращать таб статистики, если в параметрах несколько марок',
        tab: 'stats',
        searchParams: { category: 'cars', mark_model_nameplate: [ 'AUDI', 'BMW' ] },
    },
    {
        name: 'Не должен возвращать таб статистики, если в параметрах несколько марок',
        tab: 'stats',
        searchParams: { category: 'cars', catalog_filter: [ { mark: 'AUDI' }, { model: 'BMW' } ] },
    },
    {
        name: 'Не должен возвращать таб видео, если в параметрах несколько марок',
        tab: 'video',
        searchParams: { category: 'cars', mark_model_nameplate: [ 'AUDI', 'BMW' ] },
    },
    {
        name: 'Не должен возвращать таб видео, если в параметрах несколько марок',
        tab: 'video',
        searchParams: { category: 'cars', catalog_filter: [ { mark: 'AUDI' }, { model: 'BMW' } ] },
    },
    {
        name: 'Не должен возвращать таб видео, если марка запрещена в разделе видео',
        tab: 'video',
        searchParams: { category: 'cars', mark: 'LAMBORGHINI' },
    },
    {
        name: 'Не должен возвращать таб видео, если марка запрещена в разделе видео',
        tab: 'video',
        searchParams: { category: 'cars', mark: 'lamborghini' },
    },
    {
        name: 'Не должен возвращать таб статистики, если марка "эксклюзив"',
        tab: 'stats',
        searchParams: { category: 'cars', mark: 'EXCLUSIVE' },
    },
    {
        name: 'Не должен возвращать таб каталога, если категория не cars',
        tab: 'catalog',
        searchParams: { parent_category: 'trucks' },
    },
    {
        name: 'Не должен возвращать таб каталога, если категория не cars',
        tab: 'catalog',
        searchParams: { parent_category: 'moto' },
    },
];

const TESTS_LINK_PARAMS = [
    {
        name: 'Должен построить ссылку на каталог с правильными параметрами',
        tab: 'catalog',
        searchParams: { category: 'cars', mark: 'AUDI', section: 'used' },
        linkPage: 'catalog',
        linkParams: { category: 'cars', mark: 'AUDI', model: undefined, super_gen: undefined },
    },
    {
        name: 'Должен построить ссылку на отзывы с правильными параметрами',
        tab: 'reviews',
        searchParams: { category: 'moto', section: 'used' },
        linkPage: 'reviews',
        linkParams: { mark: undefined, model: undefined, parent_category: 'moto', super_gen: undefined },
    },
    {
        name: 'Должен построить ссылку на отзывы c moto_category',
        tab: 'reviews',
        searchParams: { category: 'moto', moto_category: 'atv', section: 'used' },
        linkPage: 'reviews',
        linkParams: { mark: undefined, model: undefined, parent_category: 'moto', super_gen: undefined, category: 'ATV' },
    },
    {
        name: 'Должен построить ссылку на отзывы c moto_category, mark и model',
        tab: 'reviews',
        searchParams: { category: 'moto', moto_category: 'atv', catalog_filter: [ { mark: 'HONDA', model: 'c100' } ] },
        linkPage: 'reviews',
        linkParams: { mark: 'HONDA', model: 'c100', parent_category: 'moto', super_gen: undefined, category: 'ATV' },
    },
    {
        name: 'Должен построить ссылку на отзывы c trucks_category',
        tab: 'reviews',
        searchParams: { category: 'trucks', trucks_category: 'artic', section: 'used' },
        linkPage: 'reviews',
        linkParams: { mark: undefined, model: undefined, parent_category: 'trucks', super_gen: undefined, category: 'ARTIC' },
    },
    {
        name: 'Должен построить ссылку на листинг с правильными параметрами',
        tab: 'listing',
        searchParams: { view_type: 'list' },
        linkPage: 'listing',
        linkParams: {},
    },
    {
        name: 'Должен построить ссылку на листинг с правильными параметрами',
        tab: 'listing',
        searchParams: { mark: 'AUDI', model: 'A6', super_gen: '21210593', configuration_id: '21210635', complectation_id: '21210635_21410389_21365499' },
        linkPage: 'listing',
        linkParams: { configuration_id: '21210635', mark: 'AUDI', model: 'A6', super_gen: '21210593', tech_param_id: '21365499' },
    },
    {
        name: 'должен вернуть ссылку без марок, если в параметрах несколько марок (nameplate)',
        tab: 'dealers',
        searchParams: { category: 'cars', mark_model_nameplate: [ 'AUDI#A6##21210593', 'BMW#X5##21307931' ], section: 'used' },
        linkPage: 'dealers-listing',
        linkParams: { section: 'used' },
    },
    {
        name: 'должен вернуть ссылку без марок, если в параметрах несколько марок',
        tab: 'dealers',
        searchParams: { category: 'cars', catalog_filter: [
            { mark: 'AUDI', model: 'A6', generation: '21210593' },
            { mark: 'BMW', model: 'X5', generation: '21307931' },
        ], section: 'new' },
        linkPage: 'dealers-listing',
        linkParams: { section: 'new' },
    },
    {
        name: 'должен вернуть ссылку без марки, если ссылка стоится для б/у листинга',
        tab: 'dealers',
        searchParams: { category: 'cars', catalog_filter: [ { mark: 'AUDI' } ], section: 'used' },
        linkPage: 'dealers-listing',
        linkParams: { mark: undefined, section: 'used' },
    },
    {
        name: 'должен вернуть ссылку без моделей, если в параметрах несколько моделей',
        tab: 'catalog',
        searchParams: { category: 'cars', mark_model_nameplate: [ 'AUDI#A6##21210593', 'AUDI#A5##21307931' ], section: 'used' },
        linkPage: 'catalog',
        linkParams: { category: 'cars', mark: 'AUDI' },
    },
    {
        name: 'должен вернуть ссылку без моделей, если в параметрах несколько моделей',
        tab: 'catalog',
        searchParams: { category: 'cars', catalog_filter: [
            { mark: 'AUDI', model: 'A6', generation: '21210593' },
            { mark: 'AUDI', model: 'A5', generation: '21307931' },
        ], section: 'used' },
        linkPage: 'catalog',
        linkParams: { category: 'cars', mark: 'AUDI' },
    },
    {
        name: 'должен вернуть ссылку без поколенй, если в параметрах несколько поколений',
        tab: 'catalog',
        searchParams: { category: 'cars', mark_model_nameplate: [ 'AUDI#A6##21210593', 'AUDI#A6##21307931' ], section: 'used' },
        linkPage: 'catalog',
        linkParams: { category: 'cars', mark: 'AUDI', model: 'A6' },
    },
    {
        name: 'должен вернуть ссылку без поколенй, если в параметрах несколько поколений',
        tab: 'catalog',
        searchParams: { category: 'cars', catalog_filter: [
            { mark: 'AUDI', model: 'A6', generation: '21210593' },
            { mark: 'AUDI', model: 'A6', generation: '21307931' },
        ], section: 'used' },
        linkPage: 'catalog',
        linkParams: { category: 'cars', mark: 'AUDI', model: 'A6' },
    },
    {
        name: 'должен отбросить вендров (mark_model_nameplate=[VENDOR1]) при формировании ссылок',
        tab: 'dealers',
        searchParams: { category: 'cars', mark_model_nameplate: [ 'VENDOR1' ] },
        linkPage: 'dealers-listing',
        linkParams: { mark: undefined, model: undefined, section: 'all', super_gen: undefined },
    },
    {
        name: 'должен отбросить вендров (catalog_filter=[{vendor:VENDOR1}]) при формировании ссылок',
        tab: 'dealers',
        searchParams: { category: 'cars', catalog_filter: [ { vendor: 'VENDOR1' } ] },
        linkPage: 'dealers-listing',
        linkParams: { mark: undefined, model: undefined, section: 'all', super_gen: undefined },
    },
    {
        name: 'должен убрать параметры поиска, которых нет в дилерах',
        tab: 'dealers',
        searchParams: { category: 'cars', seller_group: 'PRIVATE', engine_group: 'GASOLINE', section: 'all' },
        linkPage: 'dealers-listing',
        linkParams: { section: 'all' },
    },
    {
        name: 'Должен правильно строить ссылки со старыми параметрами',
        tab: 'video',
        searchParams: { category: 'cars', catalog_filter: [ {} ] },
        linkPage: 'video',
        linkParams: { category: 'cars' },
    },
];

describe('Ссылки должны строиться с правильными параметрами', () => {
    let linkBuilderStub;
    beforeEach(() => {
        linkBuilderStub = jest.fn();
    });

    TESTS_LINK_PARAMS.forEach((testCase) => {
        it(testCase.name, () => {
            getNavigationItem(testCase.tab, testCase.searchParams, linkBuilderStub);
            expect(linkBuilderStub).toHaveBeenCalledWith(testCase.linkPage, testCase.linkParams);
        });
    });
});

describe('Не возвращает определенные табы, когда они не нужны', () => {
    let linkBuilderStub;
    beforeEach(() => {
        linkBuilderStub = jest.fn();
    });

    TESTS_NO_TAB.forEach((testCase) => {
        it(testCase.name, () => {
            expect(getNavigationItem(testCase.tab, testCase.searchParams, linkBuilderStub)).toBeNull();
        });
    });
});
