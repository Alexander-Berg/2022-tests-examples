const getCreditTermSliderSteps = require('./getCreditTermSliderSteps');

it('должен вернуть массив объектов', () => {
    expect(getCreditTermSliderSteps([ 1, 2, 3, 4, 5 ])).toEqual([ { value: 1 }, { value: 2 }, { value: 3 }, { value: 4 }, { value: 5 } ]);
});
