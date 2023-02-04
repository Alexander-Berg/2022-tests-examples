/* eslint-disable no-undef */
import { parseISO8601, formatISO8601 } from './iso8601';

describe('iso8601', () => {
  it('parseISO8601', () => {
    expect(parseISO8601('2017-01-21T12:29:44.782Z')).toEqual(Date.UTC(2017, 0, 21, 12, 29, 44, 0));
    expect(parseISO8601('2017-01-21T12:29:44Z')).toEqual(Date.UTC(2017, 0, 21, 12, 29, 44, 0));
    expect(parseISO8601('2017-01-21')).toEqual(NaN);
  });
  it('formatISO8601', () => {
    expect(formatISO8601(Date.UTC(2017, 0, 21, 12, 29, 44, 782))).toEqual('2017-01-21T12:29:44.782Z');
    expect(formatISO8601(Date.UTC(2017, 0, 21, 12, 29, 44, 0))).toEqual('2017-01-21T12:29:44.000Z');
    expect(parseISO8601('2017-01-21')).toEqual(NaN);
  });
});
