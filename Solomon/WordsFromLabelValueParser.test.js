/* eslint-disable no-undef */
import WordsFromLabelValueParser from './WordsFromLabelValueParser';

function test(input, ...output) {
  const result = WordsFromLabelValueParser.parse(input);

  expect(result.length).toEqual(output.length);

  for (let i = 0; i < output.length; i++) {
    expect(result[i][0]).toEqual(output[i]);
  }
}

describe('WordsFromLabelValueParser', () => {
  it('testSeparators', () => {
    test('abc', 'abc');
    test('abc_def', 'abc', 'def');
    test('abc_def/jhi', 'abc', 'def', 'jhi');
    test('');
  });

  it('testCamelCase', () => {
    test('Abc', 'Abc');
    test('abcDef', 'abc', 'Def');
    test('AbcDef', 'Abc', 'Def');
    test('abc_Def', 'abc', 'Def');
    test('abc_defJhi', 'abc', 'def', 'Jhi');
  });

  it('testDigits', () => {
    test('Abc00', 'Abc00');
    test('Abc00_01', 'Abc00', '01');
    test('Abc00_01def', 'Abc00', '01', 'def');
  });
});
