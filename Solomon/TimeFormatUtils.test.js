/* eslint-disable no-undef */
import TimeFormatUtils from './TimeFormatUtils';

describe('TimeFormatUtils', () => {
  it('testFormatUtcMillis', () => {
    const millis = Date.UTC(2017, 0, 21, 12, 29, 44, 782);
    expect(TimeFormatUtils.formatUtcMillis(millis))
      .toEqual('2017-01-21T12:29:44.782Z');
  });

  it('testParseUtc', () => {
    expect(TimeFormatUtils.parseUtc('2017-01-21T12:29:44.782Z'))
      .toEqual(Date.UTC(2017, 0, 21, 12, 29, 44, 0));
    expect(TimeFormatUtils.parseUtc('2017-01-21T12:29:44Z'))
      .toEqual(Date.UTC(2017, 0, 21, 12, 29, 44, 0));
  });

  it('testDateToDateHhMmBrowserTz', () => {
    const jsDate = new Date(2017, 0, 21, 15, 45, 10, 10);
    expect(TimeFormatUtils.dateToDateHhMmBrowserTz(jsDate))
      .toEqual('2017-01-21 15:45:10');
  });

  it('testParseShortDateTimeInLocalTz', () => {
    expect(TimeFormatUtils.parseShortDateTimeInLocalTz('2017-01-21 12:29'))
      .toEqual(new Date(2017, 0, 21, 12, 29));
    expect(TimeFormatUtils.parseShortDateTimeInLocalTz('2017-01-21 12:29:30'))
      .toEqual(new Date(2017, 0, 21, 12, 29, 30));
  });
});
