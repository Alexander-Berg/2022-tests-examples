const getSortFromCookie = require('./getSortFromCookie');

it('должен вернуть сортировку из куки', () => {
    expect(getSortFromCookie({ context: { req: {
        cookies: { listing_view_session: '{"sort_offers":"alphabet-ASC","sort":"year-asc"}' },
        experimentsData: { has: () => false },
    } } })).toEqual('year-asc');
});

it('должен вернуть старую сортировку из куки', () => {
    expect(getSortFromCookie({ context: { req: {
        cookies: { listing_view_session: '{"sort_offers":"alphabet-ASC"}' },
        experimentsData: { has: () => false },
    } } })).toEqual('alphabet-asc');
});

it('должен вернуть пустую строку, если нет куки', () => {
    expect(getSortFromCookie({ context: { req: {
        cookies: {},
        experimentsData: { has: () => false },
    } } })).toEqual('');
});

it('должен вернуть пустую строку, если в куке нет сортировки', () => {
    expect(getSortFromCookie({ context: { req: {
        cookies: { listing_view_session: '{%22not-sort%22:%22year-asc%22}' },
        experimentsData: { has: () => false },
    } } })).toEqual('');
});
