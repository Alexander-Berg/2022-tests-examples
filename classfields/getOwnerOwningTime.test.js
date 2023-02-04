const getOwnerOwningTime = require('./getOwnerOwningTime');
const { nbsp } = require('auto-core/react/lib/html-entities');

it('должен вернуть от и до, если есть даты', () => {
    const owner = {
        index: 1,
        time_from: '1498899600000',
        time_to: '1507798800000',
    };

    expect(getOwnerOwningTime(owner)).toEqual('1 июля 2017 — 12 октября 2017');
});

it('должен вернуть только от, если нет даты окончания', () => {
    const owner = {
        index: 1,
        time_from: '1498899600000',
    };

    expect(getOwnerOwningTime(owner)).toEqual('1 июля 2017 — настоящее время');
});

describe('продолжительность', () => {
    it('должен вернуть от и до и указать продолжительность в днях', () => {
        const owner = {
            index: 1,
            time_from: '1506848400000',
            time_to: '1507798800000',
        };

        expect(getOwnerOwningTime(owner, true)).toEqual('1 октября 2017 — 12 октября 2017 (11 дней)');
    });

    it('должен вернуть от и до и указать продолжительность в 1 день, если меньше суток 0', () => {
        const owner = {
            index: 1,
            time_from: '1507712400000',
            time_to: '1507798800000',
        };

        expect(getOwnerOwningTime(owner, true)).toEqual('11 октября 2017 — 12 октября 2017 (1 день)');
    });

    it('должен вернуть от и до и указать продолжительность в 1 день, если разница 0', () => {
        const owner = {
            index: 1,
            time_from: '1507798800000',
            time_to: '1507798800000',
        };

        expect(getOwnerOwningTime(owner, true)).toEqual('12 октября 2017 — 12 октября 2017 (1 день)');
    });

    it('должен вернуть от и до и указать продолжительность в месяцах 1', () => {
        const owner = {
            index: 1,
            time_from: '1435741200000',
            time_to: '1439499600000',
        };

        expect(getOwnerOwningTime(owner, true)).toEqual(`1 июля 2015 — 14 августа 2015 (1.5${ nbsp }месяца)`);
    });

    it('должен вернуть от и до и указать продолжительность в месяцах 2', () => {
        const owner = {
            index: 1,
            time_from: '1435741200000',
            time_to: '1441065800000',
        };

        expect(getOwnerOwningTime(owner, true)).toEqual(`1 июля 2015 — 1 сентября 2015 (2${ nbsp }месяца)`);
    });

    it('должен вернуть от и до и указать продолжительность в месяцах 3', () => {
        const owner = {
            index: 1,
            time_from: '1498899600000',
            time_to: '1507798800000',
        };

        expect(getOwnerOwningTime(owner, true)).toEqual('1 июля 2017 — 12 октября 2017 (3.5 месяца)');
    });

    it('должен вернуть от и до и указать продолжительность в годах и месяцах', () => {
        const owner = {
            index: 1,
            time_from: '1435741200000',
            time_to: '1507798800000',
        };

        expect(getOwnerOwningTime(owner, true)).toEqual(`1 июля 2015 — 12 октября 2017 (2${ nbsp }года${ nbsp }4${ nbsp }месяца)`);
    });

    it('продолжительность почти 8 лет, но поменьше', () => {
        const owner = {
            index: 1,
            time_from: '1378497600000',
            time_to: '1629907854682',
        };
        const result = getOwnerOwningTime(owner, true);

        expect(result).toEqual('7 сентября 2013 — 25 августа 2021 (8 лет)');
    });

    it('должен вернуть от и до и указать продолжительность в годах', () => {
        const owner = {
            index: 1,
            time_from: '1435741200000',
            time_to: '1530446400000',
        };

        expect(getOwnerOwningTime(owner, true)).toEqual('1 июля 2015 — 1 июля 2018 (3 года)');
    });

    it('продолжительность почти 1 год, но побольше', () => {
        const owner = {
            index: 1,
            time_from: '1378497600000',
            time_to: '1410120000000',
        };
        const result = getOwnerOwningTime(owner, true);

        expect(result).toEqual(`7 сентября 2013 — 8 сентября 2014 (1${ nbsp }год${ nbsp }1${ nbsp }месяц)`);
    });

    it('продолжительность почти 1 год, но поменьше, округляется', () => {
        const owner = {
            index: 1,
            time_from: '1378497600000',
            time_to: '1409947200000',
        };
        const result = getOwnerOwningTime(owner, true);

        expect(result).toEqual('7 сентября 2013 — 6 сентября 2014 (1 год)');
    });
});
