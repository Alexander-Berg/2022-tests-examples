/* eslint-disable no-undef */
import sortTextWithNumber from './sortTextWithNumber';

function test(a, b, c) {
  expect(sortTextWithNumber(a, b)).toBeLessThan(0);
  expect(sortTextWithNumber(b, a)).toBeGreaterThan(0);

  expect(sortTextWithNumber(b, c)).toBeLessThan(0);
  expect(sortTextWithNumber(c, b)).toBeGreaterThan(0);

  expect(sortTextWithNumber(a, c)).toBeLessThan(0);
  expect(sortTextWithNumber(c, a)).toBeGreaterThan(0);
}

describe('sortTextWithNumber', () => {
  it('alertIdCompare', () => {
    const a = '9e2a85e06fd214c18ffb4ea146389b9fb2061fa3';
    const b = '15f15a419f873490ce5c5e6ce651e10920a0acff';
    const c = '4265427724080393024270779296d40304f6605e';

    test(a, b, c);
  });

  it('binMs', () => {
    test('1ms', '2ms', '10ms');
    test('10 ms', '50 ms', '100 ms');
  });

  it('text', () => {
    test('a', 'b', 'c');
    test('aa', 'bb', 'cc');
  });

  it('number', () => {
    test('10', '50', '100');
  });

  it('partNum', () => {
    test('>10ms', '>50ms', '>100ms');
  });

  it('empty text', () => {
    test('', '10', 'a');
  });
});
