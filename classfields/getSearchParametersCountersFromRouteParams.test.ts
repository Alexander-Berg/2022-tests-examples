import getSearchParametersCountersFromRouteParams from './getSearchParametersCountersFromRouteParams';
it('должен вернуть правильный счетчик расширенных фильтров', () => {
    expect(getSearchParametersCountersFromRouteParams({
        tag: 'good_price',
        exclude_tag: [],
        has_interior_panorama: true,
        has_exterior_panorama: true,
        has_photo: false,
        auction: true,
    }).extended).toEqual(4);
});

it('должен вернуть правильный счетчик главных фильтров', () => {
    expect(getSearchParametersCountersFromRouteParams({
        tag: [ 'vin_resolution_ok', 'autoru_posted' ],
        multiposting_service: 'autoru_fresh',
        mark_model: 'CADILLAC',
        price_to: '300000',
        create_date_from: '2021-08-16',
    }).main).toEqual(6);
});
