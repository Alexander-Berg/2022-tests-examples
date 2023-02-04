import drawRange from '../';

describe('EGRNgetOwnerText', () => {
    it('should return empty string when given no arguments', () => {
        expect(drawRange()).toEqual('');
    });

    it('should return empty string when given falsy value as argument', () => {
        expect(drawRange(null)).toEqual('');
        expect(drawRange(undefined)).toEqual('');
        expect(drawRange(NaN)).toEqual('');
        expect(drawRange(0)).toEqual('');
        expect(drawRange(false)).toEqual('');
    });

    it('should return empty string when given empty object as argument', () => {
        expect(drawRange({})).toEqual('');
    });

    it('should correctly parse falsy "from" and "to" values', () => {
        expect(drawRange({ from: NaN, to: NaN, prefix: '$' })).toEqual('');
        expect(drawRange({ from: null, to: null, prefix: '$' })).toEqual('');
        expect(drawRange({ from: undefined, to: undefined, prefix: '$' })).toEqual('');
        expect(drawRange({ from: '', to: '', prefix: '$' })).toEqual('');
        expect(drawRange({ from: false, to: false, prefix: '$' })).toEqual('');
        expect(drawRange({ from: 0, to: 0, prefix: '$' })).toEqual('$ 0');
    });

    it('should correctly draw range with "from" value only', () => {
        expect(drawRange({ from: 12 })).toEqual('12');
    });

    it('should correctly draw range with "to" value only', () => {
        expect(drawRange({ to: 12 })).toEqual('12');
    });

    it('should correctly draw range with both "from" and "to" values', () => {
        expect(drawRange({ from: 12, to: 15 })).toEqual('12 — 15');
    });

    it('should correctly draw range with equal "from" and "to" values', () => {
        expect(drawRange({ from: 12, to: 12 })).toEqual('12');
    });

    it('should correctly draw "format: 2" range with "from" value only', () => {
        expect(drawRange({ from: 12, format: 2 })).toEqual('от\u00a012');
    });

    it('should correctly draw "format: 2" range with "to" value only', () => {
        expect(drawRange({ to: 12, format: 2 })).toEqual('до\u00a012');
    });

    it('should correctly draw "format: 2" range with both "from" and "to" values', () => {
        expect(drawRange({ from: 12, to: 15, format: 2 })).toEqual('от\u00a012 до\u00a015');
    });

    it('should correctly draw "format: 2" range with equal "from" and "to" values', () => {
        expect(drawRange({ from: 12, to: 12, format: 2 })).toEqual('от\u00a012');
    });

    it('should correctly insert &nbsp instead of spaces', () => {
        expect(drawRange({
            from: 12,
            to: 15,
            isNoWrap: true
        })).toEqual('12\u00a0—\u00a015');
    });

    it('should correctly draw simply formatted string with custom delimiter', () => {
        expect(drawRange({ from: 12, to: 15, delimiter: '^' })).toEqual('12 ^ 15');
    });

    it('should correctly draw simply formatted string with prefix', () => {
        expect(drawRange({ from: 12, to: 15, prefix: '^' })).toEqual('^ 12 — 15');
    });

    it('should correctly draw simply formatted string with postfix', () => {
        expect(drawRange({ from: 12, to: 15, postfix: '^' })).toEqual('12 — 15 ^');
    });

    it('should correctly draw simply formatted string with prefix and postfix', () => {
        expect(drawRange({ from: 12, to: 15, prefix: '^', postfix: '&' })).toEqual('^ 12 — 15 &');
    });
});
