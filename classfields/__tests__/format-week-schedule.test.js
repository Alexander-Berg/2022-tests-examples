import { formatWeekSchedule } from '../format-week-schedule';

describe('formatWeekTimetable', () => {
    it('correct format one day', () => {
        const schedule = [ { day: 'MONDAY', minutesFrom: 1200, minutesTo: 1400 } ];

        expect(formatWeekSchedule(schedule)).toEqual('Пн с 20:00 до 23:20');
    });

    it('correct format two days', () => {
        const schedule = [
            { day: 'MONDAY', minutesFrom: 600, minutesTo: 1200 },
            { day: 'FRIDAY', minutesFrom: 600, minutesTo: 1200 }
        ];

        expect(formatWeekSchedule(schedule)).toEqual('Пн, Пт с 10:00 до 20:00');
    });

    it('correct format interval days', () => {
        const schedule = [
            { day: 'MONDAY', minutesFrom: 700, minutesTo: 1100 },
            { day: 'TUESDAY', minutesFrom: 700, minutesTo: 1100 },
            { day: 'WEDNESDAY', minutesFrom: 700, minutesTo: 1100 }
        ];

        expect(formatWeekSchedule(schedule)).toEqual('Пн - Ср с 11:40 до 18:20');
    });

    it('correct format interval and separated day', () => {
        const schedule = [
            { day: 'MONDAY', minutesFrom: 700, minutesTo: 1100 },
            { day: 'TUESDAY', minutesFrom: 700, minutesTo: 1100 },
            { day: 'WEDNESDAY', minutesFrom: 700, minutesTo: 1100 },
            { day: 'SATURDAY', minutesFrom: 700, minutesTo: 1100 }
        ];

        expect(formatWeekSchedule(schedule)).toEqual('Пн - Ср, Сб с 11:40 до 18:20');
    });

    it('correct format two interval and separated day', () => {
        const schedule = [
            { day: 'MONDAY', minutesFrom: 700, minutesTo: 1100 },
            { day: 'TUESDAY', minutesFrom: 700, minutesTo: 1100 },
            { day: 'THURSDAY', minutesFrom: 700, minutesTo: 1100 },
            { day: 'FRIDAY', minutesFrom: 700, minutesTo: 1100 },
            { day: 'SUNDAY', minutesFrom: 700, minutesTo: 1100 }
        ];

        expect(formatWeekSchedule(schedule)).toEqual('Пн - Вт, Чт - Пт, Вс с 11:40 до 18:20');
    });

    it('correct format everyday', () => {
        const schedule = [
            { day: 'MONDAY', minutesFrom: 700, minutesTo: 1100 },
            { day: 'TUESDAY', minutesFrom: 700, minutesTo: 1100 },
            { day: 'WEDNESDAY', minutesFrom: 700, minutesTo: 1100 },
            { day: 'THURSDAY', minutesFrom: 700, minutesTo: 1100 },
            { day: 'FRIDAY', minutesFrom: 700, minutesTo: 1100 },
            { day: 'SATURDAY', minutesFrom: 700, minutesTo: 1100 },
            { day: 'SUNDAY', minutesFrom: 700, minutesTo: 1100 }
        ];

        expect(formatWeekSchedule(schedule)).toEqual('Ежедневно с 11:40 до 18:20');
    });

    it('correct pad minutes and hours', () => {
        const schedule = [
            { day: 'MONDAY', minutesFrom: 10, minutesTo: 100 }
        ];

        expect(formatWeekSchedule(schedule)).toEqual('Пн с 00:10 до 01:40');
    });
});
