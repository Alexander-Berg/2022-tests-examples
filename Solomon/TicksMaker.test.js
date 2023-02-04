/* eslint-disable no-undef */
import StepUnit from './StepUnit';
import * as TimeUnit from './TimeUnit';
import TicksMaker from './TicksMaker';

describe('TicksMakerTest', () => {
  it('roundDownByHour', () => {
    const h2 = new StepUnit(TimeUnit.HOUR_MILLIS, 2);
    const actual = TicksMaker.roundDown(new Date(2013, 12, 25, 21, 52, 47), h2);
    const expected = new Date(2013, 12, 25, 20, 0);
    expect(actual).toEqual(expected);
  });

  it('roundDownByWeek', () => {
    const w2 = new StepUnit(TimeUnit.WEEK_MILLIS, 2);
    const actual = TicksMaker.roundDown(new Date(2013, 11, 25, 21, 52, 47), w2);
    const expected = new Date(2013, 11, 16, 0, 0);
    expect(actual).toEqual(expected);
  });

  it('makeStepMarks', () => {
    const interval = {
      begin: new Date(2013, 11, 26, 23, 3),
      end: new Date(2013, 11, 26, 23, 57),
    };

    const ticks = TicksMaker.tickMarks(interval, new StepUnit(TimeUnit.MINUTE_MILLIS, 15));
    const expected = [
      new Date(2013, 11, 26, 23, 15),
      new Date(2013, 11, 26, 23, 30),
      new Date(2013, 11, 26, 23, 45),
    ];

    expect(ticks).toEqual(expected);
  });

  it('makeStepMarksForCalendarSteps', () => {
    const interval = {
      begin: new Date(2018, 0),
      end: new Date(2019, 0),
    };

    const ticks = TicksMaker.tickMarks(interval, new StepUnit(TimeUnit.MONTH_MILLIS, 2));
    const expected = [
      new Date(2018, 0),
      new Date(2018, 2),
      new Date(2018, 4),
      new Date(2018, 6),
      new Date(2018, 8),
      new Date(2018, 10),
      new Date(2019, 0),
    ];

    expect(ticks).toEqual(expected);
  });
});
