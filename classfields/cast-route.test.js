/**
 * @jest-environment node
 */
jest.mock('auto-core/lib/core/isMobileApp', () => jest.fn());
const castRoute = require('auto-core/router/libs/cast-route');
const isMobileApp = require('auto-core/lib/core/isMobileApp');

const autoRuSusanin = require('auto-core/router/auto.ru/susanin');
const seoTestCases = require('auto-core/router/libs/castRoute.seo.testcases.js');
const geoUrl = require('auto-core/router/libs/geo-url.js');

const defaultOptions = {
    baseDomain: 'auto.ru',
    geoAlias: '',
    geoIds: [],
    geoOverride: false,
};

describe('card ->', () => {
    it('должен поменять роут на card-new для section=new, если есть tech_param_id и complectation_id', () => {
        const newRoute = castRoute('card', {
            complectation_id: '2',
            mark: 'audi',
            model: 'a4',
            sale_id: '123',
            sale_hash: '456',
            section: 'new',
            tech_param_id: '1',
        });
        expect(newRoute.routeName).toEqual('card-new');
    });

    it('должен поменять роут на card-new для section=new, если есть tech_param_id и complectation_id=0', () => {
        const newRoute = castRoute('card', {
            complectation_id: '0',
            mark: 'audi',
            model: 'a4',
            sale_id: '123',
            sale_hash: '456',
            section: 'new',
            tech_param_id: '1',
        });
        expect(newRoute.routeName).toEqual('card-new');
    });

    it('не должен поменять роут на card-new для section=new, если нет tech_param_id', () => {
        const newRoute = castRoute('card', {
            complectation_id: '1',
            mark: 'audi',
            model: 'a4',
            sale_id: '123',
            sale_hash: '456',
            section: 'new',
        });
        expect(newRoute.routeName).toEqual('card');
    });

    it('не должен поменять роут на card-new для section=new, если нет complectation_id', () => {
        const newRoute = castRoute('card', {
            mark: 'audi',
            model: 'a4',
            sale_id: '123',
            sale_hash: '456',
            section: 'new',
            tech_param_id: '1',
        });
        expect(newRoute.routeName).toEqual('card');
    });

    it('должен поменять moto_category на category', () => {
        const newRoute = castRoute('card', {
            mark: 'audi',
            model: 'a4',
            sale_id: '123',
            sale_hash: '456',
            section: 'used',
            category: 'moto',
            moto_category: 'atv',
        });

        expect(newRoute.routeName).toEqual('card');
        expect(newRoute.routeParams).toEqual({
            mark: 'audi',
            model: 'a4',
            sale_id: '123',
            sale_hash: '456',
            section: 'used',
            category: 'atv',
        });
    });

    it('должен поменять truck_category на category', () => {
        const newRoute = castRoute('card', {
            mark: 'audi',
            model: 'a4',
            sale_id: '123',
            sale_hash: '456',
            section: 'used',
            category: 'trucks',
            truck_category: 'truck',
        });

        expect(newRoute.routeName).toEqual('card');
        expect(newRoute.routeParams).toEqual({
            mark: 'audi',
            model: 'a4',
            sale_id: '123',
            sale_hash: '456',
            section: 'used',
            category: 'truck',
        });
    });

    it('должен поменять trucks_category на category', () => {
        const newRoute = castRoute('card', {
            mark: 'audi',
            model: 'a4',
            sale_id: '123',
            sale_hash: '456',
            section: 'used',
            category: 'trucks',
            trucks_category: 'truck',
        });

        expect(newRoute.routeName).toEqual('card');
        expect(newRoute.routeParams).toEqual({
            mark: 'audi',
            model: 'a4',
            sale_id: '123',
            sale_hash: '456',
            section: 'used',
            category: 'truck',
        });
    });
});

