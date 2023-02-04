import getSliderSteps from './getSliderSteps';

it('должен вернуть верный массив объектов', () => {
    expect(getSliderSteps({ min: 285000, max: 321000 }))
        .toEqual([ { value: 285000 }, { value: 290000 }, { value: 295000 }, { value: 300000 }, { value: 310000 }, { value: 320000 }, { value: 321000 } ]);
});
