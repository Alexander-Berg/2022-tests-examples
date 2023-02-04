/* eslint-disable no-undef */
import SensorsChecksSet from './SensorsChecksSet';

describe('SensorsChecksSetTest', () => {
  function testMakeQueryArgValue(expectedValue, positive, ...sensorIds) {
    const actualValue = new SensorsChecksSet(positive, sensorIds).makeQueryArgValue();
    expect(actualValue).toEqual(expectedValue);
  }

  function testParsing(value, positive, ...sensorIds) {
    const expected = new SensorsChecksSet(positive, sensorIds);
    const actual = SensorsChecksSet.parse(value);
    expect(actual).toEqual(expected);
  }

  it('makeQueryArgValue', () => {
    testMakeQueryArgValue('+', true);
    testMakeQueryArgValue('-', false);

    testMakeQueryArgValue('+aaa', true, 'aaa');
    testMakeQueryArgValue('-aaa', false, 'aaa');

    testMakeQueryArgValue('+aaa;bbb', true, 'aaa', 'bbb');
    testMakeQueryArgValue('-aaa;bbb', false, 'aaa', 'bbb');

    testMakeQueryArgValue('+;\\;;\\;\\;;aa;bb\\;bb;\\;cc;dd\\;', true, '', ';', ';;', 'aa', 'bb;bb', ';cc', 'dd;');
  });

  it('parse', () => {
    testParsing('', false);

    testParsing('+', true);
    testParsing('-', false);

    testParsing('+aaa', true, 'aaa');
    testParsing('-aaa', false, 'aaa');

    testParsing('+aaa;bbb', true, 'aaa', 'bbb');
    testParsing('-aaa;bbb', false, 'aaa', 'bbb');

    testParsing('+;\\;;\\;\\;;aa;bb\\;bb;\\;cc;dd\\;', true, '', ';', ';;', 'aa', 'bb;bb', ';cc', 'dd;');
  });

  it('include', () => {
    {
      const checks = new SensorsChecksSet(false, []);
      expect(checks.include('id')).toBeTruthy();
    }

    {
      const checks = new SensorsChecksSet(true, []);
      expect(checks.include('id')).toBeFalsy();
    }

    {
      const checks = new SensorsChecksSet(false, []);
      expect(checks.include('')).toBeTruthy();
      expect(checks.include('id')).toBeTruthy();
    }

    {
      const checks = new SensorsChecksSet(true, []);
      expect(checks.include('')).toBeFalsy();
      expect(checks.include('id')).toBeFalsy();
    }

    {
      const checks = new SensorsChecksSet(true, ['xxx=yyy']);
      expect(checks.include('')).toBeFalsy();
      expect(checks.include('xxx=yyy')).toBeTruthy();
      expect(checks.include('xxx')).toBeFalsy();
    }

    {
      const checks = new SensorsChecksSet(false, ['xxx=yyy']);
      expect(checks.include('')).toBeTruthy();
      expect(checks.include('xxx')).toBeTruthy();
      expect(checks.include('xxx=yyy')).toBeFalsy();
    }

    {
      const checks = new SensorsChecksSet(true, ['aaa=bbb', 'ccc=ddd']);
      expect(checks.include('aaa=bbb')).toBeTruthy();
      expect(checks.include('ccc=ddd')).toBeTruthy();
      expect(checks.include('aaa=ddd')).toBeFalsy();
    }
  });
});
