const isIPFromYandexNetwork = require('./isIPFromYandexNetwork');

it('должен вернуть false, если биндинг выбросил исключение', () => {
    expect(isIPFromYandexNetwork(' 178.154.250.88')).toEqual(false);
});

it('должен вернуть true для адреса 178.154.250.88', () => {
    expect(isIPFromYandexNetwork('178.154.250.88')).toEqual(true);
});

it('должен вернуть true для адреса 2a02:6b8::506:18e1:7b97:99:e1fe', () => {
    expect(isIPFromYandexNetwork('178.154.250.88')).toEqual(true);
});

it('должен вернуть true для адреса 211.285.35.394', () => {
    expect(isIPFromYandexNetwork('211.285.35.394')).toEqual(false);
});