describe('card-group ->', () => {
    it('должен правильно строить ссылку на группу с каталог-фильтром без техпараметром и комплектаций', () => {
        const newRoute = castRoute('card-group', {
            catalog_filter: [ {
                mark: 'A',
                model: 'B',
                generation: '1',
                configuration: '2',
            } ],
        });
        expect(newRoute.routeParams).toEqual({
            mark: 'a',
            model: 'b',
            super_gen: '1',
            configuration_id: '2',
        });
    });

    it('должен правильно строить ссылку на группу с каталог-фильтром c техпараметром и комплектацией', () => {
        const newRoute = castRoute('card-group', {
            catalog_filter: [ {
                mark: 'A',
                model: 'B',
                generation: '1',
                configuration: '2',
                tech_param: '3',
                complectation_name: 'c',
            } ],
        });
        expect(newRoute.routeParams).toEqual({
            mark: 'a',
            model: 'b',
            super_gen: '1',
            configuration_id: '2',
            catalog_filter: [ 'mark=A,model=B,generation=1,configuration=2,tech_param=3,complectation_name=c' ],
        });
    });

    it('должен правильно строить ссылку на группу без каталог-фильтра', () => {
        const newRoute = castRoute('card-group', {
            mark: 'A',
            model: 'B',
            generation: '1',
            configuration: '2',
            tech_param: '3',
            complectation_name: 'c',
        });
        expect(newRoute.routeParams).toEqual({
            mark: 'a',
            model: 'b',
            generation: '1',
            configuration: '2',
            tech_param: '3',
            complectation_name: 'c',
        });
    });
});

describe('catalog ->', () => {
    it('should set route name "catalog-generation-listing", if "super_gen" exists', () => {
        const newRoute = castRoute('catalog', {
            mark: 'audi',
            model: 'a4',
            super_gen: '3242332',
        });
        expect(newRoute.routeName).toEqual('catalog-generation-listing');
    });
    it('should set route name "catalog-generation-listing", if "model" exists', () => {
        const newRoute = castRoute('catalog', {
            mark: 'audi',
            model: 'a4',
        });
        expect(newRoute.routeName).toEqual('catalog-generation-listing');
    });
    it('should set route name "catalog-model-listing", if "mark" exists only', () => {
        const newRoute = castRoute('catalog', {
            mark: 'audi',
        });
        expect(newRoute.routeName).toEqual('catalog-model-listing');
    });
    it('should set route name "catalog-index", if any mark_model_nameplate params exist', () => {
        const newRoute = castRoute('catalog', {});
        expect(newRoute.routeName).toEqual('catalog-index');
    });
    it('should set route name "catalog-index", if params are put in "link"', () => {
        const newRoute = castRoute('catalog');
        expect(newRoute.routeName).toEqual('catalog-index');
    });
    it('должен убрать section из урла, если нет sale_id', () => {
        const newRoute = castRoute('catalog', { section: 'all' });
        expect(newRoute).toEqual({
            routeName: 'catalog-index',
            routeParams: {},
        });
    });
    it('должен оставить section, sale_id и sale_hash', () => {
        const newRoute = castRoute('catalog', { sale_id: '123', sale_hash: 'abc', section: 'all', mark: 'audi', model: 'a4' });
        expect(newRoute).toEqual({
            routeName: 'catalog-generation-listing',
            routeParams: { sale_id: '123', sale_hash: 'abc', section: 'all', mark: 'audi', model: 'a4' },
        });
    });

    it('должен оставить from', () => {
        const newRoute = castRoute('catalog', { section: 'all', mark: 'audi', model: 'a4', from: 'top_menu' });
        expect(newRoute).toEqual({
            routeName: 'catalog-generation-listing',
            routeParams: { mark: 'audi', model: 'a4', from: 'top_menu' },
        });
    });
});

describe('catalog-generation-listing ->', () => {
    it('should convert route name "catalog-generation-listing" to "catalog-model-listing", if "model" doesn\'t exist', () => {
        const newRoute = castRoute('catalog-generation-listing', {
            mark: 'audi',
        });
        expect(newRoute.routeName).toEqual('catalog-model-listing');
    });

    it('shouldn\'t convert route name "catalog-generation-listing", if "model" exists', () => {
        const newRoute = castRoute('catalog-generation-listing', {
            mark: 'audi',
            model: 'a4',
        });
        expect(newRoute.routeName).toEqual('catalog-generation-listing');
    });
});

