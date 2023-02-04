/* eslint-disable no-undef */
import StepUnit from './StepUnit';
import * as TimeUnit from './TimeUnit';

describe('StepUnit', () => {
  it('forHourDuration', () => {
    const expected = StepUnit.forDuration(55 * TimeUnit.MINUTE_MILLIS);
    expect(new StepUnit(TimeUnit.HOUR_MILLIS, 1)).toEqual(expected);
  });

  it('forMinuteDuration', () => {
    const expected = StepUnit.forDuration(100);
    expect(new StepUnit(TimeUnit.MINUTE_MILLIS, 1)).toEqual(expected);
  });

  it('forYearDuration', () => {
    const expected = StepUnit.forDuration(TimeUnit.DAY_MILLIS * 10000);
    expect(new StepUnit(TimeUnit.YEAR_MILLIS, 1)).toEqual(expected);
  });
});
