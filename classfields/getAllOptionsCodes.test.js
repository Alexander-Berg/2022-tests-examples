const getAllOptionsCodes = require('./getAllOptionsCodes');

const cardGroupComplectations = {
    data: {
        search_parameters: {
            tech_param_id: '21221591',
        },
        complectations: [
            {
                tech_info: {
                    tech_param: {
                        id: '21221591',
                    },
                    complectation: {
                        available_options: [
                            'cruise-control',
                            'multi-wheel',
                            'airbag-passenger',
                            'bas',
                            'lock',
                            'electro-mirrors',
                            'laser-lights',
                        ],
                    },
                },
            },
            {
                tech_info: {
                    tech_param: {
                        id: '21221591',
                    },
                    complectation: {
                        available_options: [
                            'cruise-control',
                            'airbag-passenger',
                            'mirrors-heat',
                            'volume-sensor',
                            'leather',
                            'glonass',
                            'xenon',
                        ],
                    },
                },
            },
        ],
    },
};

const equipmentFilters = {
    data: {
        categories: [
            {
                groups: [
                    {
                        options: [
                            {
                                code: 'xenon',
                            },
                            {
                                code: 'cruise-control',
                                offers_count: 10,
                            },
                            {
                                code: 'multi-wheel',
                                offers_count: 10,
                            },
                            {
                                code: 'airbag-passenger',
                                offers_count: 10,
                            },
                            {
                                code: 'bas',
                                offers_count: 10,
                            },
                            {
                                code: 'lock',
                                offers_count: 10,
                            },
                            {
                                code: 'laser-lights',
                                offers_count: 3,
                            },
                            {
                                code: 'led-lights',
                                offers_count: 10,
                            },
                        ],
                        code: 'lights',
                    },
                ],
            },
        ],
    },
};

const state = {
    cardGroupComplectations,
    equipmentFilters,
};

const allOptions = getAllOptionsCodes(state);

it('должен вернуть список кодов опций', () => {
    expect(allOptions).toStrictEqual([
        'multi-wheel',
        'bas',
        'lock',
        'laser-lights',
        'led-lights',
    ]);
});

it('должен вернуть список, в котором присутствуют базовые опции, не являющиеся общими для всех комплектаций', () => {
    expect(allOptions).toEqual(expect.arrayContaining([
        'multi-wheel',
        'bas',
        'lock',
    ]));
});

it('должен вернуть список, в котором отсутствуют базовые опции, общие для всех комплектаций', () => {
    expect(allOptions).not.toEqual(expect.arrayContaining([
        'cruise-control',
        'airbag-passenger',
    ]));
});

it('должен вернуть список, в котором содержатся дополнительные опции для всех комплектаций', () => {
    expect(allOptions).toEqual(expect.arrayContaining([
        'laser-lights',
        'led-lights',
    ]));
});

it('должен вернуть список, не содержащий опции, для которых нет офферов', () => {
    expect(allOptions).not.toEqual(expect.arrayContaining([
        'xenon',
    ]));
});
