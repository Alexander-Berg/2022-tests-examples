const getTimeAgoString = require('./getTimeAgoString');

let currentTime;

beforeEach(() => {
    currentTime = new Date().getTime();
});

it('возвращает "только что" если разница меньше минуты', () => {
    const timestamp = currentTime - 1000 * 30;

    expect(getTimeAgoString(timestamp)).toBe('только что');
});

it('возвращает "x мин. назад" если разница меньше часа', () => {
    const timestamp = currentTime - 1000 * 60 * 15;

    expect(getTimeAgoString(timestamp)).toBe('15 мин. назад');
});

it('возвращает "x часов назад" если разница больше часа', () => {
    const timestamp = currentTime - 1000 * 60 * 60 * 2;

    expect(getTimeAgoString(timestamp)).toBe('2 часа назад');
});

it('ничего не возвращает если время в будущем', () => {
    const timestamp = currentTime + 1000 * 20;

    expect(getTimeAgoString(timestamp)).toBeNull();
});

it('ничего не возвращает если разница больше переданного лимита', () => {
    const timestamp = currentTime - 1000 * 60 * 60 * 2;
    const limit = 1000 * 60 * 60;

    expect(getTimeAgoString(timestamp, limit)).toBeNull();
});
