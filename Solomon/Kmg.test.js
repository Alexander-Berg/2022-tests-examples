/* eslint-disable no-undef */
import parseKmg from './Kmg';

it('Kmg', () => {
  expect(parseKmg('10')).toEqual(10.0);
  expect(parseKmg('10.0')).toEqual(10.0);
  expect(parseKmg('10.00')).toEqual(10.0);
  expect(parseKmg('+10.00')).toEqual(10.0);
  expect(parseKmg('-10.00')).toEqual(-10.0);
  expect(parseKmg('10e+1')).toEqual(100.0);
  expect(parseKmg('+10.0e1')).toEqual(100.0);
  expect(parseKmg('10E2')).toEqual(1000.0);
  expect(parseKmg('12k')).toEqual(12000);
  expect(parseKmg('123G')).toEqual(123000000000);
  expect(parseKmg('123 + 12G')).toEqual(NaN);
  expect(parseKmg(' 40M ')).toEqual(40e+6);
  expect(parseKmg(' 40 ')).toEqual(40);
});
