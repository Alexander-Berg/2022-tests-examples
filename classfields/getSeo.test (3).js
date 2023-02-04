const getSeo = require('./getSeo');
const breadcrumbsPublicApiMock = require('auto-core/react/dataDomain/breadcrumbsPublicApi/mocks/breadcrumbsPublicApi.mock');

describe('catalog-listing-specifications', () => {
    it('должен сгенерировать корректные title, description и canonical', () => {
        const result = getSeo({
            config: {
                data: {
                    pageType: 'catalog-listing-specifications',
                },
            },
            searchParams: {
                category: 'cars',
                mark: 'FORD',
                model: 'ECOSPORT',
                specification: 'razmer-koles',
            },
            breadcrumbsPublicApi: breadcrumbsPublicApiMock,
        });

        expect(result).toHaveProperty('title', 'Размер колес Ford EcoSport. Все характеристики автомобилей Форд ЭкоСпорт в каталоге Авто.ру');
        expect(result).toHaveProperty('description',
            'Все характеристики, комплектации и цены на автомобили Ford EcoSport на Авто.ру. ' +
            'Размер колес Форд ЭкоСпорт и аналогичных по классу машин в каталоге');
        expect(result).toHaveProperty('canonical', 'https://autoru_frontend.base_domain/catalog/cars/ford/ecosport/specifications/razmer-koles/');
    });
});
