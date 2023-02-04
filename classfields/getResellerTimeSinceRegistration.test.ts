import MockDate from 'mockdate';

import { nbsp } from 'auto-core/react/lib/html-entities';

import getResellerTimeSinceRegistration from './getResellerTimeSinceRegistration';

beforeEach(() => {
    MockDate.set('2021-01-13');
});

afterEach(() => {
    MockDate.reset();
});

it('вернет число лет, прошедшее с регистрации если зарегестрирован больше года', () => {
    const user = {
        registration_date: '2018-01-12',
    };

    expect(getResellerTimeSinceRegistration(user)).toBe(`3${ nbsp }года`);
});

it('вернет число месяцев, прошедшее с регистрации если зарегестрирован меньше года', () => {
    const user = {
        registration_date: '2020-07-12',
    };

    expect(getResellerTimeSinceRegistration(user)).toBe(`6${ nbsp }месяцев`);
});

it('вернет число месяцев, прошедшее с регистрации если зарегестрирован меньше месяца', () => {
    const user = {
        registration_date: '2020-12-20',
    };

    expect(getResellerTimeSinceRegistration(user)).toBe(`24${ nbsp }дня`);
});

it('вернет пустую строку, если с регистрации прошло меньше дня', () => {
    const user = {
        registration_date: '2021-01-13',
    };

    expect(getResellerTimeSinceRegistration(user)).toBe('1 день');
});

it('вернет пустую строку, если нет registration_date', () => {
    const user = {};

    expect(getResellerTimeSinceRegistration(user)).toBe('');
});
