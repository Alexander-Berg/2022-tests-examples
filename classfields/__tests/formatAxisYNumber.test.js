import { formatAxisYNumber } from '../';

describe('formatAxisYNumber', () => {
    it("doesn't round numbers less than 1000", () => {
        expect(formatAxisYNumber(5)).toEqual('5');
        expect(formatAxisYNumber(55)).toEqual('55');
        expect(formatAxisYNumber(555)).toEqual('555');
    });

    it('rounds numbers between 1000 and 10000 to hundreds', () => {
        expect(formatAxisYNumber(1666)).toEqual('1\u00a0700');
        expect(formatAxisYNumber(9444)).toEqual('9\u00a0400');
    });

    it('rounds numbers more than 10 000 to thousands', () => {
        expect(formatAxisYNumber(10501)).toEqual('11\u00a0000');
        expect(formatAxisYNumber(1000499)).toEqual('1\u00a0000\u00a0000');
    });
});
