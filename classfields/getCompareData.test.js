const getCompareData = require('./getCompareData');

const MockDate = require('mockdate');

beforeEach(() => {
    MockDate.set('2020-12-31');
});

it(`должен отдать относительную статистику по пресетам, если передана дефолтная дата`, () => {
    const dates = {
        from: '2020-12-01',
        to: '2020-12-31',
    };

    const arr = new Array(60).fill({ value: 1 });

    const result = getCompareData(arr, dates);

    expect(result).toEqual({
        daily: {
            last: 1,
            prev: 1,
        },
        weekly: {
            last: 7,
            prev: 7,
        },
        monthly: {
            last: 30,
            prev: 30,
        },
    });
});

it(`должен отдать относительную статистику с датами по выбранному периоду, если передана не дефолтная дата`, () => {
    const dates = {
        from: '2020-12-06',
        to: '2020-12-10',
    };

    const arr = [];

    for (let i = 0; i < 10; i++) {
        arr.push({ value: i });
    }

    const result = getCompareData(arr, dates);

    expect(result).toEqual({
        relative: {
            last: 35,
            prev: 10,
            lastDateLimits: {
                from: '2020-12-06',
                to: '2020-12-10',
            },
            prevDateLimits: {
                from: '2020-12-01',
                to: '2020-12-05',
            },
        },
    });
});

it(`должен отрезать лишние метрики по краям, если в период попадает сегодняшний день (по нему еще не собрана вся метрика)`, () => {
    const dates = {
        from: '2020-12-27',
        to: '2020-12-31',
    };

    const arr = [];

    for (let i = 0; i < 10; i++) {
        arr.push({ value: i });
    }

    arr[9].date = '2020-12-31';

    const result = getCompareData(arr, dates);

    expect(result).toEqual({
        relative: {
            last: 26,
            prev: 10,
            lastDateLimits: {
                from: '2020-12-27',
                to: '2020-12-30',
            },
            prevDateLimits: {
                from: '2020-12-23',
                to: '2020-12-26',
            },
        },
    });
});
