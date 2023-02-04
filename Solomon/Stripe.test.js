/* eslint-disable no-undef */
import Stripe from './Stripe';
import Interpolate from './Interpolate';

describe('StripeTest', () => {
  it('normalize', () => {
    const s = new Stripe([1000, 2000, 3000], [1, 3, 6], [1, 1.5, 3]);
    const h = { timestamps: [1000, 3000], values: [2, 10] };
    const r = s.normalize(h, Interpolate.LINEAR);
    const expected = new Stripe([1000, 2000, 3000], [50, 50, 60], [50, 25, 30]);
    expect(r).toEqual(expected);
  });
});
