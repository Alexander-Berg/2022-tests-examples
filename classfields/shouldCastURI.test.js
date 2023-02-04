const shouldCastURI = require('auto-core/router/libs/shouldCastURI');

it('Должен вернуть true, если количество routeParams не избыточно для ЧПУ', () => {
    const routeParams = {
        category: 'cars',
        mark: 'vaz',
        moto_category: null,
        section: 'all',
        trucks_category: null,
        year_from: null,
        year_to: null,
    };
    const gids = [ 213 ];

    expect(shouldCastURI(routeParams, gids)).toEqual(true);
});

it('Должен вернуть false, если категория не cars', () => {
    const routeParams = {
        category: 'trucks',
    };
    expect(shouldCastURI(routeParams)).toEqual(false);
});

it('Должен вернуть false для Грозного, если там мало офферов', () => {
    const routeParams = {
        category: 'cars',
        mark: 'vaz',
        moto_category: null,
        section: 'all',
        trucks_category: null,
        year_from: null,
        year_to: null,
    };
    const gids = [ 1106 ];
    expect(shouldCastURI(routeParams, gids, { isValidOffersNumber: false })).toEqual(false);
});

it('Должен вернуть false для небольшого гео (Грозный), даже если там достаточно офферов', () => {
    const routeParams = {
        category: 'cars',
        mark: 'vaz',
        moto_category: null,
        section: 'all',
        trucks_category: null,
        year_from: null,
        year_to: null,
    };
    const gids = [ 1106 ];
    expect(shouldCastURI(routeParams, gids, { isValidOffersNumber: true })).toEqual(false);
});

it('Должен вернуть false, если количество routeParams избыточно для ЧПУ', () => {
    const routeParams = {
        category: 'cars',
        mark: 'vaz',
        moto_category: null,
        section: 'all',
        model: '1111',
        nameplate_name: 'seaz',
        trucks_category: null,
        year_from: null,
        year_to: null,
        engine_sef: 'engine-dizel',
    };
    expect(shouldCastURI(routeParams)).toEqual(false);
});

it('Должен вернуть false для Москвы, если там мало офферов', () => {
    const routeParams = {
        category: 'cars',
        mark: 'vaz',
        moto_category: null,
        section: 'all',
        trucks_category: null,
        year_from: null,
        year_to: null,
    };
    const gids = [ 213 ];
    expect(shouldCastURI(routeParams, gids, { isValidOffersNumber: false })).toEqual(false);
});

it('Должен вернуть true для Москвы, если там достаточно офферов', () => {
    const routeParams = {
        category: 'cars',
        mark: 'vaz',
        moto_category: null,
        section: 'all',
        trucks_category: null,
        year_from: null,
        year_to: null,
    };
    const gids = [ 213 ];
    expect(shouldCastURI(routeParams, gids, { isValidOffersNumber: true })).toEqual(true);
});
