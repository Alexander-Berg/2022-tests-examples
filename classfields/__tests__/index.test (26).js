import MockDate from 'mockdate';

import { getFileName } from '../';

describe('getFileName', () => {
    beforeEach(() => {
        MockDate.set('12.23.2018');
    });

    afterEach(() => {
        MockDate.reset();
    });

    it('generates filename without type and category', () => {
        expect(getFileName()).toEqual('offers-23-12-2018.xls');
    });

    it('generates filename with only category', () => {
        expect(getFileName('COMMERCIAL')).toEqual('offers-commercial-23-12-2018.xls');
    });

    it('generates filename with only type', () => {
        expect(getFileName(null, 'RENT')).toEqual('offers-rent-23-12-2018.xls');
    });

    it('generates filename for apartment sell', () => {
        expect(getFileName('APARTMENT', 'SELL')).toEqual('offers-sell-apartment-23-12-2018.xls');
    });

    it('generates filename for garage rent', () => {
        expect(getFileName('GARAGE', 'RENT')).toEqual('offers-rent-garage-23-12-2018.xls');
    });

    it('generates filename for short rent', () => {
        expect(getFileName('HOUSE', 'RENT', 'SHORT'))
            .toEqual('offers-rent-short-house-23-12-2018.xls');
    });

    it('generates filename for long rent', () => {
        expect(getFileName('HOUSE', 'RENT', 'LARGE'))
            .toEqual('offers-rent-long-house-23-12-2018.xls');
    });
});
