const getCrumbsData = require('./getCrumbsData');

it('должен вернуть данные для крошек, если marks - пустой массив, но есть searchParams.mark', () => {
    expect(getCrumbsData({
        breadcrumbs: {
            isFetching: false,
            marks: [],
            models: [],
            super_gen: [],
        },
        dealersListing: {
            searchParams: {
                mark: 'scania',
            },
        },
    })).toEqual([ { linkParams: [ 'listing', {} ], text: 'Продажа автомобилей' }, { text: 'Все дилеры ' } ]);
});
