import getDurationDisplayString from './getDurationDisplayString';

it('должен вернуть 5 лет', () => {
    expect(getDurationDisplayString(1825)).toEqual('5 лет');
});

it('должен вернуть 1 год', () => {
    expect(getDurationDisplayString(365)).toEqual('1 год');
});

it('должен вернуть 2 дня', () => {
    expect(getDurationDisplayString(2)).toEqual('2 дня');
});

it('должен вернуть 430 дней', () => {
    expect(getDurationDisplayString(430)).toEqual('430 дней');
});

it('должен вернуть 201 день', () => {
    expect(getDurationDisplayString(201)).toEqual('201 день');
});
