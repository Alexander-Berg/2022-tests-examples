/* eslint-disable no-undef */
import StackBuilder from './StackBuilder';
import Baseline from './Baseline';
import Interpolate from './Interpolate';
import StripeBuilder from './StripeBuilder';
import GraphDataToBaseline from './GraphDataToBaseline';
import GraphDataWithStackConfigAndCookie from './GraphDataWithStackConfigAndCookie';
import Stripe from './Stripe';

describe('StackBuilderTest', () => {
  it('combineLinear', () => {
    const t = StackBuilder.combine(
      Baseline.fromMiddles([1000, 2000, 3000], [1, 1, 3]),
      { timestamps: [1500, 2500], values: [3, 5] },
      Interpolate.LINEAR,
    );
    const baseline = t[0];
    const stripe = t[1].toStripe();

    const expected = Baseline.forTest(
      [1000, 1500, 2000, 2500, 3000],
      [
        [0, 1, 1],
        [1, 4, 4],
        [5, 5, 5],
        [7, 7, 2],
        [3, 3, 0],
      ],
    );

    expect(baseline).toEqual(expected);

    const stripeExpected = new StripeBuilder();
    stripeExpected.addRaw(1500, 4, 1);
    stripeExpected.addRaw(2000, 5, 1);
    stripeExpected.addRaw(2500, 7, 2);
    expect(stripe).toEqual(stripeExpected.build());
  });

  it('combineLeft', () => {
    const firstBaseline = GraphDataToBaseline.graphDataToBaseline(
      { timestamps: [1000, 2000, 3000], values: [1, 1, 3] },
      Interpolate.LEFT,
    );

    const firstBaselineExpected = Baseline.forTest(
      [1000, 2000, 3000],
      [
        [0, 1, 1],
        [1, 1, 1],
        [1, 3, 0],
      ],
    );

    expect(firstBaseline).toEqual(firstBaselineExpected);

    const t = StackBuilder.combine(
      firstBaseline,
      { timestamps: [1500, 2500], values: [3, 5] }, Interpolate.LEFT,
    );
    const baseline = t[0];
    const stripe = t[1].toStripe();

    expect(baseline).toEqual(
      Baseline.forTest(
        [1000, 1500, 2000, 2500, 3000],
        [
          [0, 1, 1],
          [1, 4, 4],
          [4, 4, 4],
          [4, 6, 1],
          [1, 3, 0],
        ],
      ),
    );

    const stripeExpected = new StripeBuilder();
    stripeExpected.addRaw(1500, 4, 1);
    stripeExpected.addRaw(2000, 4, 1);
    stripeExpected.addRaw(2500, 4, 1);
    stripeExpected.addRaw(2500, 6, 1);
    expect(stripe).toEqual(stripeExpected.build());
  });

  // bug: http://jing.yandex-team.ru/files/nga/2015-07-04_0043.png
  // https://st.yandex-team.ru/SOLOMON-635
  it('normalizeWithGap', () => {
    const inputs = [];
    inputs.push(new GraphDataWithStackConfigAndCookie(
      { timestamps: [2000, 3000], values: [1, 1] },
      's1',
      false,
      true,
      'left',
    ));
    inputs.push(new GraphDataWithStackConfigAndCookie(
      { timestamps: [1000, 2000, 3000], values: [1, 1, 1] },
      's1',
      false,
      true,
      'left',
    ));
    const stripes = StackBuilder.makeStacks(inputs, Interpolate.LINEAR, true);

    const firstStripe = stripes[0][0];
    const expectedFirstStripe = new Stripe(
      [2000, 3000],
      [50, 50],
      [0, 0],
    );
    expect(firstStripe).toEqual(expectedFirstStripe);

    const secondStripe = stripes[1][0];
    const expectedSecondStripe = new Stripe(
      [1000, 2000, 2000, 3000],
      [100, 100, 100, 100],
      [0, 0, 50, 50],
    );
    expect(secondStripe).toEqual(expectedSecondStripe);
  });
});
