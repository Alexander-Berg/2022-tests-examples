/* eslint-disable no-undef */
import Weekends from './Weekends';

it('testWeekends', () => {
  const weekends = Weekends.weekendsIntersectingInterval({
    begin: new Date(2013, 11, 26, 22, 30),
    end: new Date(2013, 11, 30, 22, 30),
  });

  const expected = [{
    begin: new Date(2013, 11, 28),
    end: new Date(2013, 11, 30),
  }];

  expect(weekends).toEqual(expected);
});
