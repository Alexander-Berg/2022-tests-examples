const processTimingsHeader = require('./processTimingsHeader');

const CASES = {
    onlyName: {
        value: 'missedCache',
        result: {
            "missedCache": {
                description: null,
                numericValue: null,
            },
        },
    },
    nameAndValue: {
        value: 'cpu;dur=2.4',
        result: {
            "cpu": {
                description: null,
                numericValue: 2.4,
            },
        },
    },
    NameValueAndDesc: {
        value: 'cache;desc="Cache Read";dur=23.2',
        result: {
            "cache": {
                description: "Cache Read",
                numericValue: 23.2,
            },
        },
    },
    twoMetrics: {
        value: 'db;dur=53, app;dur=47.2',
        result: {
            "app": {
                description: null,
                numericValue: 47.2,
            },
            "db": {
                description: null,
                numericValue: 53,
            },
        },
    },
    full: {
        value: 'db;dur=53,app;dur=47.2,cache;desc="Cache Read";dur=23.2,total;dur=123.4',
        result: {
            "app": {
                description: null,
                numericValue: 47.2,
            },
            "cache": {
                description: "Cache Read",
                numericValue: 23.2,
            },
            "db": {
                description: null,
                numericValue: 53,
            },
            "total": {
                description: null,
                numericValue: 123.4,
            },
        },
    },
    fullWithSpaces: {
        value: 'db; dur=53, app; dur=47.2, cache; desc="Cache Read"; dur=23.2, total;dur=123.4455;desc="Total"',
        result: {
            "app": {
                description: null,
                numericValue: 47.2,
            },
            "cache": {
                description: "Cache Read",
                numericValue: 23.2,
            },
            "db": {
                description: null,
                numericValue: 53,
            },
            "total": {
                description: "Total",
                numericValue: 123.45,
            },
        },
    },
};

Object.keys(CASES).forEach(caseName => {
    const caseData = CASES[caseName];

    it(caseName, () => {
        expect(processTimingsHeader({
            value: caseData.value,
        })).toEqual(caseData.result);
    });
});