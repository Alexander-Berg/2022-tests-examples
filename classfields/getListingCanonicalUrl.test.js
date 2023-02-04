const _ = require('lodash');
const MockDate = require('mockdate');
const getListingCanonicalUrl = require('./getListingCanonicalUrl');

const breadcrumbsPublicApiMock = require('auto-core/react/dataDomain/breadcrumbsPublicApi/mocks/breadcrumbsPublicApi.mock');
const listingState = require('autoru-frontend/mockData/state/listingState.mock');
const geoMock = require('auto-core/react/dataDomain/geo/mocks/geo.mock');

let mockState;

beforeEach(() => {
    mockState = _.cloneDeep({
        breadcrumbsPublicApi: breadcrumbsPublicApiMock,
        ...listingState,
        geo: geoMock,
    });

    mockState.listing.data = {
        offers: [ {}, {}, {}, {} ],
        search_parameters: {
            category: 'cars',
            catalog_filter: [ {
                mark: 'FORD',
                model: 'ECOSPORT',
                generation: '20104320',
            } ],
            section: 'all',
            year_from: 2018,
            year_to: 2018,
        },
    };
});

afterEach(() => {
    MockDate.reset();
});

const isAmp = { isAmp: true };

describe('canonical', () => {
    it('отдал url без изменений', () => {
        expect(getListingCanonicalUrl(mockState)).toEqual('/moskva/cars/ford/ecosport/2018-year/20104320/all/');
    });

    it('вырезал из url год и гео, если год не подходит под комплектацию', () => {
        mockState.listing.data.search_parameters.year_from = 2019;
        mockState.listing.data.search_parameters.year_to = 2019;

        expect(getListingCanonicalUrl(mockState)).toEqual('/cars/ford/ecosport/20104320/all/');
    });

    it('не вырезал из url год и гео, если год подходит под комплектацию', () => {
        mockState.listing.data.search_parameters.year_from = 2018;
        mockState.listing.data.search_parameters.year_to = 2018;

        expect(getListingCanonicalUrl(mockState)).toEqual('/moskva/cars/ford/ecosport/2018-year/20104320/all/');
    });

    it('вырезал из url год и гео, если год не подходит под модель', () => {
        MockDate.set('2019-08-30');
        mockState.listing.data.search_parameters.year_from = 2013;
        mockState.listing.data.search_parameters.year_to = 2013;
        mockState.listing.data.search_parameters.catalog_filter = [ { mark: 'FORD', model: 'ECOSPORT' } ];

        expect(getListingCanonicalUrl(mockState)).toEqual('/cars/ford/ecosport/all/');
    });

    it('не вырезал из url год и гео, если год подходит под модель', () => {
        MockDate.set('2019-08-30');
        mockState.listing.data.search_parameters.year_from = 2018;
        mockState.listing.data.search_parameters.year_to = 2018;
        mockState.listing.data.search_parameters.catalog_filter = [ { mark: 'FORD', model: 'ECOSPORT' } ];

        expect(getListingCanonicalUrl(mockState)).toEqual('/moskva/cars/ford/ecosport/2018-year/all/');
    });

    it('не вырезал из url body_type_group(кузов)', () => {
        mockState.listing.data.search_parameters.body_type_group = [ 'ALLROAD_5_DOORS' ];

        expect(getListingCanonicalUrl(mockState)).toEqual('/moskva/cars/ford/ecosport/2018-year/20104320/all/body-allroad_5_doors/');
    });

    it('не вырезал из url gear_type(топливо)', () => {
        mockState.listing.data.search_parameters.engine_group = 'GASOLINE';

        expect(getListingCanonicalUrl(mockState)).toEqual('/moskva/cars/ford/ecosport/2018-year/20104320/all/engine-benzin/');
    });

    it('не вырезал из url color(цвет)', () => {
        mockState.listing.data.search_parameters.color = [ 'FAFBFB' ];
        expect(getListingCanonicalUrl(mockState)).toEqual('/moskva/cars/ford/ecosport/2018-year/20104320/all/color-belyj/');
    });

    it('не вырезал из url engine_group(привод)', () => {
        mockState.listing.data.search_parameters.gear_type = 'ALL_WHEEL_DRIVE';

        expect(getListingCanonicalUrl(mockState)).toEqual('/moskva/cars/ford/ecosport/2018-year/20104320/all/drive-4x4_wheel/');
    });

    it('не вырезал из url цену', () => {
        mockState.listing.data.search_parameters.price_to = 1000000;
        mockState.listingPriceRanges.data = [ { price_to: 1000000 } ];

        expect(getListingCanonicalUrl(mockState)).toEqual('/moskva/cars/ford/ecosport/2018-year/20104320/all/do-1000000/');
    });

    it('для невалидной цены строим каноникал на ближайшую большую валидную цену', () => {
        mockState.listing.data.search_parameters.price_to = 1_000_001;
        mockState.listingPriceRanges.data = [ { price_to: 1_000_000 }, { price_to: 1_500_000 } ];

        expect(getListingCanonicalUrl(mockState)).toEqual('/moskva/cars/ford/ecosport/2018-year/20104320/all/do-1500000/');
    });

    it('вырезал из url фильтры так как меньше 3 оферов', () => {
        mockState.listing.data.offers = [ {} ];
        mockState.listing.data.search_parameters.price_to = 1000000;
        mockState.listingPriceRanges.data = [ { price_to: 1000000 } ];

        expect(getListingCanonicalUrl(mockState)).toEqual('/moskva/cars/ford/ecosport/20104320/all/');
    });

    it('не вырезал из url фильтры так как section === new', () => {
        mockState.listing.data.offers = [ {} ];
        mockState.listing.data.search_parameters.price_to = 1000000;
        mockState.listing.data.search_parameters.section = 'new';
        mockState.listingPriceRanges.data = [ { price_to: 1000000 } ];

        expect(getListingCanonicalUrl(mockState)).toEqual('/moskva/cars/ford/ecosport/2018-year/20104320/new/');
    });

    it('вырезал из url поколение так как меньше 3 оферов', () => {
        mockState.listing.data.offers = [ {} ];
        delete mockState.listing.data.search_parameters.year_from;
        delete mockState.listing.data.search_parameters.year_to;

        expect(getListingCanonicalUrl(mockState)).toEqual('/moskva/cars/ford/ecosport/all/');
    });

    it('вырезал из url гео так как меньше 3 оферов', () => {
        mockState.listing.data.offers = [ {} ];
        mockState.listing.data.search_parameters.catalog_filter = [ { mark: 'FORD', model: 'ECOSPORT' } ];
        delete mockState.listing.data.search_parameters.year_from;
        delete mockState.listing.data.search_parameters.year_to;

        expect(getListingCanonicalUrl(mockState)).toEqual('/cars/ford/ecosport/all/');
    });

    it('вырезал из url поколение так нет офферов и ручка сброса фильтров не смогла сбросить параметры', () => {
        mockState.listing.data.offers = [];
        mockState.listing.data.search_parameters = {
            category: 'cars',
            catalog_filter: [ {
                mark: 'BMW',
                model: '340',
                generation: '20654943',
            } ],
            section: 'all',
        };

        expect(getListingCanonicalUrl(mockState)).toEqual('/moskva/cars/bmw/340/all/');
    });

    it('вырезал из url гео так нет офферов и ручка сброса фильтров не смогла сбросить параметры', () => {
        mockState.listing.data.offers = [];
        mockState.listing.data.search_parameters = {
            category: 'cars',
            catalog_filter: [ {
                mark: 'BMW',
                model: '340',
            } ],
            section: 'all',
        };

        expect(getListingCanonicalUrl(mockState)).toEqual('/cars/bmw/340/all/');
    });

    it('вырезал из url цену так как цена не валидна по списку priceRange', () => {
        mockState.listing.data.search_parameters.price_to = 1000000;

        expect(getListingCanonicalUrl(mockState)).toEqual('/moskva/cars/ford/ecosport/2018-year/20104320/all/');
    });

    it('вырезал из url гео и год, если выбрана марка, которая не выпускается в этом году', () => {
        mockState.geo.gids = [ 51 ];
        mockState.geo.geoAlias = 'samara';
        mockState.listing.data.search_parameters.section = 'new';
        mockState.listing.data.search_parameters.year_from = 2015;
        mockState.listing.data.search_parameters.year_to = 2015;
        mockState.listing.data.search_parameters.catalog_filter = [ {
            mark: 'FORD',
            model: 'SCORPIO',
        } ];
        mockState.breadcrumbsPublicApi.data = mockState.breadcrumbsPublicApi.data
            .filter((entity) => [ 'MARK_LEVEL', 'MODEL_LEVEL' ].includes(entity.level));

        expect(getListingCanonicalUrl(mockState)).toEqual('/cars/ford/scorpio/used/');
    });

    it('вырезал из url гео, если это /rossiya', () => {
        mockState.geo.gids = [ 225 ];

        expect(getListingCanonicalUrl(mockState)).toEqual('/cars/ford/ecosport/2018-year/20104320/all/');
    });

    it('поменял url на /used, если выбрана марка, которая более не выпускается', () => {
        mockState.listing.data.offers = [];
        mockState.listing.data.search_parameters.section = 'all';
        mockState.listing.data.search_parameters.year_from = undefined;
        mockState.listing.data.search_parameters.year_to = undefined;
        mockState.listing.data.search_parameters.catalog_filter = [ {
            mark: 'SAAB',
        } ];
        mockState.breadcrumbsPublicApi.data = mockState.breadcrumbsPublicApi.data
            .filter((entity) => entity.level === 'MARK_LEVEL');

        expect(getListingCanonicalUrl(mockState)).toEqual('/moskva/cars/saab/used/');
    });

    it('поменял url на /used, если выбрана модель, которая более не выпускается', () => {
        mockState.listing.data.offers = [];
        mockState.listing.data.search_parameters.section = 'all';
        mockState.listing.data.search_parameters.year_from = undefined;
        mockState.listing.data.search_parameters.year_to = undefined;
        mockState.listing.data.search_parameters.catalog_filter = [ {
            mark: 'FORD',
            model: 'SCORPIO',
        } ];
        mockState.breadcrumbsPublicApi.data = mockState.breadcrumbsPublicApi.data
            .filter((entity) => [ 'MARK_LEVEL', 'MODEL_LEVEL' ].includes(entity.level));

        expect(getListingCanonicalUrl(mockState)).toEqual('/moskva/cars/ford/scorpio/used/');
    });

    it('не должен учитывать configuration и tech_param', () => {
        mockState.listing.data.search_parameters.catalog_filter[0].configuration = '111';
        mockState.listing.data.search_parameters.catalog_filter[0].tech_param = '222';

        expect(getListingCanonicalUrl(mockState)).toEqual('/moskva/cars/ford/ecosport/2018-year/20104320/all/');
    });

    it('не должен добавляться лишний GET параметр category=moto', () => {
        mockState.listing.data.search_parameters = {
            category: 'moto',
            moto_category: 'MOTORCYCLE',
        };

        expect(getListingCanonicalUrl(mockState)).toEqual('/moskva/motorcycle/all/');
    });

    it('не должен добавляться лишний GET параметр category=trucks', () => {
        mockState.listing.data.search_parameters = {
            category: 'trucks',
            trucks_category: 'LCV',
        };

        expect(getListingCanonicalUrl(mockState)).toEqual('/moskva/lcv/all/');
    });

    it('убираем шильд в случае бесшильдовой модификации', () => {
        MockDate.set('2020-08-30');
        mockState.listing.data.search_parameters = {
            category: 'cars',
            catalog_filter: [
                {
                    mark: 'AUDI',
                    model: 'A8',
                    nameplate_name: 'a8',
                },
            ],
            section: 'all',
        };

        expect(getListingCanonicalUrl(mockState)).toEqual('/moskva/cars/audi/a8/all/');
    });

    it('убираем шильд в случае бесшильдовой модификации, поколение должно остаться', () => {
        MockDate.set('2021-08-12');
        mockState.listing.data.search_parameters = {
            category: 'cars',
            catalog_filter: [
                {
                    mark: 'AUDI',
                    model: 'A8',
                    nameplate_name: 'a8',
                    generation: '20071435',
                },
            ],
            section: 'all',
        };

        expect(getListingCanonicalUrl(mockState)).toEqual('/moskva/cars/audi/a8/20071435/all/');
    });

    it('убираем шильд в случае бесшильдовой модификации, поколение и тип кузова должны остаться', () => {
        MockDate.set('2021-08-12');
        mockState.listing.data.search_parameters = {
            category: 'cars',
            body_type_group: [ 'SEDAN' ],
            catalog_filter: [
                {
                    mark: 'AUDI',
                    model: 'A8',
                    nameplate_name: 'a8',
                    generation: '20071435',
                },
            ],
            section: 'all',
        };

        expect(getListingCanonicalUrl(mockState)).toEqual('/moskva/cars/audi/a8/20071435/all/body-sedan/');
    });

    it('оставляем шильд, если модификация не бесшильдовая', () => {
        MockDate.set('2020-08-30');
        mockState.listing.data.search_parameters = {
            category: 'cars',
            catalog_filter: [
                {
                    mark: 'AUDI',
                    model: 'A8',
                    nameplate_name: 'long',
                },
            ],
            section: 'all',
        };

        expect(getListingCanonicalUrl(mockState)).toEqual('/moskva/cars/audi/a8-long/all/');
    });

    it('если прилетает много чего, то оставляем mark, model+nameplate_name и generation и фильтр чпу(в зависимости от их приоритета)', () => {
        mockState.listing.data.search_parameters = {
            category: 'cars',
            catalog_filter: [
                {
                    mark: 'AUDI',
                    model: 'A8',
                    nameplate_name: 'long',
                    generation: '20071435',
                },
            ],
            section: 'all',
            engine_group: 'GASOLINE',
            body_type_group: [ 'SEDAN' ],
        };

        expect(getListingCanonicalUrl(mockState)).toEqual('/moskva/cars/audi/a8-long/20071435/all/body-sedan/');
    });

    it('группа кузовов внедорожники должна вернуть общее название кузова body-allroad', () => {
        mockState.listing.data.search_parameters = {
            category: 'cars',
            catalog_filter: [
                {
                    mark: 'LEXUS',
                    model: 'LX',
                    generation: '7962034',
                    nameplate_name: '450',
                },
            ],
            section: 'all',
            body_type_group: [ 'ALLROAD_3_DOORS', 'ALLROAD_5_DOORS', 'ALLROAD' ],
        };

        expect(getListingCanonicalUrl(mockState)).toEqual('/moskva/cars/lexus/lx-450/7962034/all/body-allroad/');
    });

    it('если у нас пустая выдача, то сбрасываем фильтр и ведем на уровень выше', () => {
        mockState.listing.data.offers = [];
        mockState.listing.data.resetParams = {
            category: 'cars',
            catalog_filter: [
                {
                    mark: 'LEXUS',
                    model: 'LX',
                    generation: '7962034',
                    nameplate_name: '450',
                },
            ],
            section: 'all',
        };
        mockState.listing.data.search_parameters = {
            category: 'cars',
            catalog_filter: [
                {
                    mark: 'LEXUS',
                    model: 'LX',
                    generation: '7962034',
                    nameplate_name: '450',
                },
            ],
            section: 'all',
            body_type_group: [ 'ALLROAD_3_DOORS', 'ALLROAD_5_DOORS', 'ALLROAD' ],
        };

        expect(getListingCanonicalUrl(mockState)).toEqual('/moskva/cars/lexus/lx-450/7962034/all/');
    });

    it('если выбрано 2 фильтра и они выводятся по чпу, выводим чпу по первому фильтру', () => {
        MockDate.set('2021-12-01');
        const CURRENT_YEAR = new Date().getFullYear();
        mockState.listing.data.search_parameters = {
            category: 'cars',
            catalog_filter: [
                {
                    mark: 'LEXUS',
                    model: 'LX',
                    generation: '7962034',
                    nameplate_name: '450',
                },
            ],
            section: 'new',
            body_type_group: [ 'ALLROAD_3_DOORS', 'ALLROAD_5_DOORS', 'ALLROAD' ],
            year_from: CURRENT_YEAR,
            year_to: CURRENT_YEAR,
        };

        expect(getListingCanonicalUrl(mockState)).toEqual(`/moskva/cars/lexus/lx-450/${ CURRENT_YEAR }-year/7962034/new/body-allroad/`);
    });

    it('Если год от и до совпадают и меньше "текущего года минус 4, section all и new должны вести на used', () => {
        mockState.listing.data.offers = [];
        mockState.listing.data.search_parameters = {
            category: 'cars',
            catalog_filter: [
                {
                    mark: 'LEXUS',
                    model: 'LX',
                    generation: '7962034',
                    nameplate_name: '450',
                },
            ],
            section: 'used',
            year_from: 2016,
            year_to: 2016,
        };

        expect(getListingCanonicalUrl(mockState)).toEqual('/moskva/cars/lexus/lx-450/7962034/used/');
    });

    it('Если год от и до совпадают и меньше "текущего года минус 4, но есть офферы, оставляем как есть', () => {
        mockState.listing.data.search_parameters = {
            category: 'cars',
            catalog_filter: [
                {
                    mark: 'LEXUS',
                    model: 'LX',
                    generation: '7962034',
                    nameplate_name: '450',
                },
            ],
            section: 'all',
            year_from: 2016,
            year_to: 2016,
        };

        expect(getListingCanonicalUrl(mockState)).toEqual('/moskva/cars/lexus/lx-450/2016-year/7962034/all/');
    });

    it('Должен простроить ссылку в регионе', () => {
        mockState.geo.geoAlias = 'samara';
        mockState.listing.data.search_parameters = {
            category: 'cars',
            catalog_filter: [
                {
                    mark: 'LEXUS',
                    model: 'LX',
                    generation: '7962034',
                    nameplate_name: '450',
                },
            ],
            section: 'all',
            year_from: 2016,
            year_to: 2016,
        };

        expect(getListingCanonicalUrl(mockState)).toEqual('/samara/cars/lexus/lx-450/2016-year/7962034/all/');
    });

    it('Должен остаться вендор', () => {
        mockState.listing.data.search_parameters = {
            category: 'cars',
            catalog_filter: [
                {
                    vendor: 'VENDOR2',
                },
            ],
            section: 'all',
            body_type_group: [ 'SEDAN' ],
            year_from: 2016,
            year_to: 2016,
        };

        expect(getListingCanonicalUrl(mockState)).toEqual('/moskva/cars/vendor-foreign/2016-year/all/body-sedan/');
    });
});

