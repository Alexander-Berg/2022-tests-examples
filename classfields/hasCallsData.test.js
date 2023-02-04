const hasCallsData = require('www-cabinet/react/dataDomain/sales/helpers/hasCallsData');

it('должен вернуть true, если включены настройки offers_stat_enabled и calltracking_enabled и ' +
    'регион пользователя входит в спиок регионов из бункера',
() => {
    expect(hasCallsData({
        state: {
            client: {
                salon: {
                    poi: {
                        ya_region_id: 1,
                    },
                },
            },
            bunker: {
                'cabinet/calltracking_tariffs_regions': {
                    CARS_USED: [ 1 ],
                },
            },
        },
        callTrackingSettings: {
            offers_stat_enabled: true,
            calltracking_enabled: true,
        },
        section: 'used',
        category: 'cars',
    })).toBe(true);
});

it('должен вернуть true, если включены настройки offers_stat_enabled' +
    'регион пользователя входит в спиок регионов из бункера и' +
    'у пользователя включен звонковый тариф в легковых новых и объявление из легковых новых', () => {
    expect(hasCallsData(
        {
            state: {
                client: {
                    salon: {
                        poi: {
                            ya_region_id: 1,
                        },
                    },
                },
                bunker: {
                    'cabinet/calltracking_tariffs_regions': {
                        CARS_NEW: [ 1 ],
                    },
                },
            },
            tariffs: [
                {
                    category: 'CARS',
                    enabled: true,
                    type: 'CALLS',
                    section: [ 'NEW' ],
                },
            ],
            callTrackingSettings: {
                offers_stat_enabled: true,
                calltracking_enabled: false,
            },
            section: 'new',
            category: 'cars',
        },
    )).toBe(true);
});
