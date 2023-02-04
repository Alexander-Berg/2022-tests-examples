const reducer = require('./index');

it('должен правильно обработать неизвестное событие', () => {
    expect(reducer(undefined, { type: '@TEST_ACTION' })).toMatchObject({
        // base
        config: {},
        cookies: {},
        geo: {},
        notifier: {},
        user: {},

        // project
        ads: {},
        breadcrumbs: {},
        breadcrumbsPublicApi: {},
        breadcrumbsParams: {},
        breadcrumbsSearch: {},
        cardGroupComplectations: {},
        cardGroupGallery: {},
        router: {},
        seo: {},
        state: {},
    });
});
