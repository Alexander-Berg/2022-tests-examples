import { stringifyQuery, parseQuery } from '../location';

describe('stringifyQuery', () => {
    it('converts a simple object to query string', () => {
        const query = { a: 1, b: '2' };

        expect(stringifyQuery(query)).toBe('?a=1&b=2');
    });

    it('converts an object with an array property', () => {
        const query = { a: [ 1, 2 ] };

        expect(stringifyQuery(query)).toBe('?a=1&a=2');
    });

    it('converts an object with encoding', () => {
        const query = { a: 'test,test' };

        expect(stringifyQuery(query, true)).toBe('?a=test%2Ctest');
    });
});

describe('parseQuery', () => {
    it('converts simple query string into an object', () => {
        const query = '?a=1&b=some_value';

        expect(parseQuery(query)).toEqual({ a: '1', b: 'some_value' });
    });

    it('converts query string with a sequence into js array', () => {
        const query = '?test=1&test=2&test=3';

        expect(parseQuery(query)).toEqual({ test: [ '1', '2', '3' ] });
    });
});
