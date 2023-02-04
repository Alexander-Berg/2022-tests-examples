import utmsForStat, { UTMS_NAME, EXPIRED_NAME } from './utm';

let neededParams = {};

beforeEach(() => {
    neededParams = { from: 'test' };
});

describe('method "utmsForStat.set"', () => {
    it('withot params must Set nothing', () => {
        utmsForStat.set();

        const res = localStorage.getItem(UTMS_NAME);
        expect(JSON.parse(res as string)).toBeNull();
    });

    it('withot params must Get empty object', () => {
        const res = utmsForStat.get();
        expect(res).toEqual({});
    });

    it('with needed params must Set correct object', () => {
        utmsForStat.set(neededParams);

        const res = localStorage.getItem(UTMS_NAME);
        expect(JSON.parse(res as string)).toEqual(neededParams);
    });

    it('with needed and another params must Set correct object', () => {
        utmsForStat.set({
            ...neededParams,
            test: 'test',
        });

        const res = localStorage.getItem(UTMS_NAME);
        expect(JSON.parse(res as string)).toEqual(neededParams);
    });

    it('with needed params must Get correct object', () => {
        utmsForStat.set(neededParams);

        const res = utmsForStat.get();
        expect(res).toEqual(neededParams);
    });

    it('при установке новых параметров затирает старые', () => {
        utmsForStat.set({
            ...neededParams,
            utm_source: 'utm_source',
        });

        utmsForStat.set({
            ...neededParams,
        });

        const res = localStorage.getItem(UTMS_NAME);
        expect(JSON.parse(res as string)).toEqual(neededParams);
    });
});

describe('expires', () => {
    it('обновляет дату, если были установлены новые параметры', () => {
        return new Promise<void>((done) => {
            utmsForStat.set({
                ...neededParams,
            });

            const expiredOld = localStorage.getItem(EXPIRED_NAME);

            setTimeout(() => {
                utmsForStat.set({
                    ...neededParams,
                });

                const expiredUpdated = localStorage.getItem(EXPIRED_NAME);
                expect(expiredUpdated !== expiredOld).toBe(true);

                done();
            }, 10);
        });
    });

    it('не обновляет дату, если не были установлены новые параметры', () => {
        return new Promise<void>((done) => {
            utmsForStat.set({
                ...neededParams,
            });

            const expiredOld = localStorage.getItem(EXPIRED_NAME);

            setTimeout(() => {
                utmsForStat.set();

                const expiredUpdated = localStorage.getItem(EXPIRED_NAME);

                expect(expiredUpdated === expiredOld).toBe(true);

                done();
            }, 10);
        });
    });

    it('не обновляет дату, стирает данные, если дата истекла', () => {
        return new Promise<void>((done) => {
            utmsForStat.set({
                ...neededParams,
            });

            localStorage.setItem(EXPIRED_NAME, '0');

            setTimeout(() => {
                utmsForStat.set();

                const expiredUpdated = localStorage.getItem(EXPIRED_NAME);

                expect(expiredUpdated === '0').toBe(true);

                const res = localStorage.getItem(UTMS_NAME);
                expect(JSON.parse(res as string)).toEqual({});

                done();
            }, 10);
        });
    });
});
