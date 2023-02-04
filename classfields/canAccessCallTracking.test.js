const TARIFF_TYPES = require('www-cabinet/data/calculator/categories-types.json');

const canAccessCallTracking = require('./canAccessCallTracking');

it('должен вернуть true, если коллтрекинг включен', () => {
    const result = canAccessCallTracking({ calltracking_enabled: true }, []);

    expect(result).toBe(true);
});

it('должен вернуть true, если есть звонковый тариф', () => {
    const result = canAccessCallTracking(
        { calltracking_enabled: false },
        [ { type: TARIFF_TYPES.calls, enabled: true } ],
    );

    expect(result).toBe(true);
});
