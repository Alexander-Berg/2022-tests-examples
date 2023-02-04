import mockdate from 'mockdate';

import getBirthDateError from './getBirthDateError';

beforeEach(() => {
    mockdate.set('2020-11-10');
});

it('не должен вернуть ошибку для валидной даты рождения', () => {
    expect(getBirthDateError('10.11.2002')).toBe('');
});

it('должен вернуть ошибку, если дата не дата', () => {
    expect(getBirthDateError('привет')).toBe('Неверная дата');
});

it('должен вернуть ошибку, если дата в будущем', () => {
    expect(getBirthDateError('11.11.2020')).toBe('Неверная дата');
});

it('должен вернуть ошибку, если возраст меньше 18 лет', () => {
    expect(getBirthDateError('11.11.2002')).toBe('Вы слишком молоды');
});