describe('dealer-page ->', () => {

    let routeParams;
    beforeEach(() => {
        routeParams = {
            category: 'cars',
            dealer_code: 'test',
        };
    });

    it('should not cast "dealer-page" to "dealer-page-official" if dealer_org_type != 1', () => {
        expect(castRoute('dealer-page', routeParams).routeName).toEqual('dealer-page');
    });

    it('should cast "dealer-page" to "dealer-page-official" if dealer_org_type === 1', () => {
        routeParams.dealer_org_type = 1;
        expect(castRoute('dealer-page', routeParams).routeName).toEqual('dealer-page-official');
    });

    it('should cast "dealer-page" to "dealer-page-official" and delete "dealer_org_type" if "dealer_org_type" === 1', () => {
        routeParams.dealer_org_type = 1;
        expect(castRoute('dealer-page', routeParams).routeParams).toEqual({
            category: 'cars',
            dealer_code: 'test',
        });
    });

});

describe('listing ->', () => {

    it('should lowercase moto_category param', () => {
        const newRoute = castRoute('moto-listing', {
            moto_category: 'MOTORCYCLE',
        });
        expect(newRoute.routeParams).toEqual({ moto_category: 'motorcycle' });
    });

    it('should lowercase mark and model params', () => {
        const newRoute = castRoute('listing', {
            mark: 'AUDI',
            model: 'A4',
        });
        expect(newRoute.routeParams).toEqual({ mark: 'audi', model: 'a4' });
    });

    it('should cast single "catalog_filter" with mark to "mark" params', () => {
        const newRoute = castRoute('listing', {
            catalog_filter: [ { mark: 'AUDI' } ],
        });
        expect(newRoute.routeParams).toEqual({ mark: 'audi' });
    });

    it('should ignore empty "catalog_filter"', () => {
        const newRoute = castRoute('listing', {
            catalog_filter: [ { mark: 'AUDI' }, {} ],
        });
        expect(newRoute.routeParams).toEqual({ mark: 'audi' });
    });

    it('should remove duplicate "catalog_filter"', () => {
        const newRoute = castRoute('listing', {
            catalog_filter: [ { mark: 'AUDI' }, { mark: 'AUDI' } ],
        });
        expect(newRoute.routeParams).toEqual({ mark: 'audi' });
    });

    it('should cast single "catalog_filter" with mark, model to "mark" and "model" params', () => {
        const newRoute = castRoute('listing', {
            catalog_filter: [ { mark: 'AUDI', model: 'A4' } ],
        });
        expect(newRoute.routeParams).toEqual({ mark: 'audi', model: 'a4' });
    });

    it('should cast single "catalog_filter" with mark, model, generation to "mark", "model" and "super_gen" params', () => {
        const newRoute = castRoute('listing', {
            catalog_filter: [ { mark: 'AUDI', model: 'A4', generation: '1' } ],
        });
        expect(newRoute.routeParams).toEqual({ mark: 'audi', model: 'a4', super_gen: '1' });
    });

    it('should cast single "catalog_filter" with mark, model, nameplate to "mark", "model" and "catalog_filter" params', () => {
        const newRoute = castRoute('listing', {
            catalog_filter: [ { mark: 'AUDI', model: 'A4', nameplate: '1' } ],
        });
        expect(newRoute.routeParams).toEqual({
            mark: 'audi',
            model: 'a4',
            catalog_filter: [ 'mark=AUDI,model=A4,nameplate=1' ],
        });
    });

    it('should cast single "catalog_filter" with mark, model, nameplate_name to "mark", "model" and "nameplate_name" params', () => {
        const newRoute = castRoute('listing', {
            catalog_filter: [ { mark: 'AUDI', model: 'A4', nameplate_name: '1a' } ],
        });
        expect(newRoute.routeParams).toEqual({
            mark: 'audi',
            model: 'a4',
            nameplate_name: '1a',
        });
    });

    it('should not cast array "catalog_filter"', () => {
        const newRoute = castRoute('listing', {
            catalog_filter: [ { mark: 'AUDI', model: 'A4' }, { mark: 'BMW', model: '3ER' } ],
        });
        expect(newRoute.routeParams).toEqual({
            catalog_filter: [ 'mark=AUDI,model=A4', 'mark=BMW,model=3ER' ],
        });
    });

    it('should not cast array "exclude_catalog_filter"', () => {
        const newRoute = castRoute('listing', {
            exclude_catalog_filter: [ { mark: 'AUDI', model: 'A4' }, { mark: 'BMW', model: '3ER' } ],
        });
        expect(newRoute.routeParams).toEqual({
            exclude_catalog_filter: [ 'mark=AUDI,model=A4', 'mark=BMW,model=3ER' ],
        });
    });

    it('should remove bad params for "new" section', () => {
        const newRoute = castRoute('listing', {
            section: 'new',
            price_to: '1000000',
            km_age_to: '1000000',
        });
        expect(newRoute.routeParams).toEqual({
            section: 'new',
            price_to: '1000000',
        });
    });

    it('should remove bad params for "used" section', () => {
        const newRoute = castRoute('listing', {
            section: 'used',
            price_to: '1000000',
            km_age_to: '1000000',
            official_dealer: 'true',
        });
        expect(newRoute.routeParams).toEqual({
            section: 'used',
            price_to: '1000000',
            km_age_to: '1000000',
        });
    });

    it('should remove bad params for "all" section', () => {
        const newRoute = castRoute('listing', {
            category: 'cars',
            section: 'all',
            price_to: '1000000',
            km_age_to: '1000000',
            official_dealer: 'true',
        });
        expect(newRoute.routeParams).toEqual({
            category: 'cars',
            section: 'all',
            price_to: '1000000',
            km_age_to: '1000000',
        });
    });

    it('should remove empty path params', () => {
        const newRoute = castRoute('listing', {
            state: 'all',
            catalog_filter: [ { mark: 'AUDI', model: 'A5' } ],
            mark: '',
            model: '',
            super_gen: '',
        });
        expect(newRoute.routeParams).toEqual({
            mark: 'audi',
            model: 'a5',
            state: 'all',
        });
    });

    it('должен вернуть параметр year, если передали одинаковые year_from и year_to', () => {
        const newRoute = castRoute('listing', {
            catalog_filter: [ { mark: 'AUDI', model: 'A5' } ],
            year_from: 2018,
            year_to: 2018,
            state: 'all',
        });
        expect(newRoute).toEqual({
            routeName: 'listing',
            routeParams: {
                mark: 'audi',
                model: 'a5',
                state: 'all',
                year: '2018-year',
            },
        });
    });

    it('должен правильно вернуть параметр body_type_sef, если есть body_type_group и существует такая комплектация', () => {
        const newRoute = castRoute('listing', {
            catalog_filter: [ { mark: 'AUDI', model: 'A5' } ],
            category: 'cars',
            body_type_group: [ 'COUPE' ],
            state: 'all',
        });
        expect(newRoute).toEqual({
            routeName: 'listing',
            routeParams: {
                category: 'cars',
                state: 'all',
                mark: 'audi',
                model: 'a5',
                body_type_sef: 'body-coupe',
            },
        });
    });

    it('должен правильно вернуть параметр do, если есть флаг isValidPriceTo = true', () => {
        const newRoute = castRoute('listing', {
            catalog_filter: [ { mark: 'BMW', model: '3ER', nameplate_name: '320' } ],
            section: 'all',
            category: 'cars',
            price_to: 1000000,
        }, [ 213 ], { isValidPriceTo: true });
        expect(newRoute).toEqual({
            routeName: 'listing',
            routeParams: {
                mark: 'bmw',
                model: '3er',
                nameplate_name: '320',
                category: 'cars',
                'do': 1000000,
                section: 'all',
            },
        });
    });

    it('должен правильно вернуть параметр price_to, если есть флаг isValidPriceTo = false', () => {
        const newRoute = castRoute('listing', {
            catalog_filter: [ { mark: 'BMW', model: '3ER', nameplate_name: '320' } ],
            section: 'all',
            category: 'cars',
            price_to: 1000000,
        }, [ 213 ], { isValidPriceTo: false });
        expect(newRoute).toEqual({
            routeName: 'listing',
            routeParams: {
                mark: 'bmw',
                model: '3er',
                nameplate_name: '320',
                category: 'cars',
                price_to: 1000000,
                section: 'all',
            },
        });
    });

    it('не должен вернуть параметр do если есть price_to и валюта не рубли', () => {
        const newRoute = castRoute('listing', {
            section: 'all',
            category: 'cars',
            price_to: 100000,
            currency: 'USD',
        });
        expect(newRoute).toEqual({
            routeName: 'listing',
            routeParams: {
                category: 'cars',
                price_to: 100000,
                section: 'all',
                currency: 'USD',
            },
        });
    });

    it('не должен вернуть параметр body_type_sef, если есть body_type_group и не существует такой комплектации', () => {
        const newRoute = castRoute('listing', {
            catalog_filter: [ { mark: 'AUDI', model: 'A5' } ],
            body_type_group: [ 'UNIVERSAL' ],
            state: 'all',
        });
        expect(newRoute).toEqual({
            routeName: 'listing',
            routeParams: {
                mark: 'audi',
                model: 'a5',
                state: 'all',
                body_type_group: [ 'UNIVERSAL' ],
            },
        });
    });

    it('должен вернуть body_type_sef в любом регионе, если указан body_type_group и не указаны mark и model', () => {
        const newRoute = castRoute('listing', {
            body_type_group: [ 'WAGON' ],
            state: 'all',
            category: 'cars',
        }, [ 1106, 1, 2, 213 ]);
        expect(newRoute).toEqual({
            routeName: 'listing',
            routeParams: {
                state: 'all',
                body_type_sef: 'body-wagon',
                category: 'cars',
            },
        });
    });

    it('не должен вернуть body_type_sef в нескольких регионах, если указан body_type_group и указан mark', () => {
        const newRoute = castRoute('listing', {
            body_type_group: [ 'WAGON' ],
            state: 'all',
            category: 'cars',
            mark: 'AUDI',
        }, [ 1106, 1, 2, 213 ]);
        expect(newRoute).toEqual({
            routeName: 'listing',
            routeParams: {
                state: 'all',
                body_type_group: [ 'WAGON' ],
                category: 'cars',
                mark: 'audi',
            },
        });
    });

    it('должен правильно вернуть параметр color_sef, если есть color и он один один из часто встречающихся', () => {
        const newRoute = castRoute('listing', {
            catalog_filter: [ { mark: 'AUDI' } ],
            color: [ '040001' ],
            section: 'all',
            state: 'all',
            category: 'cars',
        }, [ 213 ]);
        expect(newRoute).toEqual({
            routeName: 'listing',
            routeParams: {
                category: 'cars',
                mark: 'audi',
                section: 'all',
                state: 'all',
                color_sef: 'color-chernyj',
            },
        });
    });

    it('должен правильно вернуть параметр engine_type_sef, если есть правильный engine_group', () => {
        const newRoute = castRoute('listing', {
            engine_group: [ 'DIESEL' ],
            section: 'all',
            state: 'all',
            category: 'cars',
        });
        expect(newRoute).toEqual({
            routeName: 'listing',
            routeParams: {
                category: 'cars',
                state: 'all',
                section: 'all',
                engine_type_sef: 'engine-dizel',
            },
        });
    });

    it('должен правильно вернуть параметр drive_sef, если есть gear_type и существует такая комплектация', () => {
        const newRoute = castRoute('listing', {
            catalog_filter: [ { mark: 'AUDI', model: 'A5' } ],
            category: 'cars',
            gear_type: [ 'FORWARD_CONTROL' ],
            section: 'all',
        });
        expect(newRoute).toEqual({
            routeName: 'listing',
            routeParams: {
                category: 'cars',
                section: 'all',
                mark: 'audi',
                model: 'a5',
                drive_sef: 'drive-forward_wheel',
            },
        });
    });

    it('должен вернуть параметр drive_sef для section used', () => {
        const newRoute = castRoute('listing', {
            catalog_filter: [ { mark: 'AUDI', model: 'A5' } ],
            category: 'cars',
            gear_type: [ 'FORWARD_CONTROL' ],
            section: 'used',
        });
        expect(newRoute).toEqual({
            routeName: 'listing',
            routeParams: {
                category: 'cars',
                mark: 'audi',
                model: 'a5',
                section: 'used',
                drive_sef: 'drive-forward_wheel',
            },
        });
    });

    it('не должен вернуть параметр drive_sef, если есть gear_type и такая комплектация отсутствует', () => {
        const newRoute = castRoute('listing', {
            catalog_filter: [ { mark: 'AUDI', model: 'A5' } ],
            category: 'cars',
            gear_type: [ 'REAR_DRIVE' ],
            section: 'all',
        });
        expect(newRoute).toEqual({
            routeName: 'listing',
            routeParams: {
                category: 'cars',
                section: 'all',
                mark: 'audi',
                model: 'a5',
                gear_type: [ 'REAR_DRIVE' ],
            },
        });
    });

    it('должен удалить category из параметров при наличии trucks_category в desktop', () => {
        isMobileApp.mockImplementation(() => false);
        expect(castRoute('listing', {
            category: 'trucks',
            trucks_category: 'lcv',
            section: 'new',
        })).toEqual(
            {
                routeName: 'commercial-listing',
                routeParams: {
                    section: 'new',
                    trucks_category: 'lcv',
                },
            },
        );
    });

    it('должен удалить category из параметров при наличии trucks_category в mobile', () => {
        jest.resetModules();
        isMobileApp.mockImplementation(() => true);
        expect(castRoute('listing', {
            category: 'trucks',
            trucks_category: 'lcv',
            section: 'new',
        })).toEqual(
            {
                routeName: 'commercial-listing',
                routeParams: {
                    section: 'new',
                    trucks_category: 'lcv',
                },
            },
        );
    });

    describe('Проверка ЧПУ', () => {
        // Листинг валидный, если в нем достаточное кол-во офферов

        describe('ссылки на валидный листинг', () => {
            const isValidOffersNumber = true;

            seoTestCases.forEach((testCase) => {
                const caseTest = { ...testCase };
                const section = caseTest.section;
                const { generation, mark, model } = caseTest.catalog_filter[0];
                // eslint-disable-next-line @typescript-eslint/naming-convention
                const { body_type_group, color, engine_group, gear_type, price_to } = caseTest;
                const mmmName = [ mark, model, generation ].filter(Boolean).join(' ');
                const filterParams = [ body_type_group, color, engine_group, gear_type, price_to ].filter(Boolean).flat().join(' ');

                it(`${ section } ${ mmmName || 'без марки' } с параметрами ${ filterParams }`, () => {
                    const newRoute = castRoute('listing', caseTest, [], { isValidOffersNumber, isValidPriceTo: true });

                    const route = geoUrl.generate(autoRuSusanin, newRoute, defaultOptions);
                    expect(route).toMatchSnapshot();
                });
            });
        });

        describe('ссылки на валидный листинг c невалидной ценой', () => {
            const isValidOffersNumber = true;
            seoTestCases.forEach((testCase) => {
                const caseTest = { ...testCase };
                const section = caseTest.section;
                const { generation, mark, model } = caseTest.catalog_filter[0];
                // eslint-disable-next-line @typescript-eslint/naming-convention
                const { body_type_group, color, engine_group, gear_type, price_to } = caseTest;
                const mmmName = [ mark, model, generation ].filter(Boolean).join(' ');
                const filterParams = [ body_type_group, color, engine_group, gear_type, price_to ].filter(Boolean).flat().join(' ');

                it(`${ section } ${ mmmName || ' без марки' } с параметрами ${ filterParams }`, () => {
                    const newRoute = castRoute('listing', caseTest, null, { isValidOffersNumber, isValidPriceTo: false });

                    expect(newRoute.routeParams.price_to).toBe(testCase.price_to);
                    expect(newRoute.routeParams['do']).toBeUndefined();
                });
            });
        });

        describe('ссылки на невалидный листинг (мало офферов)', () => {
            const isValidOffersNumber = false;

            seoTestCases.forEach((testCase) => {
                const caseTest = { ...testCase };
                const section = caseTest.section;
                const { generation, mark, model } = caseTest.catalog_filter[0];
                // eslint-disable-next-line @typescript-eslint/naming-convention
                const { body_type_group, color, engine_group, gear_type } = caseTest;
                const mmmName = [ mark, model, generation ].filter(Boolean).join(' ');
                const filterParams = [ body_type_group, color, engine_group, gear_type ].filter(Boolean).flat().join(' ');

                it(`${ section } ${ mmmName || ' без марки' } с параметрами ${ filterParams }`, () => {
                    const newRoute = castRoute('listing', caseTest, null, { isValidOffersNumber });

                    // Проверяем, что параметры никак не изменились и не образовались ЧПУ
                    expect(newRoute.routeParams.mark).toBe(testCase.catalog_filter[0].mark?.toLowerCase());
                    expect(newRoute.routeParams.model).toBe(testCase.catalog_filter[0].model?.toLowerCase());
                    expect(newRoute.routeParams.super_gen).toBe(testCase.catalog_filter[0].generation);
                    expect(newRoute.routeParams.category).toBe(testCase.category);
                    expect(newRoute.routeParams.section).toBe(testCase.section);

                    expect(newRoute.routeParams.body_type_group).toBe(testCase.body_type_group);
                    expect(newRoute.routeParams.engine_group).toBe(testCase.engine_group);
                    expect(newRoute.routeParams.gear_type).toBe(testCase.gear_type);
                    expect(newRoute.routeParams.color).toBe(testCase.color);
                    expect(newRoute.routeParams.price_to).toBe(testCase.price_to);
                });
            });
        });

        describe('Порядок параметров', () => {
            it('должен вернуть параметр body_type_sef, если есть body_type_group и engine_group (body_type_group важнее engine_group)', () => {
                const newRoute = castRoute('listing', {
                    catalog_filter: [ { mark: 'AUDI', model: 'A5' } ],
                    category: 'cars',
                    body_type_group: [ 'COUPE' ],
                    state: 'all',
                    engine_group: [ 'DIESEL' ],
                });
                expect(newRoute).toEqual({
                    routeName: 'listing',
                    routeParams: {
                        category: 'cars',
                        state: 'all',
                        mark: 'audi',
                        model: 'a5',
                        body_type_sef: 'body-coupe',
                        engine_group: [ 'DIESEL' ],
                    },
                });
            });

            it('должен вернуть параметр engine_type_sef, если есть engine_group и color (engine_group важнее color)', () => {
                const newRoute = castRoute('listing', {
                    engine_group: [ 'DIESEL' ],
                    section: 'all',
                    state: 'all',
                    category: 'cars',
                    color: [ '040001' ],
                });
                expect(newRoute).toEqual({
                    routeName: 'listing',
                    routeParams: {
                        category: 'cars',
                        state: 'all',
                        section: 'all',
                        engine_type_sef: 'engine-dizel',
                        color: [ '040001' ],
                    },
                });
            });

            it('должен вернуть параметр color_sef, если есть color и gear_type (color важнее gear_type)', () => {
                const newRoute = castRoute('listing', {
                    catalog_filter: [ { mark: 'AUDI' } ],
                    color: [ '040001' ],
                    section: 'all',
                    state: 'all',
                    category: 'cars',
                    gear_type: [ 'FORWARD_CONTROL' ],
                }, [ 213 ]);
                expect(newRoute).toEqual({
                    routeName: 'listing',
                    routeParams: {
                        category: 'cars',
                        mark: 'audi',
                        section: 'all',
                        state: 'all',
                        color: [ '040001' ],
                        drive_sef: 'drive-forward_wheel',
                    },
                });
            });

            it('должен вернуть параметр drive_sef, если есть gear_type и price_to (gear_type важнее price_to)', () => {
                const newRoute = castRoute('listing', {
                    catalog_filter: [ { mark: 'AUDI', model: 'A5' } ],
                    category: 'cars',
                    gear_type: [ 'FORWARD_CONTROL' ],
                    section: 'all',
                    price_to: 600000,
                });
                expect(newRoute).toEqual({
                    routeName: 'listing',
                    routeParams: {
                        category: 'cars',
                        section: 'all',
                        mark: 'audi',
                        model: 'a5',
                        drive_sef: 'drive-forward_wheel',
                        price_to: 600000,
                    },
                });
            });
        });
    });

});

