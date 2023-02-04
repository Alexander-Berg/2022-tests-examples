const link = require('./link');

it('должен сгенерировать ссылку с доменом', () => {
    expect(link('mag-index')).toEqual('https://mag.autoru_frontend.base_domain/');
});

it('должен сгенерировать ссылку без домена, если есть флаг noDomain', () => {
    expect(link('mag-index', {}, {}, { noDomain: true })).toEqual('/');
});