describe('amp', () => {
    it('отдал url без изменений', () => {
        expect(getListingCanonicalUrl(mockState, isAmp)).toEqual('/moskva/amp/cars/ford/ecosport/2018-year/20104320/all/');
    });

    it('вырезал из url год и гео, если год не подходит под комплектацию', () => {
        mockState.listing.data.search_parameters.year_from = 2019;
        mockState.listing.data.search_parameters.year_to = 2019;

        expect(getListingCanonicalUrl(mockState, isAmp)).toEqual('/amp/cars/ford/ecosport/20104320/all/');
    });

    it('не вырезал из url год и гео, если год подходит под комплектацию', () => {
        mockState.listing.data.search_parameters.year_from = 2018;
        mockState.listing.data.search_parameters.year_to = 2018;

        expect(getListingCanonicalUrl(mockState, isAmp)).toEqual('/moskva/amp/cars/ford/ecosport/2018-year/20104320/all/');

    });

    it('вырезал из url год и гео, если год не подходит под модель', () => {
        MockDate.set('2019-08-30');
        mockState.listing.data.offers = [];
        mockState.listing.data.search_parameters.year_from = 2013;
        mockState.listing.data.search_parameters.year_to = 2013;
        mockState.listing.data.search_parameters.catalog_filter = [ { mark: 'FORD', model: 'ECOSPORT' } ];

        expect(getListingCanonicalUrl(mockState, isAmp)).toEqual('/amp/cars/ford/ecosport/used/');
    });

    it('не вырезал из url год и гео, если год подходит под модель', () => {
        MockDate.set('2019-08-30');
        mockState.listing.data.search_parameters.year_from = 2018;
        mockState.listing.data.search_parameters.year_to = 2018;
        mockState.listing.data.search_parameters.catalog_filter = [ { mark: 'FORD', model: 'ECOSPORT' } ];

        expect(getListingCanonicalUrl(mockState, isAmp)).toEqual('/moskva/amp/cars/ford/ecosport/2018-year/all/');
    });

    it('не вырезал из url body_type_group(кузов)', () => {
        mockState.listing.data.search_parameters.body_type_group = [ 'ALLROAD_5_DOORS' ];

        expect(getListingCanonicalUrl(mockState, isAmp)).toEqual('/moskva/amp/cars/ford/ecosport/2018-year/20104320/all/body-allroad_5_doors/');
    });

    it('не вырезал из url gear_type(топливо)', () => {
        mockState.listing.data.search_parameters.engine_group = 'GASOLINE';

        expect(getListingCanonicalUrl(mockState, isAmp)).toEqual('/moskva/amp/cars/ford/ecosport/2018-year/20104320/all/engine-benzin/');
    });

    it('не вырезал из url color(цвет)', () => {
        mockState.listing.data.search_parameters.color = [ 'FAFBFB' ];

        expect(getListingCanonicalUrl(mockState, isAmp)).toEqual('/moskva/amp/cars/ford/ecosport/2018-year/20104320/all/color-belyj/');
    });

    it('не вырезал из url engine_group(привод)', () => {
        mockState.listing.data.search_parameters.gear_type = 'ALL_WHEEL_DRIVE';

        expect(getListingCanonicalUrl(mockState, isAmp)).toEqual('/moskva/amp/cars/ford/ecosport/2018-year/20104320/all/drive-4x4_wheel/');
    });

    it('не вырезал из url цену', () => {
        mockState.listing.data.search_parameters.price_to = 1000000;
        mockState.listingPriceRanges.data = [ { price_to: 1000000 } ];

        expect(getListingCanonicalUrl(mockState, isAmp)).toEqual('/moskva/amp/cars/ford/ecosport/2018-year/20104320/all/do-1000000/');
    });

    it('для невалидной цены строим каноникал на ближайшую большую валидную цену', () => {
        mockState.listing.data.search_parameters.price_to = 1_000_001;
        mockState.listingPriceRanges.data = [ { price_to: 1_000_000 }, { price_to: 1_500_000 } ];

        expect(getListingCanonicalUrl(mockState, isAmp)).toEqual('/moskva/amp/cars/ford/ecosport/2018-year/20104320/all/do-1500000/');
    });

    it('вырезал из url фильтры так как меньше 3 оферов', () => {
        mockState.listing.data.offers = [ {} ];
        mockState.listing.data.search_parameters.price_to = 1000000;
        mockState.listingPriceRanges.data = [ { price_to: 1000000 } ];

        expect(getListingCanonicalUrl(mockState, isAmp)).toEqual('/moskva/amp/cars/ford/ecosport/20104320/all/');
    });

    it('вырезал из url поколение так нет офферов и ручка сброса фильтров не смогла сбросить параметры', () => {
        mockState.listing.data.offers = [];
        mockState.listing.data.search_parameters = {
            category: 'cars',
            catalog_filter: [ {
                mark: 'BMW',
                model: '340',
                generation: '20654943',
            } ],
            section: 'all',
        };

        expect(getListingCanonicalUrl(mockState, isAmp)).toEqual('/moskva/amp/cars/bmw/340/all/');
    });

    it('вырезал из url гео так нет офферов и ручка сброса фильтров не смогла сбросить параметры', () => {
        mockState.listing.data.offers = [];
        mockState.listing.data.search_parameters = {
            category: 'cars',
            catalog_filter: [ {
                mark: 'BMW',
                model: '340',
            } ],
            section: 'all',
        };

        expect(getListingCanonicalUrl(mockState, isAmp)).toEqual('/amp/cars/bmw/340/all/');
    });

    it('вырезал из url цену так как цена не валидна по списку priceRange', () => {
        mockState.listing.data.search_parameters.price_to = 1000000;

        expect(getListingCanonicalUrl(mockState, isAmp)).toEqual('/moskva/amp/cars/ford/ecosport/2018-year/20104320/all/');
    });

    it('вырезал из url гео и год, если выбрана марка, которая не выпускается в этом году', () => {
        mockState.geo.gids = [ 51 ];
        mockState.geo.geoAlias = 'samara';
        mockState.listing.data.search_parameters.section = 'new';
        mockState.listing.data.search_parameters.year_from = 2015;
        mockState.listing.data.search_parameters.year_to = 2015;
        mockState.listing.data.search_parameters.catalog_filter = [ {
            mark: 'FORD',
            model: 'SCORPIO',
        } ];
        mockState.breadcrumbsPublicApi.data = mockState.breadcrumbsPublicApi.data
            .filter((entity) => [ 'MARK_LEVEL', 'MODEL_LEVEL' ].includes(entity.level));

        expect(getListingCanonicalUrl(mockState, isAmp)).toEqual('/amp/cars/ford/scorpio/used/');
    });

    it('вырезал из url гео, если это /rossiya', () => {
        mockState.geo.gids = [ 225 ];

        expect(getListingCanonicalUrl(mockState, isAmp)).toEqual('/amp/cars/ford/ecosport/2018-year/20104320/all/');
    });

    it('поменял url на /used, если выбрана марка, которая более не выпускается', () => {
        mockState.listing.data.offers = [];
        mockState.listing.data.search_parameters.section = 'all';
        mockState.listing.data.search_parameters.year_from = undefined;
        mockState.listing.data.search_parameters.year_to = undefined;
        mockState.listing.data.search_parameters.catalog_filter = [ {
            mark: 'SAAB',
        } ];
        mockState.breadcrumbsPublicApi.data = mockState.breadcrumbsPublicApi.data
            .filter((entity) => entity.level === 'MARK_LEVEL');

        expect(getListingCanonicalUrl(mockState, isAmp)).toEqual('/moskva/amp/cars/saab/used/');
    });

    it('поменял url на /used, если выбрана модель, которая более не выпускается', () => {
        mockState.listing.data.offers = [];
        mockState.listing.data.search_parameters.section = 'all';
        mockState.listing.data.search_parameters.year_from = undefined;
        mockState.listing.data.search_parameters.year_to = undefined;
        mockState.listing.data.search_parameters.catalog_filter = [ {
            mark: 'FORD',
            model: 'SCORPIO',
        } ];
        mockState.breadcrumbsPublicApi.data = mockState.breadcrumbsPublicApi.data
            .filter((entity) => [ 'MARK_LEVEL', 'MODEL_LEVEL' ].includes(entity.level));

        expect(getListingCanonicalUrl(mockState, isAmp)).toEqual('/moskva/amp/cars/ford/scorpio/used/');
    });

    it('не должен учитывать configuration и tech_param', () => {
        mockState.listing.data.search_parameters.catalog_filter[0].configuration = '111';
        mockState.listing.data.search_parameters.catalog_filter[0].tech_param = '222';

        expect(getListingCanonicalUrl(mockState, isAmp)).toEqual('/moskva/amp/cars/ford/ecosport/2018-year/20104320/all/');
    });

    it('не должен добавляться лишний GET параметр category=moto', () => {
        mockState.listing.data.search_parameters = {
            category: 'moto',
            moto_category: 'MOTORCYCLE',
        };

        expect(getListingCanonicalUrl(mockState, isAmp)).toEqual('/moskva/amp/motorcycle/all/');
    });

    it('не должен добавляться лишний GET параметр category=trucks', () => {
        mockState.listing.data.search_parameters = {
            category: 'trucks',
            trucks_category: 'LCV',
        };

        expect(getListingCanonicalUrl(mockState, isAmp)).toEqual('/moskva/amp/lcv/all/');

    });

    it('убираем шильд в случае бесшильдовой модификации', () => {
        MockDate.set('2020-08-30');
        mockState.listing.data.search_parameters = {
            category: 'cars',
            catalog_filter: [
                {
                    mark: 'AUDI',
                    model: 'A8',
                    nameplate_name: 'a8',
                },
            ],
            section: 'all',
        };

        expect(getListingCanonicalUrl(mockState, isAmp)).toEqual('/moskva/amp/cars/audi/a8/all/');
    });

    it('убираем шильд в случае бесшильдовой модификации, поколение должно остаться', () => {
        MockDate.set('2021-08-12');
        mockState.listing.data.search_parameters = {
            category: 'cars',
            catalog_filter: [
                {
                    mark: 'AUDI',
                    model: 'A8',
                    nameplate_name: 'a8',
                    generation: '20071435',
                },
            ],
            section: 'all',
        };

        expect(getListingCanonicalUrl(mockState, isAmp)).toEqual('/moskva/amp/cars/audi/a8/20071435/all/');
    });

    it('убираем шильд в случае бесшильдовой модификации, поколение и тип кузова должны остаться', () => {
        MockDate.set('2021-08-12');
        mockState.listing.data.search_parameters = {
            category: 'cars',
            body_type_group: [ 'SEDAN' ],
            catalog_filter: [
                {
                    mark: 'AUDI',
                    model: 'A8',
                    nameplate_name: 'a8',
                    generation: '20071435',
                },
            ],
            section: 'all',
        };

        expect(getListingCanonicalUrl(mockState, isAmp)).toEqual('/moskva/amp/cars/audi/a8/20071435/all/body-sedan/');
    });

    it('оставляем шильд, если модификация не бесшильдовая', () => {
        MockDate.set('2020-08-30');
        mockState.listing.data.search_parameters = {
            category: 'cars',
            catalog_filter: [
                {
                    mark: 'AUDI',
                    model: 'A8',
                    nameplate_name: 'long',
                },
            ],
            section: 'all',
        };

        expect(getListingCanonicalUrl(mockState, isAmp)).toEqual('/moskva/amp/cars/audi/a8-long/all/');
    });

    it('если прилетает много чего, то оставляем mark, model+nameplate_name и generation и фильтр чпу(в зависимости от их приоритета)', () => {
        mockState.listing.data.search_parameters = {
            category: 'cars',
            catalog_filter: [
                {
                    mark: 'AUDI',
                    model: 'A8',
                    nameplate_name: 'long',
                    generation: '20071435',
                },
            ],
            section: 'all',
            engine_group: 'GASOLINE',
            body_type_group: [ 'SEDAN' ],
        };

        expect(getListingCanonicalUrl(mockState, isAmp)).toEqual('/moskva/amp/cars/audi/a8-long/20071435/all/body-sedan/');
    });

    it('группа кузовов внедорожники должна вернуть общее название кузова body-allroad', () => {
        mockState.listing.data.search_parameters = {
            category: 'cars',
            catalog_filter: [
                {
                    mark: 'LEXUS',
                    model: 'LX',
                    generation: '7962034',
                    nameplate_name: '450',
                },
            ],
            section: 'all',
            body_type_group: [ 'ALLROAD_3_DOORS', 'ALLROAD_5_DOORS', 'ALLROAD' ],
        };

        expect(getListingCanonicalUrl(mockState, isAmp)).toEqual('/moskva/amp/cars/lexus/lx-450/7962034/all/body-allroad/');
    });

    it('если у нас пустая выдача, то сбрасываем фильтр и ведем на уровень выше', () => {
        mockState.listing.data.offers = [];
        mockState.listing.data.resetParams = {
            category: 'cars',
            catalog_filter: [
                {
                    mark: 'LEXUS',
                    model: 'LX',
                    generation: '7962034',
                    nameplate_name: '450',
                },
            ],
            section: 'all',
        };
        mockState.listing.data.search_parameters = {
            category: 'cars',
            catalog_filter: [
                {
                    mark: 'LEXUS',
                    model: 'LX',
                    generation: '7962034',
                    nameplate_name: '450',
                },
            ],
            section: 'all',
            body_type_group: [ 'ALLROAD_3_DOORS', 'ALLROAD_5_DOORS', 'ALLROAD' ],
        };

        expect(getListingCanonicalUrl(mockState, isAmp)).toEqual('/moskva/amp/cars/lexus/lx-450/7962034/all/');
    });

    it('если выбрано 2 фильтра и они выводятся по чпу, выводим чпу по первому фильтру', () => {
        MockDate.set('2021-12-01');
        const CURRENT_YEAR = new Date().getFullYear();
        mockState.listing.data.search_parameters = {
            category: 'cars',
            catalog_filter: [
                {
                    mark: 'LEXUS',
                    model: 'LX',
                    generation: '7962034',
                    nameplate_name: '450',
                },
            ],
            section: 'new',
            body_type_group: [ 'ALLROAD_3_DOORS', 'ALLROAD_5_DOORS', 'ALLROAD' ],
            year_from: CURRENT_YEAR,
            year_to: CURRENT_YEAR,
        };

        expect(getListingCanonicalUrl(mockState, isAmp)).toEqual(`/moskva/amp/cars/lexus/lx-450/${ CURRENT_YEAR }-year/7962034/new/body-allroad/`);
    });

    it('Если год от и до совпадают и меньше "текущего года минус 4, section all и new должны вести на used', () => {
        mockState.listing.data.offers = [];
        mockState.listing.data.search_parameters = {
            category: 'cars',
            catalog_filter: [
                {
                    mark: 'LEXUS',
                    model: 'LX',
                    generation: '7962034',
                    nameplate_name: '450',
                },
            ],
            section: 'all',
            year_from: 2016,
            year_to: 2016,
        };

        expect(getListingCanonicalUrl(mockState, isAmp)).toEqual('/moskva/amp/cars/lexus/lx-450/7962034/used/');
    });

    it('Если год от и до совпадают и меньше "текущего года минус 4, но есть офферы, оставляем как есть', () => {
        mockState.listing.data.search_parameters = {
            category: 'cars',
            catalog_filter: [
                {
                    mark: 'LEXUS',
                    model: 'LX',
                    generation: '7962034',
                    nameplate_name: '450',
                },
            ],
            section: 'all',
            year_from: 2016,
            year_to: 2016,
        };

        expect(getListingCanonicalUrl(mockState, isAmp)).toEqual('/moskva/amp/cars/lexus/lx-450/2016-year/7962034/all/');
    });

    it('Должен простроить ссылку в регионе', () => {
        mockState.geo.geoAlias = 'samara';
        mockState.listing.data.search_parameters = {
            category: 'cars',
            catalog_filter: [
                {
                    mark: 'LEXUS',
                    model: 'LX',
                    generation: '7962034',
                    nameplate_name: '450',
                },
            ],
            section: 'all',
            year_from: 2016,
            year_to: 2016,
        };

        expect(getListingCanonicalUrl(mockState, isAmp)).toEqual('/samara/amp/cars/lexus/lx-450/2016-year/7962034/all/');
    });

    it('Должен остаться вендор', () => {
        mockState.listing.data.search_parameters = {
            category: 'cars',
            catalog_filter: [
                {
                    vendor: 'VENDOR2',
                },
            ],
            section: 'all',
            body_type_group: [ 'SEDAN' ],
            year_from: 2016,
            year_to: 2016,
        };

        expect(getListingCanonicalUrl(mockState, isAmp)).toEqual('/moskva/amp/cars/vendor-foreign/2016-year/all/body-sedan/');
    });
});
