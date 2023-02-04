import getPeriod from './getPeriod';

it('getPeriod должен вернуть рейндж "от" и "до"', () => {
    const result = getPeriod({ from: '1512766800000', to: '1544216400000' });
    expect(result).toEqual('9 декабря 2017 — 8 декабря 2018');
});

it('getPeriod должен вернуть только "от"', () => {
    const result = getPeriod({ from: '1512766800000' });
    expect(result).toEqual('9 декабря 2017');
});

it('getPeriod должен вернуть undefined', () => {
    const result = getPeriod({ to: '1512766800000' });
    expect(result).toBeUndefined();
});
