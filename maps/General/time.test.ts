import * as time from './time';

describe('lib/time', () => {
    describe('formatDate', () => {
        const originalDateNow = Date.now;

        afterAll(() => {
            Date.now = originalDateNow;
        });

        const targetDate = new Date('2018-10-03T21:16:53.785760');

        function matchDate(dates: string[], expectResult: string): void {
            dates.forEach((dateNow) => {
                it(`should return with ${dateNow}`, () => {
                    Date.now = () => {
                        const newDateObject = new Date(dateNow);
                        return newDateObject.getTime() - (newDateObject.getTimezoneOffset() * time.MINUTE_MILLISECONDS);
                    };

                    expect(time.formatDate(targetDate)).toBe(expectResult);
                });
            });
        }

        describe('today', () => {
            matchDate(
                [
                    '2018-10-03T23:59:59.785760',
                    '2018-10-03T21:16:53.785760',
                    '2018-10-03T11:16:53.785760',
                    '2018-10-03T00:01:53.785760',
                    '2018-10-03T00:00:00'
                ],
                'Сегодня 21:16'
            );
        });

        describe('yesterday', () => {
            matchDate(
                [
                    '2018-10-04T23:59:59.785760',
                    '2018-10-04T23:59:59',
                    '2018-10-04T21:16:53.785760',
                    '2018-10-04T11:16:53.785760',
                    '2018-10-04T00:01:53.785760',
                    '2018-10-04T00:00:00'
                ],
                'Вчера 21:16'
            );
        });

        describe('common date', () => {
            matchDate(
                [
                    '2018-11-04T23:59:59.785760',
                    '2018-11-04T23:59:59',
                    '2018-11-04T21:16:53.785760',
                    '2018-11-04T11:16:53.785760',
                    '2018-11-04T00:01:53.785760',
                    '2010-11-04T00:01:53.785760',
                    '2018-11-04T00:00:00'
                ],
                '21:16, 03.10.2018'
            );
        });
    });
});
