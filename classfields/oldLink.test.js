const oldLink = require('./oldLink');

it('должен сгенерировать ссылку без домена и без параметров', () => {
    expect(
        oldLink('/docs/dkp/'),
    ).toEqual('https://autoru_frontend.base_domain/docs/dkp/');
});

it('должен сгенерировать ссылку без домена с параметрами', () => {
    expect(
        oldLink('/docs/dkp/', { sale_id: '12345', foo: 'bar' }),
    ).toEqual('https://autoru_frontend.base_domain/docs/dkp/?sale_id=12345&foo=bar');
});

it('должен сгенерировать ссылку с доменом и без параметров', () => {
    expect(
        oldLink('/clients/', null, 'office7'),
    ).toEqual('https://office7.autoru_frontend.base_domain/clients/');
});

it('должен сгенерировать ссылку с доменом c параметрами', () => {
    expect(
        oldLink('/email/change/', { r: 'https://auto.ru/', foo: 'bar' }, 'auth'),
    ).toEqual('https://auth.autoru_frontend.base_domain/email/change/?r=https%3A%2F%2Fauto.ru%2F&foo=bar');
});
