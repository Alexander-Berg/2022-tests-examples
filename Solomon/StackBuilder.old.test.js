/* eslint-disable no-undef */
import GraphDataWithStackConfigAndCookie from './GraphDataWithStackConfigAndCookie';
import Stripe from './Stripe';
import StackBuilder from './StackBuilder';
import Interpolate from './Interpolate';

describe('StackBuilderOldTest', () => {
  function createFlotPointsWithStackConfigAndCookie(graphData, stack, cookie) {
    return new GraphDataWithStackConfigAndCookie(graphData, stack, false, cookie, 'left');
  }

  it('testMakeStack', () => {
    const a = {
      timestamps: [0, 1, 2, 3],
      values: [1, 2, 3, 4],
    };
    const b = {
      timestamps: [0, 1, 2, 3],
      values: [2, 3, 4, 5],
    };

    const firstLine = createFlotPointsWithStackConfigAndCookie(a, 'a', 1);
    const secondLine = createFlotPointsWithStackConfigAndCookie(b, 'a', 1);
    const actual = StackBuilder.makeStacks([firstLine, secondLine], Interpolate.LINEAR, false)
      .map((pair) => pair[0]);
    const expected = [
      Stripe.fromPoints([
        { ts: 0, high: 1, low: 0 },
        { ts: 1, high: 2, low: 0 },
        { ts: 2, high: 3, low: 0 },
        { ts: 3, high: 4, low: 0 },
      ]),
      Stripe.fromPoints([
        { ts: 0, high: 3, low: 1 },
        { ts: 1, high: 5, low: 2 },
        { ts: 2, high: 7, low: 3 },
        { ts: 3, high: 9, low: 4 },
      ]),
    ];
    expect(actual).toEqual(expected);
  });

  it('testMakeStackZeroBorder', () => {
    const a = { timestamps: [0, 1, 2, 3], values: [0, 2, 3, 0] };
    const b = { timestamps: [0, 1, 2, 3], values: [2, 3, 4, 5] };
    const firstLine = createFlotPointsWithStackConfigAndCookie(a, 'a', 1);
    const secondLine = createFlotPointsWithStackConfigAndCookie(b, 'a', 1);
    const actual = StackBuilder.makeStacks([firstLine, secondLine], Interpolate.LINEAR, false)
      .map((pair) => pair[0]);
    const expected = [
      Stripe.fromPoints([
        { ts: 0, high: 0, low: 0 },
        { ts: 1, high: 2, low: 0 },
        { ts: 2, high: 3, low: 0 },
        { ts: 3, high: 0, low: 0 },
      ]),
      Stripe.fromPoints([
        { ts: 0, high: 2, low: 0 },
        { ts: 1, high: 5, low: 2 },
        { ts: 2, high: 7, low: 3 },
        { ts: 3, high: 5, low: 0 },
      ]),
    ];
    expect(actual).toEqual(expected);
  });

  it('testMakeStackNaN', () => {
    const a = { timestamps: [0, 1, 2, 3], values: [1, NaN, 3, 4] };
    const b = { timestamps: [0, 1, 2, 3], values: [2, 3, 4, 5] };
    const firstLine = createFlotPointsWithStackConfigAndCookie(a, 'a', 1);
    const secondLine = createFlotPointsWithStackConfigAndCookie(b, 'a', 1);
    const actual = StackBuilder.makeStacks([firstLine, secondLine], Interpolate.LINEAR, false)
      .map((pair) => pair[0]);
    const expected = [
      Stripe.fromPoints([
        { ts: 0, high: 1, low: 0 },
        { ts: 0, high: NaN, low: NaN },
        { ts: 2, high: 3, low: 0 },
        { ts: 3, high: 4, low: 0 },
      ]),
      Stripe.fromPoints([
        { ts: 0, high: 3, low: 1 },
        { ts: 0, high: 2, low: 0 },
        { ts: 1, high: 3, low: 0 },
        { ts: 2, high: 4, low: 0 },
        { ts: 2, high: 7, low: 3 },
        { ts: 3, high: 9, low: 4 },
      ]),
    ];
    expect(actual).toEqual(expected);
  });

  it('testMakeStackDifferentTimeLines', () => {
    const a = { timestamps: [0, 2, 5], values: [1, 3, 6] };
    const b = { timestamps: [1, 3, 4, 7], values: [3, 5, 6, 9] };
    const firstLine = createFlotPointsWithStackConfigAndCookie(a, 'a', 1);
    const secondLine = createFlotPointsWithStackConfigAndCookie(b, 'a', 1);
    const actual = StackBuilder.makeStacks([firstLine, secondLine], Interpolate.LINEAR, false)
      .map((pair) => pair[0]);
    const expected = [
      Stripe.fromPoints([
        { ts: 0, high: 1, low: 0 },
        { ts: 1, high: 2, low: 0 },
        { ts: 2, high: 3, low: 0 },
        { ts: 3, high: 4, low: 0 },
        { ts: 4, high: 5, low: 0 },
        { ts: 5, high: 6, low: 0 },
      ]),
      Stripe.fromPoints([
        { ts: 1, high: 5, low: 2 },
        { ts: 2, high: 7, low: 3 },
        { ts: 3, high: 9, low: 4 },
        { ts: 4, high: 11, low: 5 },
        { ts: 5, high: 13, low: 6 },
        { ts: 5, high: 7, low: 0 },
        { ts: 7, high: 9, low: 0 },
      ]),
    ];
    expect(actual).toEqual(expected);
  });

  it('testMakeStackNonMatchingTimeLines', () => {
    const a = { timestamps: [0], values: [1] };
    const b = { timestamps: [11, 12, 13, 14, 15], values: [11, 12, 13, 14, 15] };
    const firstLine = createFlotPointsWithStackConfigAndCookie(a, 'a', 1);
    const secondLine = createFlotPointsWithStackConfigAndCookie(b, 'a', 1);
    const actual = StackBuilder.makeStacks([firstLine, secondLine], Interpolate.LINEAR, false)
      .map((pair) => pair[0]);
    const expected = [
      Stripe.fromPoints([{ ts: 0, high: 1, low: 0 }]),
      Stripe.fromPoints([
        { ts: 11, high: 11, low: 0 },
        { ts: 12, high: 12, low: 0 },
        { ts: 13, high: 13, low: 0 },
        { ts: 14, high: 14, low: 0 },
        { ts: 15, high: 15, low: 0 },
      ]),
    ];
    expect(actual).toEqual(expected);
  });
});
