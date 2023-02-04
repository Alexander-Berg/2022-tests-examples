/* eslint-disable no-undef */
import { parseLocalDate, formatLocalDate } from './localDate';

describe('localDate', () => {
  it('formatLocalDate', () => {
    const date = new Date(2018, 7, 15, 12, 4, 13, 122);
    const text = '2018-08-15 12:04:13.122';
    expect(formatLocalDate(date)).toEqual(text);
  });
  it('parseLocalDate', () => {
    const text = '2018-08-15 12:04:13.122';
    const date = new Date(2018, 7, 15, 12, 4, 13, 122);
    expect(parseLocalDate(text)).toEqual(date);
  });
});
