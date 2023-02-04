/* eslint-disable no-undef */
import StringInterpolator from './StringInterpolator';
import { escapeQuotes } from './Quoter';

describe('StringInterpolator', () => {
  it('empty', () => {
    expect(StringInterpolator.interpolatePattern('', {})).toEqual('');
  });

  it('single literal', () => {
    expect(StringInterpolator.interpolatePattern('aaaa', {})).toEqual('aaaa');
  });

  it('single variable', () => {
    expect(StringInterpolator.interpolatePattern('{{aa}}', { aa: 'AA' })).toEqual('AA');
  });

  it('single variable with whitespaces', () => {
    expect(StringInterpolator.interpolatePattern('{{ aa  }}', { aa: 'AA' })).toEqual('AA');
  });

  it('last variable', () => {
    expect(StringInterpolator.interpolatePattern('aa{{bb}}', { bb: 'BB' })).toEqual('aaBB');
  });

  it('first variable', () => {
    expect(StringInterpolator.interpolatePattern('{{aa}}bb', { aa: 'AA' })).toEqual('AAbb');
  });

  it('several variables', () => {
    expect(StringInterpolator.interpolatePattern('{{aa}}bb{{cc}}dd', { aa: 'AA', cc: 'CC' })).toEqual('AAbbCCdd');
  });

  it('partial variables', () => {
    expect(StringInterpolator.interpolatePattern('aaBBcc{{dd}}', {})).toEqual('aaBBcc{{dd}}');
  });

  it('incorrect variable', () => {
    expect(() => StringInterpolator.interpolatePattern('{{aa', {})).toThrow();
  });

  it('incorrect variable (2)', () => {
    expect(StringInterpolator.interpolatePattern('{{aa {{bb}}', { bb: 'BB' })).toEqual('{{aa {{bb}}');
  });

  it('interpolate with transform', () => {
    expect(StringInterpolator.interpolatePattern('"{{bb}}"', { bb: '"value\'' }, false, (value) => escapeQuotes(value))).toEqual('"\\"value\\\'"');
  });
});
