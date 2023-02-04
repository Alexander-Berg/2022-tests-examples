import getRings from './getRings';

const offersCountInMainListing = 15;
const locatorCounters = {
    firstSample: [
        { radius: 200, count: 15 },
        { radius: 300, count: 15 },
        { radius: 400, count: 20 },
        { radius: 500, count: 21 },
        { radius: 600, count: 21 },
        { radius: 700, count: 21 },
        { radius: 800, count: 22 },
        { radius: 900, count: 23 },
        { radius: 1000, count: 23 },
    ],
    secondSample: [
        { radius: 300, count: 26 },
        { radius: 400, count: 40 },
        { radius: 500, count: 52 },
        { radius: 600, count: 73 },
        { radius: 700, count: 84 },
        { radius: 800, count: 102 },
        { radius: 900, count: 106 },
        { radius: 1000, count: 118 },
    ],
};

it('должен правильно сформировать список колец для запроса БЛ', () => {
    expect(getRings(locatorCounters.firstSample, offersCountInMainListing)).toEqual([
        { radius: 400, count: 20 },
        { radius: 500, count: 21 },
        { radius: 800, count: 22 },
        { radius: 900, count: 23 },
    ]);
});

it('должен правильно сформировать список колец для запроса БЛ + скомпоновать под минимальный pageSize', () => {
    const pageSize = 4;

    expect(getRings(locatorCounters.firstSample, offersCountInMainListing, pageSize)).toEqual([
        { radius: 400, count: 20 },
        { radius: 900, count: 23 },
    ]);
});

it('должен отдать все кольца, если нет кольца с маленьким кол-вом ооферов', () => {
    expect(getRings(locatorCounters.secondSample, 20, 37)).toEqual([
        { radius: 500, count: 52 },
        { radius: 700, count: 84 },
        { radius: 1000, count: 118 },
    ]);
});
