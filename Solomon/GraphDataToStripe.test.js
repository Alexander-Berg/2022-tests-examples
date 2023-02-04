/* eslint-disable no-undef */
import GraphDataToStripe from './GraphDataToStripe';
import Interpolate from './Interpolate';
import Stripe from './Stripe';

describe('GraphDataToStripeTest', () => {
  it('linear', () => {
    const orig = {
      timestamps: [1000, 2000, 3000, 4000],
      values: [1, 2, NaN, 3],
    };
    const stripeIntermediate = GraphDataToStripe.graphDataToStripe(orig, Interpolate.LINEAR);
    const stripe = stripeIntermediate.toStripe();
    const expected = new Stripe(
      [1000, 2000, 2000, 4000],
      [1, 2, NaN, 3],
      [0, 0, NaN, 0],
    );
    expect(stripe).toEqual(expected);
  });

  it('left', () => {
    const orig = {
      timestamps: [1000, 2000, 3000, 4000, 5000],
      values: [1, 2, NaN, 4, 3],
    };
    const stripeIntermediate = GraphDataToStripe.graphDataToStripe(orig, Interpolate.LEFT);
    const stripe = stripeIntermediate.toStripe();
    const expected = Stripe.fromPoints([
      { ts: 1000, high: 1, low: 0 },
      { ts: 2000, high: 1, low: 0 },
      { ts: 2000, high: 2, low: 0 },
      { ts: 2000, high: NaN, low: NaN },
      { ts: 4000, high: 4, low: 0 },
      { ts: 5000, high: 4, low: 0 },
      { ts: 5000, high: 3, low: 0 },
    ]);
    expect(stripe).toEqual(expected);
  });

  it('right', () => {
    const orig = {
      timestamps: [1000, 2000, 3000, 4000, 5000],
      values: [1, 2, NaN, 4, 3],
    };
    const stripeIntermediate = GraphDataToStripe.graphDataToStripe(orig, Interpolate.RIGHT);
    const stripe = stripeIntermediate.toStripe();
    const expected = Stripe.fromPoints([
      { ts: 1000, high: 1, low: 0 },
      { ts: 1000, high: 2, low: 0 },
      { ts: 2000, high: 2, low: 0 },
      { ts: 2000, high: NaN, low: NaN },
      { ts: 4000, high: 4, low: 0 },
      { ts: 4000, high: 3, low: 0 },
      { ts: 5000, high: 3, low: 0 },
    ]);
    expect(stripe).toEqual(expected);
  });
});
