import getMarkModelFromRouteParam from './getMarkModelFromRouteParam';

it('должен вытащить марку и модель из routeParams', () => {
    expect(getMarkModelFromRouteParam({
        mark_model: 'BMW#X3',
    })).toEqual({
        mark: [ 'BMW' ],
        mark_model: [ 'BMW#X3' ],
    });
});

it('должен вытащить марки и модели из routeParams', () => {
    expect(getMarkModelFromRouteParam({
        mark_model: [ 'BMW#X3', 'BMW#X5', 'CADILLAC' ],
    })).toEqual({
        mark: [ 'BMW', 'CADILLAC' ],
        mark_model: [ 'BMW#X3', 'BMW#X5' ],
    });
});
