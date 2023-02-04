const link = require('./link');

const ID = '<some credit application id>';

it('должен сгенерировать ссылку с доменом', () => {
    expect(link('credit', { id: ID })).toEqual(`https://autoru_frontend.base_domain/${ ID }/`);
});

it('должен сгенерировать ссылку без домена, если есть флаг noDomain', () => {
    expect(link('credit-edit', { id: ID }, {}, { noDomain: true })).toEqual(`/edit/${ ID }/`);
});
