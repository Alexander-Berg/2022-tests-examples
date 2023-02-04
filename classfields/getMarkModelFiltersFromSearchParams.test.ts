import getMarkModelFiltersFromSearchParams from './getMarkModelFiltersFromSearchParams';

it(`должен вернуть правильные значения фильтров, когда поступает массив значений`, () => {
    const sortedMarkModels = getMarkModelFiltersFromSearchParams({ mark_model: [ 'MINI#COOPER', 'FORD#FOCUS', 'AUDI' ] });

    expect(sortedMarkModels).toEqual({ marks: [ 'MINI', 'FORD', 'AUDI' ], models: [ 'MINI#COOPER', 'FORD#FOCUS' ] });
});

it(`должен вернуть пустые массивы, когда значений нет`, () => {
    const sortedMarkModels = getMarkModelFiltersFromSearchParams({});

    expect(sortedMarkModels).toEqual({ marks: [], models: [] });
});

it(`должен вернуть массивы с одним элементов, когда приходит только одно значение марки-модели`, () => {
    const sortedMarkModels = getMarkModelFiltersFromSearchParams({ mark_model: [ 'MINI#COOPER' ] });

    expect(sortedMarkModels).toEqual({ marks: [ 'MINI' ], models: [ 'MINI#COOPER' ] });
});

it(`должен вернуть массив марок и пустой массив моделей, когда приходят только марки`, () => {
    const sortedMarkModels = getMarkModelFiltersFromSearchParams({ mark_model: [ 'MINI', 'AUDI' ] });

    expect(sortedMarkModels).toEqual({ marks: [ 'MINI', 'AUDI' ], models: [] });
});
