const preparer = require('./wanted');
const _ = require('lodash');

const DATA = {
    report: {
        constraints: {
            status: 'OK',
        },
    },
};

const testForStatus = ({ isCardPage, result, status }) => {
    it(`wanted === ${ status }`, () => {
        const item = { status };
        const data = preparer({ item, result, isCardPage });

        expect(data).toMatchSnapshot();
    });
};

describe('карточка', () => {
    describe('при наличии CONSTRAINT айтем WANTED нужно удалить', () => {
        const result = _.cloneDeep(DATA);

        [ 'OK', 'ERROR', 'UNKNOWN' ].forEach((status) => {
            testForStatus({ status, result, isCardPage: true });
        });
    });

    describe('нет CONSTRAINT, возвращаются правильные тексты', () => {
        const result = {};

        [ 'OK', 'ERROR', 'UNKNOWN' ].forEach((status) => {
            testForStatus({ status, result, isCardPage: true });
        });
    });
});

describe('полный отчет', () => {
    describe('отстутствие CONSTRAINT не влияет на тексты', () => {
        const result = _.cloneDeep(DATA);
        result.report.constraints.status = 'ERROR';

        [ 'OK', 'ERROR', 'UNKNOWN' ].forEach((status) => {
            testForStatus({ status, result, isCardPage: false });
        });
    });

    describe('наличие CONSTRAINT не влияет на тексты', () => {
        const result = {};

        [ 'OK', 'ERROR', 'UNKNOWN' ].forEach((status) => {
            testForStatus({ status, result, isCardPage: false });
        });
    });
});
