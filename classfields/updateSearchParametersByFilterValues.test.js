const updateSearchParametersByFilterValues = require('./updateSearchParametersByFilterValues');

it('должен обновить поисковые параметры в соответствии со значениями фильтра', () => {
    const searchParameters = {
        catalog_filter: [
            {
                mark: 'A',
                model: 'B',
                generation: '666',
                configuration: '666',
                tech_param: '1111',
                complectation_name: 'Test2',
            },
        ],
        transmission: [ 'AUTOMATIC' ],
        gear_type: [ 'ALL_WHEEL_DRIVE' ],
        color: [ '926547' ],
        catalog_equipment: [ 'automatic-lighting-control' ],
    };

    const filterValues = {
        complectation_name: 'Test2',
        transmission: [ 'AUTOMATIC', 'MECHANICAL' ],
        gear_type: [],
        color: [],
        catalog_equipment: [],
        search_tag: [],
    };

    expect(updateSearchParametersByFilterValues(searchParameters, filterValues)).toEqual({
        catalog_filter: [
            {
                mark: 'A',
                model: 'B',
                generation: '666',
                configuration: '666',
                complectation_name: 'Test2',
            },
        ],
        transmission: [ 'AUTOMATIC', 'MECHANICAL' ],
        gear_type: [],
        color: [],
        search_tag: [],
        catalog_equipment: [],
    });
});

it('должен добавить комплектацаию, если уже выбрана другая', () => {
    const searchParameters = {
        catalog_filter: [
            {
                mark: 'A',
                model: 'B',
                generation: '666',
                configuration: '666',
                tech_param: '1111',
                complectation_name: 'Test',
            },
        ],
        transmission: [ 'AUTOMATIC' ],
        gear_type: [ 'ALL_WHEEL_DRIVE' ],
        color: [ '926547' ],
        catalog_equipment: [ 'automatic-lighting-control' ],
    };

    const filterValues = {
        complectation_name: 'Test2',
        transmission: [ 'AUTOMATIC' ],
        gear_type: [],
        color: [],
        catalog_equipment: [],
        search_tag: [],
    };

    expect(updateSearchParametersByFilterValues(searchParameters, filterValues, { complectationFilter: true })).toEqual({
        catalog_filter: [
            {
                mark: 'A',
                model: 'B',
                generation: '666',
                configuration: '666',
                complectation_name: 'Test',
            },
            {
                mark: 'A',
                model: 'B',
                generation: '666',
                configuration: '666',
                complectation_name: 'Test2',
            },
        ],
        transmission: [ 'AUTOMATIC' ],
        gear_type: [],
        color: [],
        search_tag: [],
        catalog_equipment: [],
    });
});

it('должен удалить комплектацаию, если уже выбрана эта же', () => {
    const searchParameters = {
        catalog_filter: [
            {
                mark: 'A',
                model: 'B',
                generation: '666',
                configuration: '666',
                tech_param: '1111',
                complectation_name: 'Test',
            },
            {
                mark: 'A',
                model: 'B',
                generation: '666',
                configuration: '666',
                tech_param: '1111',
                complectation_name: 'Test2',
            },
        ],
        transmission: [ 'AUTOMATIC' ],
        gear_type: [ 'ALL_WHEEL_DRIVE' ],
        color: [ '926547' ],
        catalog_equipment: [ 'automatic-lighting-control' ],
    };

    const filterValues = {
        complectation_name: 'Test2',
        transmission: [ 'AUTOMATIC' ],
        gear_type: [],
        color: [],
        catalog_equipment: [],
        search_tag: [],
    };

    expect(updateSearchParametersByFilterValues(searchParameters, filterValues, { complectationFilter: true })).toEqual({
        catalog_filter: [
            {
                mark: 'A',
                model: 'B',
                generation: '666',
                configuration: '666',
                complectation_name: 'Test',
            },
        ],
        transmission: [ 'AUTOMATIC' ],
        gear_type: [],
        color: [],
        search_tag: [],
        catalog_equipment: [],
    });
});

it('должен разбить значение tech_param_id, если передаются значения через запятую', () => {
    const searchParameters = {
        catalog_filter: [
            {
                mark: 'A',
                model: 'B',
                generation: '666',
                configuration: '666',
                tech_param: '1111',
                complectation_name: 'Test',
            },
        ],
        transmission: [ 'AUTOMATIC' ],
        gear_type: [ 'ALL_WHEEL_DRIVE' ],
        color: [ '926547' ],
        catalog_equipment: [ 'automatic-lighting-control' ],
    };

    const filterValues = {
        transmission: [],
        gear_type: [],
        color: [],
        catalog_equipment: [],
        search_tag: [],
        tech_param_id: [ '2222,1111' ],
    };

    expect(updateSearchParametersByFilterValues(searchParameters, filterValues)).toEqual({
        catalog_filter: [
            {
                mark: 'A',
                model: 'B',
                generation: '666',
                configuration: '666',
                tech_param: '2222',
            },
            {
                mark: 'A',
                model: 'B',
                generation: '666',
                configuration: '666',
                tech_param: '1111',
            },
        ],
        transmission: [],
        gear_type: [],
        color: [],
        search_tag: [],
        catalog_equipment: [],
    });
});

