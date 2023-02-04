/* eslint-disable no-undef */
import {
  consumeQuotedString, doubleQuote, escapeQuotes, singleQuote,
} from './Quoter';
import Parser from './Parser';

function testConsumerWithForbiddenSymbols(source, forbiddenSymbols, expected) {
  const parser = Parser.create(source);
  const actual = consumeQuotedString(parser, forbiddenSymbols);
  expect(actual).toEqual(expected);
}

function testConsumer(source, expected) {
  testConsumerWithForbiddenSymbols(source, '', expected);
}

it('consumeEmpty', () => {
  testConsumer('', '');
});

it('consumeBlank', () => {
  testConsumer('  ', '');
});

it('consumeText', () => {
  testConsumer('value', 'value');
});

it('consumeTextWithWhitespace', () => {
  testConsumer('value1 value2', 'value1');
});

it('consumeSingleQuotedText', () => {
  testConsumer("'value1' value2", 'value1');
});

it('consumeDoubleQuotedText', () => {
  testConsumer('"value1" value2', 'value1');
});

it('consumeEscapedAndQuotedText', () => {
  testConsumer('"\\"value1\\"" value2', '"value1"');
});

it('consumeTextWithStopSymbols', () => {
  testConsumerWithForbiddenSymbols('value1, value2', ',', 'value1');
});

it('quoteEmpty', () => {
  expect(singleQuote('')).toEqual("''");
});

it('singleQuote', () => {
  expect(singleQuote('value')).toEqual("'value'");
});

it('doubleQuote', () => {
  expect(doubleQuote('value')).toEqual('"value"');
});

it('quoteSingleQuotedText', () => {
  expect(singleQuote("'value'")).toEqual("'\\'value\\''");
});

it('quoteDoubleQuotedText', () => {
  expect(doubleQuote('"value"')).toEqual('"\\"value\\""');
});

it('escapeQuotes', () => {
  expect(escapeQuotes('"\'')).toEqual('\\"\\\'');
});
