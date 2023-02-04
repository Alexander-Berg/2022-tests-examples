import {getWorkingTime} from 'app/v1/routers/page/helpers/get-working-time';
import {fixtures} from 'tests/fixtures/unit/working-time';

describe('getWorkingTime():', () => {
    it('Everyday and twenty four hours', async () => {
        const currentDate = new Date('Thu Jul 19 2018 10:24:31 GMT+0300 (MSK)'); // Thursday 10:24:31
        const result = getWorkingTime(fixtures.EverydayAndTwentyForHours, currentDate);
        expect(result).toEqual('Круглосуточно');
    });

    it('Opened now from before day', async () => {
        const currentDate = new Date('Sat Jul 20 2018 00:24:31 GMT+0300 (MSK)'); // Saturday 00:24:31
        const result = getWorkingTime(fixtures.DaysIntervalsWithHight, currentDate);
        expect(result).toEqual('До 3:00');
    });

    it("twenty-four-hours with break days (don't work today)", async () => {
        const currentDate = new Date('Sat Jul 21 2018 10:24:31 GMT+0300 (MSK)'); // Saturday 10:24:31
        const result = getWorkingTime(fixtures.TwentyFourHoursWithWeekend, currentDate);
        expect(result).toEqual('Закрыто до пн.');
    });

    it('Twenty for hours current day. There are break days. (full day)', async () => {
        const currentDate = new Date('Thu Jul 19 2018 10:24:31 GMT+0300 (MSK)'); // Thursday 10:24:31
        const result = getWorkingTime(fixtures.TwentyFourHoursWithWeekend, currentDate);
        expect(result).toEqual('До пт.');
    });

    it('Twenty for hours current day. There are break days. (last day)', async () => {
        const currentDate = new Date('Fri Jul 20 2018 10:24:31 GMT+0300 (MSK)'); // Friday 10:24:31
        const result = getWorkingTime(fixtures.TwentyFourHoursWithWeekend, currentDate);
        expect(result).toEqual('До 21:00');
    });

    it('Opened now', async () => {
        const currentDate = new Date('Thu Jul 19 2018 10:24:31 GMT+0300 (MSK)'); // Thursday 10:24:31
        const result = getWorkingTime(fixtures.OpenedNow, currentDate);
        expect(result).toEqual('До 21:00');
    });

    it('Works everyday, but closed now (morning)', async () => {
        const currentDate = new Date('Thu Jul 19 2018 10:24:31 GMT+0300 (MSK)'); // Thursday 10:24:31
        const result = getWorkingTime(fixtures.EverydayClosedNow, currentDate);
        expect(result).toEqual('С 12:00');
    });

    it('Works everyday, but closed now (evening)', async () => {
        const currentDate = new Date('Thu Jul 19 2018 22:24:31 GMT+0300 (MSK)'); // Thursday 22:24:31
        const result = getWorkingTime(fixtures.EverydayClosedNow, currentDate);
        expect(result).toEqual('С 12:00');
    });

    it('Closed now, but today will be open', async () => {
        const currentDate = new Date('Thu Jul 19 2018 10:24:31 GMT+0300 (MSK)'); // Thursday 10:24:31
        const result = getWorkingTime(fixtures.BreakDaysIntervals, currentDate);
        expect(result).toEqual('С 12:00');
    });

    it('Closed now, will be open tomorrow', async () => {
        const currentDate = new Date('Thu Jul 19 2018 22:24:31 GMT+0300 (MSK)'); // Thursday 10:24:31
        const result = getWorkingTime(fixtures.BreakDaysIntervals, currentDate);
        expect(result).toEqual('С 15:00');
    });

    it("Closed now, won't work tomorrow", async () => {
        const currentDate = new Date('Fri Jul 20 2018 23:24:31 GMT+0300 (MSK)'); // Friday 23:24:31
        const result = getWorkingTime(fixtures.BreakDaysIntervals, currentDate);
        expect(result).toEqual('Закрыто до ср.');
    });
});
