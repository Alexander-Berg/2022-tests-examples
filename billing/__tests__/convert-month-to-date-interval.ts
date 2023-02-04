import { convertMonthToDateInterval } from '../convert-month-to-date-interval';

describe('convertMonthToDateInterval', () => {
    test('Jenuary, 2017', () => {
        expect(convertMonthToDateInterval('012017')).toEqual(['2017-01-01', '2017-01-31']);
    });

    test('May, 2018', () => {
        expect(convertMonthToDateInterval('052018')).toEqual(['2018-05-01', '2018-05-31']);
    });

    test('June, 2018', () => {
        expect(convertMonthToDateInterval('062018')).toEqual(['2018-06-01', '2018-06-30']);
    });

    test('September, 2018', () => {
        expect(convertMonthToDateInterval('092018')).toEqual(['2018-09-01', '2018-09-30']);
    });

    test('December, 2019', () => {
        expect(convertMonthToDateInterval('122019')).toEqual(['2019-12-01', '2019-12-31']);
    });

    /*  Специальный блок для проверки правильности учета високосности года */
    test('February, 1900', () => {
        expect(convertMonthToDateInterval('021900')).toEqual(['1900-02-01', '1900-02-28']);
    });

    test('February, 2000', () => {
        expect(convertMonthToDateInterval('022000')).toEqual(['2000-02-01', '2000-02-29']);
    });

    test('February, 2100', () => {
        expect(convertMonthToDateInterval('022100')).toEqual(['2100-02-01', '2100-02-28']);
    });

    test('February, 2400', () => {
        expect(convertMonthToDateInterval('022400')).toEqual(['2400-02-01', '2400-02-29']);
    });

    test('February, 2004', () => {
        expect(convertMonthToDateInterval('022004')).toEqual(['2004-02-01', '2004-02-29']);
    });

    test('February, 2015', () => {
        expect(convertMonthToDateInterval('022015')).toEqual(['2015-02-01', '2015-02-28']);
    });

    test('February, 2016', () => {
        expect(convertMonthToDateInterval('022016')).toEqual(['2016-02-01', '2016-02-29']);
    });

    test('February, 2017', () => {
        expect(convertMonthToDateInterval('022017')).toEqual(['2017-02-01', '2017-02-28']);
    });

    test('February, 2018', () => {
        expect(convertMonthToDateInterval('022018')).toEqual(['2018-02-01', '2018-02-28']);
    });

    test('February, 2019', () => {
        expect(convertMonthToDateInterval('022019')).toEqual(['2019-02-01', '2019-02-28']);
    });

    test('February, 2020', () => {
        expect(convertMonthToDateInterval('022020')).toEqual(['2020-02-01', '2020-02-29']);
    });
});