describe('dealers-listing', () => {
    it('должен оставить марку в секции new', () => {
        jest.resetModules();
        isMobileApp.mockImplementation(() => false);
        const newRoute = castRoute('dealers-listing', {
            mark: 'BMW',
            section: 'new',
        });
        expect(newRoute).toEqual({
            routeName: 'dealers-listing',
            routeParams: {
                mark: 'bmw',
                section: 'new',
            },
        });
    });

    it('должен оставить марку, если секция не указана (по умолчанию new)', () => {
        jest.resetModules();
        isMobileApp.mockImplementation(() => false);
        const newRoute = castRoute('dealers-listing', {
            mark: 'BMW',
        });
        expect(newRoute).toEqual({
            routeName: 'dealers-listing',
            routeParams: {
                mark: 'bmw',
            },
        });
    });

    it('должен убрать марку в секции used', () => {
        jest.resetModules();
        isMobileApp.mockImplementation(() => false);
        const newRoute = castRoute('dealers-listing', {
            mark: 'BMW',
            section: 'used',
        });
        expect(newRoute).toEqual({
            routeName: 'dealers-listing',
            routeParams: {
                section: 'used',
            },
        });
    });

    it('должен оставить марку в секции used в мобиле', () => {
        jest.resetModules();
        isMobileApp.mockImplementation(() => true);
        const newRoute = castRoute('dealers-listing', {
            mark: 'BMW',
            section: 'used',
        });
        expect(newRoute).toEqual({
            routeName: 'dealers-listing',
            routeParams: {
                mark: 'bmw',
                section: 'used',
            },
        });
    });
});

