import {runParser} from '../parser';
import {customParam, numericParam, oneOfParam, param, placeholder, route} from '../route';

describe('customParam', () => {
    test('reads values and passes it to callback argument', () => {
        const parser = customParam('foo', (input) => (input === 'yes' ? 'ok' : null));

        expect(runParser(parser, [])).toEqual(null);
        expect(runParser(parser, ['no'])).toEqual(null);
        expect(runParser(parser, ['yes'])).toEqual({foo: 'ok'});
    });
});

describe('numericParam', () => {
    test('reads first value and parses number', () => {
        const parser = numericParam('foo');

        expect(runParser(parser, [])).toEqual(null);
        expect(runParser(parser, ['x'])).toEqual(null);
        expect(runParser(parser, ['1'])).toEqual({foo: 1});
    });
});

describe('oneOfParam', () => {
    test('checks if value is in list', () => {
        const parser = oneOfParam('foo', ['a', 'b', 'c']);

        expect(runParser(parser, [])).toEqual(null);
        expect(runParser(parser, ['a'])).toEqual({foo: 'a'});
        expect(runParser(parser, ['b'])).toEqual({foo: 'b'});
        expect(runParser(parser, ['c'])).toEqual({foo: 'c'});
        expect(runParser(parser, ['z'])).toEqual(null);
    });
});

describe('param', () => {
    test('reads first value', () => {
        const parser = param('foo');

        expect(runParser(parser, [])).toEqual(null);
        expect(runParser(parser, ['x'])).toEqual({foo: 'x'});
    });
});

describe('placeholder', () => {
    test('succeeds only if input is equal to placeholder value', () => {
        const parser = placeholder('foo');

        expect(runParser(parser, [])).toEqual(null);
        expect(runParser(parser, ['foo'])).toEqual({});
        expect(runParser(parser, ['bar'])).toEqual(null);
    });
});

describe('route', () => {
    test('transforms strings into placeholders', () => {
        const parser = route('foo', 'bar', 'biz');

        expect(runParser(parser, [])).toEqual(null);
        expect(runParser(parser, ['foo', 'bar'])).toEqual(null);
        expect(runParser(parser, ['foo', 'bar', 'biz'])).toEqual({});
    });

    test('handles params', () => {
        const parser = route('foo', param('id'), 'biz');

        expect(runParser(parser, [])).toEqual(null);
        expect(runParser(parser, ['foo', 'bar'])).toEqual(null);
        expect(runParser(parser, ['foo', 'bar', 'biz'])).toEqual({id: 'bar'});
    });
});
