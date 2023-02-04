/* eslint-disable no-undef */
import { formatDuration, parseDuration } from './duration';

describe('duration', () => {
  it('parseDuration', () => {
    expect(parseDuration('')).toEqual(null);
    expect(parseDuration('aabb')).toEqual(null);
    expect(parseDuration('17h')).toEqual(17 * 60 * 60 * 1000);
    expect(parseDuration('1h30m')).toEqual(90 * 60 * 1000);
    expect(parseDuration('2d')).toEqual(2 * 24 * 60 * 60 * 1000);
    expect(parseDuration('3w')).toEqual(21 * 24 * 60 * 60 * 1000);
    expect(parseDuration('3w1d')).toEqual(22 * 24 * 60 * 60 * 1000);
    expect(parseDuration('017s')).toEqual(17 * 1000);
    expect(parseDuration('1m017s45ms')).toEqual(77045);
  });
  it('formatDuration', () => {
    expect(formatDuration(undefined)).toEqual('');
    expect(formatDuration(null)).toEqual('');
    expect(formatDuration(0)).toEqual('0');
    expect(formatDuration(60 * 60 * 1000)).toEqual('1h');
    expect(formatDuration(10 * 60 * 1000)).toEqual('10m');
    expect(formatDuration(20 * 1000)).toEqual('20s');
    expect(formatDuration(90 * 60 * 1000)).toEqual('1h30m');
    expect(formatDuration(26 * 60 * 60 * 1000)).toEqual('1d2h');
  });
});
