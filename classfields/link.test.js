const link = require('./link');

it('должен сгенерировать ссылку с доменом', () => {
    expect(link('index')).toEqual('https://autoru_frontend.base_domain/');
});

it('должен сгенерировать ссылку без домена, если есть флаг noDomain', () => {
    expect(link('index', {}, {}, { noDomain: true })).toEqual('/');
});

it('должен сгенерировать ссылку с доменом и гео', () => {
    expect(link(
        'listing',
        { category: 'cars', mark: 'bmw' },
        { geoAlias: 'moskva', geoIds: [ 213 ] },
    )).toEqual('https://autoru_frontend.base_domain/moskva/cars/bmw/all/');
});

it('должен сгенерировать ссылку с доменом и переданным гео', () => {
    expect(link(
        'listing',
        { category: 'cars', mark: 'bmw', geo_id: 2 },
        { geoAlias: 'moskva', geoIds: [ 213 ] },
    )).toEqual('https://autoru_frontend.base_domain/sankt-peterburg/cars/bmw/all/');
});
