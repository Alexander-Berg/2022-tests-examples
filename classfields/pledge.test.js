const preparer = require('./pledge');

const testForStatus = ({ isCardPage, result, status }) => {
    it(`status === ${ status }`, () => {
        const item = { status };
        const data = preparer({ item, result, isCardPage });

        expect(data).toMatchSnapshot();
    });
};

describe('карточка', () => {
    describe('pledge возвращает нормальные тексты', () => {
        [ '""', 'OK', 'ERROR', 'UNKNOWN' ].forEach((status) => {
            testForStatus({ status, isCardPage: true });
        });
    });
});

describe('полный отчет', () => {
    describe('pledge возвращает нормальные тексты', () => {
        [ '""', 'OK', 'ERROR', 'UNKNOWN' ].forEach((status) => {
            testForStatus({ status, isCardPage: false });
        });
    });
});
