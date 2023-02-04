import { convertDateToMonth } from '../convert-date-to-month';

describe('convertDateToMonth', () => {
    test('Jenuary, 2017', () => {
        expect(convertDateToMonth('2017-01-01')).toEqual('012017');
    });

    test('May, 2018', () => {
        expect(convertDateToMonth('2018-05-01')).toEqual('052018');
    });

    test('June, 2018', () => {
        expect(convertDateToMonth('2018-06-01')).toEqual('062018');
    });
});
