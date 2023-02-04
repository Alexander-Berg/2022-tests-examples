const dayjs = require('auto-core/dayjs');

const getHumanDuration = require('./getHumanDuration');

const START_TIMESTAMP = 1551184406000;

const TESTS = [
    {
        value: 1,
        units: {
            days: 'день',
            hours: 'час',
            minutes: 'минута',
            seconds: 'секунда',
        },
    },
    {
        value: 2,
        units: {
            days: 'дня',
            hours: 'часа',
            minutes: 'минуты',
            seconds: 'секунды',
        },
    },
    {
        value: 5,
        units: {
            days: 'дней',
            hours: 'часов',
            minutes: 'минут',
            seconds: 'секунд',
        },
    },
];

it('должен вернуть пустую строку, если нет параметров', () => {
    expect(getHumanDuration()).toBe('');
});

it('должен вернуть пустую строку, если оба параметра некорректны', () => {
    expect(getHumanDuration('mayBeDate', 'mayBeNot')).toBe('');
});

it('должен вернуть пустую строку, если не получается преобразовать первый параметр в дату', () => {
    expect(getHumanDuration('', 1551184406000)).toBe('');
});

it('должен вернуть пустую строку, если не получается преобразовать второй параметр в дату', () => {
    expect(getHumanDuration(1551184406000, false)).toBe('');
});

TESTS.forEach(test => {
    Object.entries(test.units).forEach(([ unitKey, unitRus ]) => {
        const endDatetime = dayjs(START_TIMESTAMP).add(test.value, unitKey);
        it(`должен корректно выводить разницу в именительном падеже: ${ test.value } ${ unitRus }`, () => {
            expect(getHumanDuration(START_TIMESTAMP, endDatetime)).toBe(`${ test.value }\u00a0${ unitRus }`);
        });
    });
});

it('должен корректно выводить разницу в минутах дательном падеже для чисел, оканчивающихся на 1', () => {
    const addedMinutes = dayjs(START_TIMESTAMP).add(21, 'minutes');
    expect(getHumanDuration(START_TIMESTAMP, addedMinutes, 'dative')).toBe(`21\u00a0минуту`);
});

it('должен корректно выводить разницу в секундах в дательном падеже для чисел, оканчивающихся на 1', () => {
    const addedSeconds = dayjs(START_TIMESTAMP).add(21, 'seconds');
    expect(getHumanDuration(START_TIMESTAMP, addedSeconds, 'dative')).toBe(`21\u00a0секунду`);
});

//граничное значение!
it('должен вернуть 10 дней, если разница 10 дней!', () => {
    const endTimestamp = dayjs(0).add(10, 'days');
    expect(getHumanDuration(0, endTimestamp)).toBe(`10\u00a0дней`);
});

it('должен вернуть ничего, если разница 11 дней', () => {
    const endTimestamp = dayjs(0).add(11, 'days');
    expect(getHumanDuration(0, endTimestamp)).toBe('');
});

it('должен вернуть ничего, если разница месяц', () => {
    const endTimestamp = dayjs(0).add(1, 'month');
    expect(getHumanDuration(0, endTimestamp)).toBe('');
});

it('должен вернуть ничего, если разница год', () => {
    expect(getHumanDuration(0, 1000 * 60 * 60 * 24 * 366)).toBe('');
});
