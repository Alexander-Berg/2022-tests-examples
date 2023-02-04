import getMarkModelSearchParamsFromFilters from './getMarkModelSearchParamsFromFilters';

it(`должен вернуть правильный параметр mark_model`, () => {
    const marks = [ 'MINI', 'FORD', 'AUDI' ];
    const models = [ 'MINI#COOPER', 'FORD#FOCUS' ];

    const sortedMarkModels = getMarkModelSearchParamsFromFilters(marks, models);

    expect(sortedMarkModels).toEqual([ 'MINI#COOPER', 'FORD#FOCUS', 'AUDI' ]);
});