it('должен разбить значение tech_param_id, если передаются значения через запятую и выбрано несколько комплектаций', () => {
    const searchParameters = {
        catalog_filter: [
            {
                mark: 'A',
                model: 'B',
                generation: '666',
                configuration: '666',
                tech_param: '1111',
                complectation_name: 'Test',
            },
            {
                mark: 'A',
                model: 'B',
                generation: '666',
                configuration: '666',
                tech_param: '1111',
                complectation_name: 'Test2',
            },
        ],
        transmission: [ 'AUTOMATIC' ],
        gear_type: [ 'ALL_WHEEL_DRIVE' ],
        color: [ '926547' ],
        catalog_equipment: [ 'automatic-lighting-control' ],
    };

    const filterValues = {
        complectation_name: 'Test2',
        transmission: [],
        gear_type: [],
        color: [],
        catalog_equipment: [],
        search_tag: [],
        tech_param_id: [ '2222,1111' ],
    };

    expect(updateSearchParametersByFilterValues(searchParameters, filterValues)).toEqual({
        catalog_filter: [
            {
                mark: 'A',
                model: 'B',
                generation: '666',
                configuration: '666',
                tech_param: '2222',
                complectation_name: 'Test',
            },
            {
                mark: 'A',
                model: 'B',
                generation: '666',
                configuration: '666',
                tech_param: '1111',
                complectation_name: 'Test',
            },
            {
                mark: 'A',
                model: 'B',
                generation: '666',
                configuration: '666',
                tech_param: '2222',
                complectation_name: 'Test2',
            },
            {
                mark: 'A',
                model: 'B',
                generation: '666',
                configuration: '666',
                tech_param: '1111',
                complectation_name: 'Test2',
            },
        ],
        transmission: [],
        gear_type: [],
        color: [],
        search_tag: [],
        catalog_equipment: [],
    });
});

it('сбрасываем параметры, если передана shouldResetEngineParams', () => {
    const searchParameters = {
        catalog_filter: [
            {
                mark: 'A',
                model: 'B',
                generation: '666',
                configuration: '666',
                tech_param: '1111',
                complectation_name: 'Test',
            },
            {
                mark: 'A',
                model: 'B',
                generation: '666',
                configuration: '666',
                tech_param: '2222',
                complectation_name: 'Test',
            },
        ],
        acceleration_from: 5,
        acceleration_to: 10,
        displacement_from: 1400,
        displacement_to: 2500,
        fuel_rate_to: 6,
        power_from: 200,
        power_to: 300,
        power_kv_from: 200,
        power_kv_to: 300,
        engine_group: [ 'DIESEL' ],

    };

    const filterValues = {
        tech_param_id: [ '1111' ],
    };

    expect(updateSearchParametersByFilterValues(searchParameters, filterValues, { shouldResetEngineParams: true })).toEqual({
        catalog_filter: [
            {
                mark: 'A',
                model: 'B',
                generation: '666',
                configuration: '666',
                tech_param: '1111',
            },
        ],
        search_tag: [],
    });
});

it('сбрасываем все параметры кроме базового catalog_filter, если передается shouldResetAll', () => {
    const searchParameters = {
        catalog_filter: [
            {
                mark: 'A',
                model: 'B',
                generation: '666',
                configuration: '666',
                tech_param: '1111',
                complectation_name: 'Test',
            },
            {
                mark: 'A',
                model: 'B',
                generation: '666',
                configuration: '666',
                tech_param: '2222',
                complectation_name: 'Test',
            },
        ],
        acceleration_from: 5,
        acceleration_to: 10,
        displacement_from: 1400,
        displacement_to: 2500,
        fuel_rate_to: 6,
        power_from: 200,
        power_to: 300,
        power_kv_from: 200,
        power_kv_to: 300,
        engine_group: [ 'DIESEL' ],

    };

    const filterValues = {
        color: undefined,
        transmission: undefined,
        gear_type: undefined,
        catalog_equipment: undefined,
        complectation_name: undefined,
        tech_param_id: undefined,
        year_from: undefined,
        year_to: undefined,
        price_from: undefined,
        price_to: undefined,
    };

    expect(updateSearchParametersByFilterValues(searchParameters, filterValues, { shouldResetAll: true })).toEqual({
        catalog_filter: [
            {
                mark: 'A',
                model: 'B',
                generation: '666',
                configuration: '666',
            },
        ],
        color: undefined,
        transmission: undefined,
        gear_type: undefined,
        catalog_equipment: undefined,
        complectation_name: undefined,
        tech_param_id: undefined,
        year_from: undefined,
        year_to: undefined,
        price_from: undefined,
        price_to: undefined,
    });
});
