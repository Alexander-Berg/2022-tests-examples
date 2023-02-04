/* eslint-disable no-undef */
import Xaxis from './Xaxis';
import StepUnit from './StepUnit';
import * as TimeUnit from './TimeUnit';

it('makeStepMarks', () => {
  const interval = {
    begin: new Date(2013, 11, 26, 23, 3),
    end: new Date(2013, 11, 26, 23, 57),
  };

  const stepMarks = Xaxis.makeTicks(interval, new StepUnit(TimeUnit.MINUTE_MILLIS, 15));
  const expected = [
    [new Date(2013, 11, 26, 23, 15).getTime() / 1000, '23:15'],
    [new Date(2013, 11, 26, 23, 30).getTime() / 1000, '23:30'],
    [new Date(2013, 11, 26, 23, 45).getTime() / 1000, '23:45'],
  ];

  expect(stepMarks).toEqual(expected);
});
