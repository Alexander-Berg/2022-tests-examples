const getPageMeta = require('./getPageMeta');

const data = {
    state: {
        h1: 'H1Text',
        pageTitle: 'Title',
        pageDesc: 'Description',
        seoText: 'Text',
        seoTextFooter: 'Footer',
        seoTitle: 'Title',
        seoImage: 'https://m.auto.ru/catalog/cars/all/img.jpg',
        pageUrl: 'https://m.auto.ru/catalog/cars/all/?autoru_body_type=sedan',
        seoKeywords: 'Keywords',
        seoShortText: 'ShrTxt',
        subscriptionTitle: 'Title',
        canonical: 'https://auto.ru/catalog/cars/all/?autoru_body_type=sedan',
    },
};

const req = {
    url: '/catalog/cars/all/?autoru_body_type=sedan',
    fullUrl: 'https://m.auto.ru/catalog/cars/all/?autoru_body_type=sedan',
};

it('должен правильно вернуть pageMeta, если указан canonicle', () => {
    const pageMeta = getPageMeta(data, req);
    expect(pageMeta).toMatchSnapshot();
});

it('должен правильно вернуть pageMeta, если не указан canonicle', () => {
    data.state.canonical = undefined;
    const pageMeta = getPageMeta(data, req);
    expect(pageMeta).toMatchSnapshot();
});
