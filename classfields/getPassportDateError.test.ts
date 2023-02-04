import mockdate from 'mockdate';

import getPassportDateError from './getPassportDateError';

beforeEach(() => {
    mockdate.set('2020-11-10');
});

it('не должен вернуть ошибку для валидной даты паспорта и даты рождения', () => {
    expect(getPassportDateError('10.11.2020', '10.11.2006')).toBe('');
});

it('должен вернуть ошибку, если дата в будущем', () => {
    expect(getPassportDateError('11.11.2020', '')).toBe('Неверная дата');
});

it('должен вернуть ошибку, если дата не дата', () => {
    expect(getPassportDateError('привет', '')).toBe('Неверная дата');
});

it('должен вернуть ошибку, если паспорт выдан до 14 лет', () => {
    expect(getPassportDateError('10.11.2020', '11.11.2006')).toBe('Паспорт выдают после 14 лет!');
});

it('не должен вернуть ошибку для валидной даты паспорта и невалидной даты рождения', () => {
    expect(getPassportDateError('10.11.2020', 'ааа')).toBe('');
});
