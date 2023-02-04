const getSliderItems = require('./getSliderItems');

it('должен вернуть набор элементов для слайдера диапазона значений для широкого диапазона с указанием единицы измерениz', () => {
    expect(getSliderItems(10000, 2000000, '₽')).toMatchSnapshot();
});

it('должен вернуть набор элементов для узкого диапазона', () => {
    expect(getSliderItems(2000, 2020)).toMatchSnapshot();
});