describe('proauto-report', () => {
    it('должен поменять кириллицу в history_entity_id, если это госномер', () => {
        const newRoute = castRoute('proauto-report', {
            history_entity_id: 'М176ЕМ199',
            from: 'shapka',
        });
        expect(newRoute.routeParams).toEqual({
            history_entity_id: 'M176EM199',
            from: 'shapka',
        });
    });

    it('не должен трогать кириллицу в history_entity_id, если это не госномер', () => {
        const newRoute = castRoute('proauto-report', {
            history_entity_id: 'М176ЕМ19910324435',
            from: 'shapka',
        });
        expect(newRoute.routeParams).toEqual({
            history_entity_id: 'М176ЕМ19910324435',
            from: 'shapka',
        });
    });

    it('Должен заменить price_to на параметр do, если выбрано section=all', () => {
        const newRoute = castRoute('listing', {
            catalog_filter: [ { mark: 'BMW', model: '5ER', nameplate_name: '540' } ],
            category: 'cars',
            section: 'all',
            price_to: '1000000',
        }, [ 213 ], { isValidPriceTo: true });
        expect(newRoute.routeParams).toEqual({
            category: 'cars',
            'do': '1000000',
            mark: 'bmw',
            model: '5er',
            nameplate_name: '540',
            section: 'all',
        });
    });

    it('Должен заменить price_to на параметр do для новых автомобилей', () => {
        const newRoute = castRoute('listing', {
            catalog_filter: [ { mark: 'BMW', model: '5ER', nameplate_name: '540' } ],
            category: 'cars',
            section: 'new',
            price_to: '1000000',
        }, [ 213 ], { isValidPriceTo: true });
        expect(newRoute.routeParams).toEqual({
            category: 'cars',
            'do': '1000000',
            mark: 'bmw',
            model: '5er',
            nameplate_name: '540',
            section: 'new',
        });
    });

    it('Должен заменить price_to на параметр do для автомобилей с пробегом', () => {
        const newRoute = castRoute('listing', {
            catalog_filter: [ { mark: 'BMW', model: '5ER', nameplate_name: '540' } ],
            category: 'cars',
            section: 'used',
            price_to: '1000000',
        }, [ 213 ], { isValidPriceTo: true });
        expect(newRoute.routeParams).toEqual({
            category: 'cars',
            'do': '1000000',
            mark: 'bmw',
            model: '5er',
            nameplate_name: '540',
            section: 'used',
        });
    });
});
