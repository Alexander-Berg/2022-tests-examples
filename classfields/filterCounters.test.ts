import filterCounters from './filterCounters';

it('должен отфильтровать нули в каунтерах', () => {
    const counters = [
        {
            radius: 1,
            count: 0,
        },
        {
            radius: 2,
            count: 1,
        },
    ];
    const result = [
        {
            radius: 2,
            count: 1,
        },
    ];
    expect(filterCounters(counters, 100)).toEqual(result);
});

it('должен убрать одинаковые значения в каунтерах и оставить меньший радиус', () => {
    const counters = [
        {
            radius: 0,
            count: 1,
        },
        {
            radius: 1,
            count: 1,
        },
    ];
    const result = [
        {
            radius: 0,
            count: 1,
        },
    ];
    expect(filterCounters(counters, 100)).toEqual(result);
});

it('должен оставить запись с текущим радиусом, даже если там нуль', () => {
    const counters = [
        {
            radius: 1,
            count: 0,
        },
    ];
    const result = [
        {
            radius: 1,
            count: 0,
        },
    ];
    expect(filterCounters(counters, 1)).toEqual(result);
});

it('должен оставить нуль, даже если там нуль', () => {
    const counters = [
        {
            radius: 0,
            count: 0,
        },
    ];
    const result = [
        {
            radius: 0,
            count: 0,
        },
    ];
    expect(filterCounters(counters, 1)).toEqual(result);
});

it('должен убрать одинаковые значения в каунтерах и оставить текущий радиус, даже если он больший', () => {
    const counters = [
        {
            radius: 1,
            count: 1,
        },
        {
            radius: 2,
            count: 1,
        },
        {
            radius: 3,
            count: 1,
        },
    ];
    const result = [
        {
            radius: 2,
            count: 1,
        },
    ];
    expect(filterCounters(counters, 2)).toEqual(result);
});

it('если все каунты разные -- ничего не фильтруем', () => {
    const counters = [
        {
            radius: 0,
            count: 1,
        },
        {
            radius: 1,
            count: 2,
        },
        {
            radius: 2,
            count: 3,
        },
    ];
    expect(filterCounters(counters, 1)).toEqual(counters);
});
