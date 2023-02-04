const convertMarkModelNameplatesToMmmInfo = require('./convertMarkModelNameplatesToMmmInfo');

it('должен преобразовать массив mark_model_nameplate в массив mmmInfo', () => {
    const markModelNameplate = [ 'AUDI#A3#111#222', 'BMW', 'VENDOR1' ];
    const catalogFilter = [
        { mark: 'AUDI', model: 'A3', nameplate: '111', generation: '222' },
        { mark: 'BMW' },
        { vendor: 'VENDOR1' },
    ];
    expect(convertMarkModelNameplatesToMmmInfo(markModelNameplate)).toEqual(catalogFilter);
});

it('должен вернуть пустой массив для пустого массива', () => {
    expect(convertMarkModelNameplatesToMmmInfo([])).toEqual([]);
});
