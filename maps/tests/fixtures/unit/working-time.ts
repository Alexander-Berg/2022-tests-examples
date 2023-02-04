import {GetWorkingTimeHours} from 'app/v1/routers/page/helpers/get-working-time';

const fixtures: Record<string, GetWorkingTimeHours> = {
    EverydayAndTwentyForHours: {
        Availabilities: [
            {
                Everyday: true,
                TwentyFourHours: true
            }
        ]
    },

    OpenedNow: {
        Availabilities: [
            {
                Everyday: true,
                Intervals: [
                    {
                        to: '21:00:00',
                        from: '09:00:00'
                    }
                ]
            }
        ]
    },

    TwentyFourHoursWithWeekend: {
        Availabilities: [
            {
                Monday: true,
                Tuesday: true,
                Wednesday: true,
                Thursday: true,
                TwentyFourHours: true
            },
            {
                Friday: true,
                Intervals: [
                    {
                        from: '00:00:00',
                        to: '21:00:00'
                    }
                ]
            }
        ]
    },

    EverydayClosedNow: {
        Availabilities: [
            {
                Everyday: true,
                Intervals: [
                    {
                        from: '12:00:00',
                        to: '21:00:00'
                    }
                ]
            }
        ]
    },

    BreakDaysIntervals: {
        Availabilities: [
            {
                Wednesday: true,
                Thursday: true,
                Intervals: [
                    {
                        from: '12:00:00',
                        to: '21:00:00'
                    }
                ]
            },
            {
                Friday: true,
                Intervals: [
                    {
                        from: '15:00:00',
                        to: '23:00:00'
                    }
                ]
            }
        ]
    },

    DaysIntervals: {
        Availabilities: [
            {
                Wednesday: true,
                Thursday: true,
                Intervals: [
                    {
                        from: '12:00:00',
                        to: '21:00:00'
                    }
                ]
            },
            {
                Friday: true,
                Intervals: [
                    {
                        from: '15:00:00',
                        to: '23:00:00'
                    }
                ]
            }
        ]
    },

    DaysIntervalsWithHight: {
        Availabilities: [
            {
                Wednesday: true,
                Thursday: true,
                Intervals: [
                    {
                        from: '12:00:00',
                        to: '03:00:00'
                    }
                ]
            },
            {
                Friday: true,
                Intervals: [
                    {
                        from: '15:00:00',
                        to: '03:00:00'
                    }
                ]
            }
        ]
    }
};

export {fixtures};
