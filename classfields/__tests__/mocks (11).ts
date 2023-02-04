export const simpleData = [
    {
        dayFrom: 1,
        dayTo: 5,
        timePattern: [
            {
                open: '10:00',
                close: '18:00',
            },
        ],
    },
];

export const complicatedTimePatternData = [
    {
        dayFrom: 1,
        dayTo: 5,
        timePattern: [
            {
                open: '10:00',
                close: '14:00',
            },
            {
                open: '16:00',
                close: '18:00',
            },
        ],
    },
];

export const oneDayData = [
    {
        dayFrom: 3,
        dayTo: 3,
        timePattern: [
            {
                open: '10:00',
                close: '18:00',
            },
        ],
    },
];

export const roundTheClockData = [
    {
        dayFrom: 1,
        dayTo: 7,
        timePattern: [
            {
                open: '00:00',
                close: '23:59',
            },
        ],
    },
];

export const simpleDataLateClosing = [
    {
        dayFrom: 1,
        dayTo: 5,
        timePattern: [
            {
                open: '10:00',
                close: '23:00',
            },
        ],
    },
];
