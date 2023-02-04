const getNearestHourStart = require('./getNearestHourStart');
const MockDate = require('mockdate');

it('если меньше 30 минут то округляет до начала предыдущего часа', () => {
    MockDate.set('2019-03-01 13:13:13');
    const result = getNearestHourStart();
    expect(result).toBe('13:00');
});

it('если больше 30 минут то округляет до начала следующего часа', () => {
    MockDate.set('2019-03-01 13:43:13');
    const result = getNearestHourStart();
    expect(result).toBe('14:00');
});

it('добавит 0 впереди если часов меньше 10', () => {
    MockDate.set('2019-03-01 03:43:13');
    const result = getNearestHourStart();
    expect(result).toBe('04:00');
});
