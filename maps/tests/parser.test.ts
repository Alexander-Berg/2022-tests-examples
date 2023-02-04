import Parser, {mapOutput, optionalSequence, sequence, runParser} from '../parser';

const fooParser: Parser<{foo: boolean}> = (input) =>
    input[0] === 'foo'
        ? {
              input: input.slice(1),
              output: {foo: true}
          }
        : null;
const barParser: Parser<{bar: boolean}> = (input) =>
    input[0] === 'bar'
        ? {
              input: input.slice(1),
              output: {bar: true}
          }
        : null;

describe('runParser', () => {
    test('returns last output value if succeed', () => {
        expect(runParser(fooParser, ['foo'])).toEqual({foo: true});
    });

    test('returns null if failed', () => {
        expect(runParser(fooParser, [])).toEqual(null);
        expect(runParser(fooParser, ['foo', 'bar'])).toEqual(null);
    });
});

describe('mapOutput', () => {
    test('replaces output value', () => {
        const parser: Parser<{value: string}> = (input) =>
            input[0]
                ? {
                      input: input.slice(1),
                      output: {value: input[0]}
                  }
                : null;
        const modifiedParser = mapOutput(parser, (output) =>
            output.value === 'x' ? {value: output.value + output.value} : null
        );

        expect(runParser(modifiedParser, [])).toEqual(null);
        expect(runParser(modifiedParser, ['x'])).toEqual({value: 'xx'});
        expect(runParser(modifiedParser, ['x', 'y'])).toEqual(null);
        expect(runParser(modifiedParser, ['y'])).toEqual(null);
    });
});

describe('sequence', () => {
    const combinedParser = sequence(fooParser, barParser);

    test('merges outputs if both succeed', () => {
        expect(runParser(combinedParser, ['foo', 'bar'])).toEqual({foo: true, bar: true});
    });

    test('returns null if any is failed', () => {
        expect(runParser(combinedParser, [])).toEqual(null);
        expect(runParser(combinedParser, ['foo'])).toEqual(null);
        expect(runParser(combinedParser, ['bar'])).toEqual(null);
        expect(runParser(combinedParser, ['y'])).toEqual(null);
    });
});

describe('optionalSequence', () => {
    test('goes to next parser if failed', () => {
        const parser = optionalSequence(fooParser, barParser);

        expect(runParser(parser, [])).toEqual(null);
        expect(runParser(parser, ['foo'])).toEqual({foo: true});
        expect(runParser(parser, ['bar'])).toEqual({bar: true});
        expect(runParser(parser, ['foo', 'bar'])).toEqual(null);
        expect(runParser(parser, ['y'])).toEqual(null);
    });
});
