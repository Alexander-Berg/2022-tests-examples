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
        breadcrumbsParams: {},
        breadcrumbsPublicApi: {},
        indexReviews: {},
        journalArticles: {},
        listingSectionsCount: {},
        redirectParams: {},
        review: {},
        reviewComments: {},
        reviews: {},
        reviewsFeatures: {},
        reviewsIndex: {},
        reviewsSummary: {},
        routing: {},
        seo: {},
        state: {},
        subscriptions: {},
        video: {},
    });
});
