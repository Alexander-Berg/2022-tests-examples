const preparer = require('./constraints');
const _ = require('lodash');

const DATA = {
    report: {
        wanted: {
            status: 'OK',
        },
    },
};

const testForStatus = ({ isCardPage, result, status }) => {
    it(`constraints === ${ status }`, () => {
        const item = { status };
        const data = preparer({ item, result, isCardPage });

        expect(data).toMatchSnapshot();
    });
};

describe('карточка', () => {
    describe('есть wanted === OK', () => {
        const result = _.cloneDeep(DATA);

        [ 'OK', 'ERROR', 'UNKNOWN' ].forEach((status) => {
            testForStatus({ status, result, isCardPage: true });
        });
    });

    describe('есть wanted === ERROR', () => {
        const result = _.cloneDeep(DATA);
        result.report.wanted.status = 'ERROR';

        [ 'OK', 'ERROR', 'UNKNOWN' ].forEach((status) => {
            testForStatus({ status, result, isCardPage: true });
        });
    });

    describe('есть wanted === UNKNOWN', () => {
        const result = _.cloneDeep(DATA);
        result.report.wanted.status = 'UNKNOWN';

        [ 'OK', 'ERROR', 'UNKNOWN' ].forEach((status) => {
            testForStatus({ status, result, isCardPage: true });
        });
    });

    describe('нет wanted', () => {
        const result = {};

        [ 'OK', 'ERROR', 'UNKNOWN' ].forEach((status) => {
            testForStatus({ status, result, isCardPage: true });
        });
    });
});

describe('полный отчет', () => {
    // Для примера взял wanted === ERROR, чтобы показать,
    // что он не влияет на результат
    describe('есть wanted === ERROR', () => {
        const result = _.cloneDeep(DATA);
        result.report.wanted.status = 'ERROR';

        [ 'OK', 'ERROR', 'UNKNOWN' ].forEach((status) => {
            testForStatus({ status, result, isCardPage: false });
        });
    });

    describe('нет wanted', () => {
        const result = {};

        [ 'OK', 'ERROR', 'UNKNOWN' ].forEach((status) => {
            testForStatus({ status, result, isCardPage: false });
        });
    });
});
