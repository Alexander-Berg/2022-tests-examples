const fillSearchParametersWithBreadcrumbs = require('./fillSearchParametersWithBreadcrumbs');

const breadcrumbsMock = require('auto-core/react/dataDomain/breadcrumbsPublicApi/mock').default;

it('должен добавить параметры поиска по configuration и tech_param из catalog_filter', () => {
    const result = {
        breadcrumbsPublicApi: breadcrumbsMock.value().data,
        listing: {
            search_parameters: {
                catalog_filter: [ {
                    mark: 'FORD',
                    model: 'ECOSPORT',
                    generation: '20104320',
                    configuration: '20104322',
                    tech_param: '20104324',
                } ],
            },
        },
    };
    const expectedResult = {
        breadcrumbsPublicApi: breadcrumbsMock.value().data,
        listing: {
            search_parameters: {
                catalog_filter: [ {
                    mark: 'FORD',
                    model: 'ECOSPORT',
                    generation: '20104320',
                    configuration: '20104322',
                    tech_param: '20104324',
                } ],
                body_type_group: [ 'ALLROAD_5_DOORS' ],
                displacement_from: 1600,
                displacement_to: 1600,
                engine_group: [ 'GASOLINE' ],
                gear_type: [ 'FORWARD_CONTROL' ],
                transmission: [ 'MECHANICAL' ],
            },
        },
    };
    expect(fillSearchParametersWithBreadcrumbs({ result })).toEqual(expectedResult);
});

it('при наличии мощности в tech_param добавит и ее тоже', () => {
    const result = {
        breadcrumbsPublicApi: breadcrumbsMock
            .withLevelEntities({
                level: 'TECH_PARAM_LEVEL',
                entities: [
                    {
                        id: '20104324',
                        count: 3,
                        name: '1.6 MT (122 л.с.)',
                        itemFilterParams: {
                            tech_param_id: '20104324',
                        },
                        tech_params: {
                            engine_type: 'GASOLINE',
                            displacement: 1596,
                            gear_type: 'FORWARD_CONTROL',
                            transmission: 'MECHANICAL',
                            power: 420,
                        },
                    },
                ],
            })
            .value().data,
        listing: {
            search_parameters: {
                catalog_filter: [ {
                    mark: 'FORD',
                    model: 'ECOSPORT',
                    generation: '20104320',
                    configuration: '20104322',
                    tech_param: '20104324',
                } ],
            },
        },
    };

    const res = fillSearchParametersWithBreadcrumbs({ result });
    expect(res.listing.search_parameters.power_from).toEqual(420);
    expect(res.listing.search_parameters.power_to).toEqual(420);
});
