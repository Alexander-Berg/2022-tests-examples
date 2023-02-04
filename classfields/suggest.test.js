const suggest = require('./suggest');

let context;
beforeEach(() => {
    context = {
        config: {},
        req: {},
    };
});

it('должен добавить подкатегорию для TRUCKS, если ее нет в ответе', () => {
    const result = suggest({
        params: { section: 'new' },
        context,
        result: {
            equipmentDictionaries: {},
            suggest: {
                suggests: [
                    {
                        params: {
                            trucks_params: {
                                transmission: [
                                    'AUTOMATIC',
                                ],
                            },
                        },
                        view: {
                            applied_filter_count: 1,
                        },
                    },
                ],
            },
        },
    });

    expect(result.suggests[0].params).toEqual({
        category: 'trucks',
        from: 'searchline',
        section: 'new',
        transmission: [
            'AUTOMATIC',
        ],
        trucks_category: 'LCV',
    });
});

it('должен добавить подкатегорию для MOTO, если ее нет в ответе', () => {
    const result = suggest({
        params: { section: 'used' },
        context,
        result: {
            equipmentDictionaries: {},
            suggest: {
                suggests: [
                    {
                        params: {
                            moto_params: {
                                transmission: [
                                    'AUTOMATIC',
                                ],
                            },
                        },
                        view: {
                            applied_filter_count: 1,
                        },
                    },
                ],
            },
        },
    });

    expect(result.suggests[0].params).toEqual({
        category: 'moto',
        from: 'searchline',
        section: 'used',
        transmission: [
            'AUTOMATIC',
        ],
        moto_category: 'MOTORCYCLE',
    });
});
