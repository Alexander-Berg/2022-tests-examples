const link = require('./link');

it('должен сгенерировать ссылку с доменом', () => {
    expect(link('search-app-index')).toEqual('https://search-app.autoru_frontend.base_domain/');
});

it('должен сгенерировать ссылку без домена, если есть флаг noDomain', () => {
    expect(link('search-app-index', {}, {}, { noDomain: true })).toEqual('/');
});
