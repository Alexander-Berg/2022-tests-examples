import { hatchbackMock } from 'auto-core/react/dataDomain/techSpecifications/mocks/configuration.mock';

import { SpecificationType } from 'auto-core/types/TStateCatalogRoute';

import createDataConfiguration from './createDataConfiguration';

it('Возвращает данные для таблицы с характеристиками двигателя', () => {
    const result = createDataConfiguration(hatchbackMock, SpecificationType.HARAKTERISTIKI_DVIGATELYA)[1];
    expect(result.slice(0, result.length - 1)).toEqual([
        '2.3 MT (89 л.с.)',
        'бензин',
        2301,
        89,
        160,
    ]);
});

it('Возвращает данные для таблицы с разгоном до 100', () => {
    const result = createDataConfiguration(hatchbackMock, SpecificationType.RAZGON_DO_100)[1];
    expect(result.slice(0, result.length - 1)).toEqual([
        '2.3 MT (89 л.с.)',
        14.3,
        160,
    ]);
});

it('Возвращает данные для таблицы с трансмиссией', () => {
    const result = createDataConfiguration(hatchbackMock, SpecificationType.TRANSMISSIYA)[1];
    expect(result.slice(0, result.length - 1)).toEqual([
        '2.3 MT (89 л.с.)',
        'механика',
        4,
    ]);
});

it('Возвращает данные для таблицы с объемом топливного бака', () => {
    const result = createDataConfiguration(hatchbackMock, SpecificationType.OBEM_TOPLIVNOGO_BAKA)[1];
    expect(result.slice(0, result.length - 1)).toEqual([
        '2.3 MT (89 л.с.)',
        49,
    ]);
});

it('Возвращает данные для таблицы с расходом топлива', () => {
    const result = createDataConfiguration(hatchbackMock, SpecificationType.RASHOD_TOPLIVA)[1];
    expect(result.slice(0, result.length - 1)).toEqual([
        '2.3 MT (89 л.с.)',
        10.2,
        7.1,
        9,
    ]);
});

it('Возвращает данные для таблицы с клиренсом', () => {
    const result = createDataConfiguration(hatchbackMock, SpecificationType.KLIRENS)[1];
    expect(result.slice(0, result.length - 1)).toEqual([
        '2.3 MT (89 л.с.)',
        '130',
        2443,
        1412,
        1417,
    ]);
});

it('Возвращает данные для таблицы с объемом багажника', () => {
    const result = createDataConfiguration(hatchbackMock, SpecificationType.OBEM_BAGAZHNIKA)[1];
    expect(result.slice(0, result.length - 1)).toEqual([
        '2.3 MT (89 л.с.)',
        '20 / 60',
    ]);
});

it('Возвращает данные для таблицы с размером и весом', () => {
    const result = createDataConfiguration(hatchbackMock, SpecificationType.RAZMER_VES)[1];
    expect(result.slice(0, result.length - 1)).toEqual([
        '2.3 MT (89 л.с.)',
        '4445 x 1783 x 1278',
        1250,
    ]);
});

it('Возвращает данные для таблицы с типом привода', () => {
    const result = createDataConfiguration(hatchbackMock, SpecificationType.TIP_PRIVODA)[1];
    expect(result.slice(0, result.length - 1)).toEqual([
        '2.3 MT (89 л.с.)',
        'задний',
    ]);
});
