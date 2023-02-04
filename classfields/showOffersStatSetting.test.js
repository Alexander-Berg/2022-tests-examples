const showOffersStatSetting = require('./showOffersStatSetting');

let state;

beforeEach(() => {
    state = {
        bunker: {
            'cabinet/calltracking_tariffs_regions': {
                CARS_USED: [ 1, 10174 ],
                CARS_NEW: [ 1, 10174 ],
            },
        },
        promoPopup: { tariffs: [] },
        client: { salon: { poi: { ya_region_id: '1' } } },
    };
});

it('должен отдавать true, если у клиента есть тариф из списка в бункере, и регион клиента входит в список регионов данного тарифа', () => {
    state.promoPopup.tariffs.push({ category: 'CARS', section: [ 'USED' ], enabled: true });

    const result = showOffersStatSetting(state);

    expect(result).toBe(true);
});

it('должен отдавать false, если у клиента есть тариф из списка в бункере, но регион клиента не входит в список регионов данного тарифа', () => {
    state.promoPopup.tariffs.push({ category: 'CARS', section: [ 'NEW' ], enabled: true });
    state.client.salon.poi.ya_region_id = '2';

    const result = showOffersStatSetting(state);

    expect(result).toBe(false);
});

it('должен отдавать false, если у клиента нет тарифа из списка в бункере', () => {
    const result = showOffersStatSetting(state);

    expect(result).toBe(false);
});
