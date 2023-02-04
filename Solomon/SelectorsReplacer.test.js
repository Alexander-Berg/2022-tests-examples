/* eslint-disable no-undef */
import {
  divideCodeBySelectors,
  LOAD1_PART_TYPE,
  LOAD_PART_TYPE,
  SELECTORS_PART_TYPE,
  TEXT_PART_TYPE,
} from './SelectorsReplacer';

it('empty', () => {
  const actual = divideCodeBySelectors('');
  expect(actual).toEqual([]);
});

it('blank', () => {
  const code = '  ';
  const expected = [{
    type: TEXT_PART_TYPE,
    text: '  ',
  }];
  const actual = divideCodeBySelectors(code);
  expect(actual).toEqual(expected);
});

it(TEXT_PART_TYPE, () => {
  const code = '2 + 2';
  const expected = [
    {
      type: TEXT_PART_TYPE,
      text: '2 + 2',
    },
  ];
  const actual = divideCodeBySelectors(code);
  expect(actual).toEqual(expected);
});

it('single selectors', () => {
  const code = '{a=b, c=d, e=\'f\', "g"="h"}';
  const expected = [
    {
      type: SELECTORS_PART_TYPE,
      text: '{a=b, c=d, e=\'f\', "g"="h"}',
    },
  ];
  const actual = divideCodeBySelectors(code);
  expect(actual).toEqual(expected);
});

it('selectors at first', () => {
  const code = '{sensor="value"} + 1';
  const expected = [
    {
      type: SELECTORS_PART_TYPE,
      text: '{sensor="value"}',
    },
    {
      type: TEXT_PART_TYPE,
      text: ' + 1',
    },
  ];
  const actual = divideCodeBySelectors(code);
  expect(actual).toEqual(expected);
});

it('selectors at last', () => {
  const code = '1 + {sensor="value"}';
  const expected = [
    {
      type: TEXT_PART_TYPE,
      text: '1 + ',
    },
    {
      type: SELECTORS_PART_TYPE,
      text: '{sensor="value"}',
    },
  ];
  const actual = divideCodeBySelectors(code);
  expect(actual).toEqual(expected);
});

it('selectors at middle', () => {
  const code = '1 + {sensor="value"};';
  const expected = [
    {
      type: TEXT_PART_TYPE,
      text: '1 + ',
    },
    {
      type: SELECTORS_PART_TYPE,
      text: '{sensor="value"}',
    },
    {
      type: TEXT_PART_TYPE,
      text: ';',
    },
  ];
  const actual = divideCodeBySelectors(code);
  expect(actual).toEqual(expected);
});

it('selectors with name', () => {
  const code = 'part.subpart.other_subpart {sensor="value"}';
  const expected = [
    {
      type: SELECTORS_PART_TYPE,
      text: 'part.subpart.other_subpart {sensor="value"}',
    },
  ];
  const actual = divideCodeBySelectors(code);
  expect(actual).toEqual(expected);
});

it('selectors with return', () => {
  const code = 'return {sensor="value"};';
  const expected = [
    {
      text: 'return ',
      type: TEXT_PART_TYPE,
    },
    {
      type: SELECTORS_PART_TYPE,
      text: '{sensor="value"}',
    },
    {
      text: ';',
      type: TEXT_PART_TYPE,
    },
  ];
  const actual = divideCodeBySelectors(code);
  expect(actual).toEqual(expected);
});

it('old load() selectors', () => {
  const code = 'load("a=b&c=d")';
  const expected = [
    {
      type: LOAD_PART_TYPE,
      text: 'load("a=b&c=d")',
      selectors: 'a=b&c=d',
    },
  ];
  const actual = divideCodeBySelectors(code);
  expect(actual).toEqual(expected);
});

it('old load1() selectors', () => {
  const code = 'load1("a=b&c=d")';
  const expected = [
    {
      type: LOAD1_PART_TYPE,
      text: 'load1("a=b&c=d")',
      selectors: 'a=b&c=d',
    },
  ];
  const actual = divideCodeBySelectors(code);
  expect(actual).toEqual(expected);
});

it('old and new selectors together', () => {
  const code = 'load1("a=b&c=d") + {a=b, c=d}';
  const expected = [
    {
      type: LOAD1_PART_TYPE,
      text: 'load1("a=b&c=d")',
      selectors: 'a=b&c=d',
    },
    {
      type: TEXT_PART_TYPE,
      text: ' + ',
    },
    {
      type: SELECTORS_PART_TYPE,
      text: '{a=b, c=d}',
    },
  ];
  const actual = divideCodeBySelectors(code);
  expect(actual).toEqual(expected);
});

it('complex', () => {
  const code = 'alias({sensor=\'jvm.classes.loaded|jvm.classes.unloaded\',host!=\'cluster\',host!=\'per_dc\'},\'{{host}}.{{sensor}}\')';
  const expected = [
    {
      type: TEXT_PART_TYPE,
      text: 'alias(',
    },
    {
      type: SELECTORS_PART_TYPE,
      text: '{sensor=\'jvm.classes.loaded|jvm.classes.unloaded\',host!=\'cluster\',host!=\'per_dc\'}',
    },
    {
      type: TEXT_PART_TYPE,
      text: ',\'{{host}}.{{sensor}}\')',
    },
  ];
  const actual = divideCodeBySelectors(code);
  expect(actual).toEqual(expected);
});
