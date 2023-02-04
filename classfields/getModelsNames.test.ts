import versusMock from '../mock';

import getModelsNames from './getModelsNames';

it('должен вернуть правильные названия тачек для версуса', () => {
    expect(getModelsNames(versusMock.value())).toEqual([ 'Ford EcoSport', 'Kia Rio' ]);
});

it('должен вернуть правильные названия тачек для версуса (русский вариант)', () => {
    expect(getModelsNames(versusMock.value(), true)).toEqual([ 'Форд ЭкоСпорт', 'Киа Рио' ]);
});

it('должен вернуть правильные названия тачек для версуса с учётом nameplate', () => {
    const data = versusMock
        .withModelNameplate({ number: 1, nameplate: { id: 'x_line', name: 'X-Line' } })
        .value();

    expect(getModelsNames(data)).toEqual([ 'Ford EcoSport', 'Kia Rio X-Line' ]);
});
