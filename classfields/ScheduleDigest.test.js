jest.mock('@app/libs/logger', () => ({
    info: () => {},
    error: () => {},
}));

jest.mock('@app/handlers/digests', () => ({
    SendDigest: () => {},
}));

jest.mock('@app/libs/scheduler', () => ({
    scheduleTask: () => {},
    scheduleEvent: () => {}
}));
const Scheduler = require('@app/libs/scheduler');

const { ScheduleDigest } = require('./ScheduleDigest');

describe('Планирование дайджеста', () => {
    it('Планирует дайджест на сегодня, если есть время', () => {
        jest
            .useFakeTimers()
            .setSystemTime(new Date('2021-12-01 07:00').getTime());

        let scheduleId;
        let scheduleFireTime;
        const sendDigestSpy = jest.spyOn(Scheduler, 'scheduleEvent').mockImplementation(({ id, fireTime }) => {
            scheduleId = id;
            scheduleFireTime = fireTime;
        });
        ScheduleDigest();

        expect(sendDigestSpy).toBeCalledTimes(1);
        expect(scheduleId).toBe('digest');
        expect(scheduleFireTime).toEqual(new Date('2021-12-01 09:00'));

        sendDigestSpy.mockClear();
        jest.clearAllTimers();
    });

    it('Планирует дайджест на завтра, если есть сегодня нет времени', () => {
        jest
            .useFakeTimers()
            .setSystemTime(new Date('2021-12-01 10:00').getTime());

        let scheduleId;
        let scheduleFireTime;
        const sendDigestSpy = jest.spyOn(Scheduler, 'scheduleEvent').mockImplementation(({ id, fireTime }) => {
            scheduleId = id;
            scheduleFireTime = fireTime;
        });
        ScheduleDigest();

        expect(sendDigestSpy).toBeCalledTimes(1);
        expect(scheduleId).toBe('digest');
        expect(scheduleFireTime).toEqual(new Date('2021-12-02 09:00'));

        sendDigestSpy.mockClear();
        jest.clearAllTimers();
    });

    it('Планирует дайджест на сегодня, если сегодня пятница и есть время', () => {
        jest
            .useFakeTimers()
            .setSystemTime(new Date('2021-12-03 08:00').getTime());

        let scheduleId;
        let scheduleFireTime;
        const sendDigestSpy = jest.spyOn(Scheduler, 'scheduleEvent').mockImplementation(({ id, fireTime }) => {
            scheduleId = id;
            scheduleFireTime = fireTime;
        });
        ScheduleDigest();

        expect(sendDigestSpy).toBeCalledTimes(1);
        expect(scheduleId).toBe('digest');
        expect(scheduleFireTime).toEqual(new Date('2021-12-03 09:00'));

        sendDigestSpy.mockClear();
        jest.clearAllTimers();
    });

    it('Планирует дайджест на понедельник, если сегодня пятница и нет времени', () => {
        jest
            .useFakeTimers()
            .setSystemTime(new Date('2021-12-03 10:00').getTime());

        let scheduleId;
        let scheduleFireTime;
        const sendDigestSpy = jest.spyOn(Scheduler, 'scheduleEvent').mockImplementation(({ id, fireTime }) => {
            scheduleId = id;
            scheduleFireTime = fireTime;
        });
        ScheduleDigest();

        expect(sendDigestSpy).toBeCalledTimes(1);
        expect(scheduleId).toBe('digest');
        expect(scheduleFireTime).toEqual(new Date('2021-12-06 09:00'));

        sendDigestSpy.mockClear();
        jest.clearAllTimers();
    });

    it('Планирует дайджест на понедельник, если сегодня суббота', () => {
        jest
            .useFakeTimers()
            .setSystemTime(new Date('2021-12-04 08:00').getTime());

        let scheduleId;
        let scheduleFireTime;
        const sendDigestSpy = jest.spyOn(Scheduler, 'scheduleEvent').mockImplementation(({ id, fireTime }) => {
            scheduleId = id;
            scheduleFireTime = fireTime;
        });
        ScheduleDigest();

        expect(sendDigestSpy).toBeCalledTimes(1);
        expect(scheduleId).toBe('digest');
        expect(scheduleFireTime).toEqual(new Date('2021-12-06 09:00'));

        sendDigestSpy.mockClear();
        jest.clearAllTimers();
    });

    it('Планирует дайджест на понедельник, если сегодня воскресенье', () => {
        jest
            .useFakeTimers()
            .setSystemTime(new Date('2021-12-05 08:00').getTime());

        let scheduleId;
        let scheduleFireTime;
        const sendDigestSpy = jest.spyOn(Scheduler, 'scheduleEvent').mockImplementation(({ id, fireTime }) => {
            scheduleId = id;
            scheduleFireTime = fireTime;
        });
        ScheduleDigest();

        expect(sendDigestSpy).toBeCalledTimes(1);
        expect(scheduleId).toBe('digest');
        expect(scheduleFireTime).toEqual(new Date('2021-12-06 09:00'));

        sendDigestSpy.mockClear();
        jest.clearAllTimers();
    });
});