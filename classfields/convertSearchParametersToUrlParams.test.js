const convertSearchParametersToUrlParams = require('./convertSearchParametersToUrlParams');

it('должен преобразовать поисковые параметры в параметры для урла', () => {
    const searchParameters = {
        catalog_filter: [
            {
                mark: 'A',
                model: 'B',
                generation: '666',
                configuration: '777',
                tech_param: '1111',
                complectation_name: 'Test',
            },
            {
                mark: 'A',
                model: 'B',
                generation: '666',
                configuration: '777',
                tech_param: '2222',
                complectation_name: 'Test',
            },
        ],
        transmission: [ 'AUTOMATIC' ],
        gear_type: [ 'ALL_WHEEL_DRIVE' ],
        color: [ '926547' ],
        catalog_equipment: [ 'automatic-lighting-control' ],
    };

    expect(convertSearchParametersToUrlParams(searchParameters)).toEqual({
        mark: 'a',
        model: 'b',
        super_gen: '666',
        configuration_id: '777',
        transmission: [ 'AUTOMATIC' ],
        gear_type: [ 'ALL_WHEEL_DRIVE' ],
        color: [ '926547' ],
        catalog_equipment: [ 'automatic-lighting-control' ],
        catalog_filter: [
            'mark=A,model=B,generation=666,configuration=777,tech_param=1111,complectation_name=Test',
            'mark=A,model=B,generation=666,configuration=777,tech_param=2222,complectation_name=Test',
        ],
    });
});

it('должен преобразовать поисковые параметры в параметры для урла и удалить catalog_filter', () => {
    const searchParameters = {
        catalog_filter: [
            {
                mark: 'A',
                model: 'B',
                generation: '666',
                configuration: '777',
            },
        ],
    };

    expect(convertSearchParametersToUrlParams(searchParameters)).toEqual({
        mark: 'a',
        model: 'b',
        super_gen: '666',
        configuration_id: '777',
    });
});

it('должен преобразовать поисковые параметры в параметры для урла и не удалить catalog_filter', () => {
    const searchParameters = {
        catalog_filter: [
            {
                mark: 'A',
                model: 'B',
                generation: '666',
                configuration: '777',
                tech_param: '1111',
                complectation_name: 'Test',
            },
        ],
    };

    expect(convertSearchParametersToUrlParams(searchParameters)).toEqual({
        mark: 'a',
        model: 'b',
        super_gen: '666',
        configuration_id: '777',
        catalog_filter: [
            'mark=A,model=B,generation=666,configuration=777,tech_param=1111,complectation_name=Test',
        ],
    });
});
