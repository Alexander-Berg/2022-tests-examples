/* eslint-disable no-undef */
import Selector from './Selector';
import Selectors from './Selectors';

it('parseEmpty', () => {
  const actual = Selectors.parse('');
  const expected = Selectors.of('');
  expect(actual).toEqual(expected);
});

it('parseAny', () => {
  const actual = Selectors.parse('host=*');
  const expected = Selectors.of('', Selector.any('host'));
  expect(actual).toEqual(expected);
});

it('parseAnyNegative', () => {
  const actual = Selectors.parse('host!=*');
  const expected = Selectors.of('', Selector.absent('host'));
  expect(actual).toEqual(expected);
});

it('parseAbsent', () => {
  const actual = Selectors.parse('host!=-');
  const expected = Selectors.of('', Selector.any('host'));
  expect(actual).toEqual(expected);
});

it('parseSingleGlob', () => {
  const actual = Selectors.parse('host=solomon-*');
  const expected = Selectors.of('', Selector.glob('host', 'solomon-*'));
  expect(actual).toEqual(expected);
});

it('parseNotSingleGlob', () => {
  const actual = Selectors.parse('host!=solomon-*');
  const expected = Selectors.of('', Selector.notGlob('host', 'solomon-*'));
  expect(actual).toEqual(expected);
});

it('parseMultiGlob', () => {
  const actual = Selectors.parse('host=solomon-*|cluster');
  const expected = Selectors.of('', Selector.glob('host', 'solomon-*|cluster'));
  expect(actual).toEqual(expected);
});

it('parseNotMultiGlob', () => {
  const actual = Selectors.parse('host!=solomon-*|cluster');
  const expected = Selectors.of('', Selector.notGlob('host', 'solomon-*|cluster'));
  expect(actual).toEqual(expected);
});

it('parseExact', () => {
  const actual = Selectors.parse('host==cluster');
  const expected = Selectors.of('', Selector.exact('host', 'cluster'));
  expect(actual).toEqual(expected);
});

it('parseExactNegative', () => {
  const actual = Selectors.parse('host!==cluster');
  const expected = Selectors.of('', Selector.notExact('host', 'cluster'));
  expect(actual).toEqual(expected);
});

it('parseRegex', () => {
  const actual = Selectors.parse('host=~cluster');
  const expected = Selectors.of('', Selector.regex('host', 'cluster'));
  expect(actual).toEqual(expected);
});

it('parseRegexNegative', () => {
  const actual = Selectors.parse('host!~cluster');
  const expected = Selectors.of('', Selector.notRegex('host', 'cluster'));
  expect(actual).toEqual(expected);
});

it('parseWithWhitespaces', () => {
  const actual = Selectors.parse(' host = cluster  ');
  const expected = Selectors.of('', Selector.glob('host', 'cluster'));
  expect(actual).toEqual(expected);
});

it('parseWithDoubleQuotes', () => {
  const actual = Selectors.parse('host="cluster"');
  const expected = Selectors.of('', Selector.glob('host', 'cluster'));
  expect(actual).toEqual(expected);
});

it('parseWithSingleQuotes', () => {
  const actual = Selectors.parse("host='total Man'");
  const expected = Selectors.of('', Selector.glob('host', 'total Man'));
  expect(actual).toEqual(expected);
});

it('parseSeveralSelectors', () => {
  const actual = Selectors.parse('cluster=proxy, service=frontend, host="summary of hosts, except Man"');

  const expected = Selectors.of('', Selector.glob('cluster', 'proxy'), Selector.glob('service', 'frontend'), Selector.glob('host', 'summary of hosts, except Man'));

  expect(actual).toEqual(expected);
});

it('parseEqualSelectors', () => {
  const actual = Selectors.parse('cluster=proxy, service!~fetcher, service==frontend');
  const expected = Selectors.of('', Selector.glob('cluster', 'proxy'), Selector.notRegex('service', 'fetcher'), Selector.exact('service', 'frontend'));
  expect(actual).toEqual(expected);
});

it('parseSelectorsWithQuotes', () => {
  const actual = Selectors.parse('cluster="proxy", service==frontend, path=="\\\\totalCount"');

  const expected = Selectors.of('', Selector.glob('cluster', 'proxy'), Selector.exact('service', 'frontend'), Selector.exact('path', '\\totalCount'));
  expect(actual).toEqual(expected);
});

it('parseSelectorsWithFigureBrackets', () => {
  const actual = Selectors.parse('{host=cluster}');
  const expected = Selectors.of('', Selector.glob('host', 'cluster'));
  expect(actual).toEqual(expected);
});

it('parseSelectorsWithSensorName', () => {
  const actual = Selectors.parse('some.sensor.name{}');

  const expected = Selectors.of('some.sensor.name');

  expect(actual).toEqual(expected);
});

it('parseSelectorsWithSensorNameAndLabelSelectors', () => {
  const actual = Selectors.parse("some.sensor.name{cluster='production', service='gateway'}");

  const expected = Selectors.of(
    'some.sensor.name',
    Selector.glob('cluster', 'production'),
    Selector.glob('service', 'gateway'),
  );

  expect(actual).toEqual(expected);
});

it('parseSelectorsWithQuotedSensorName', () => {
  const actual = Selectors.parse("\"some \\\"sensor\\\" name\"{cluster='production', service='gateway'}");

  const expected = Selectors.of(
    'some "sensor" name',
    Selector.glob('cluster', 'production'),
    Selector.glob('service', 'gateway'),
  );

  expect(actual).toEqual(expected);
});

it('parseWithWhitespaces', () => {
  const actual = Selectors.parse(" \"some \\\"sensor\\\" name\" { cluster = 'production' , service = 'gateway' } ");

  const expected = Selectors.of(
    'some "sensor" name',
    Selector.glob('cluster', 'production'),
    Selector.glob('service', 'gateway'),
  );

  expect(actual).toEqual(expected);
});
