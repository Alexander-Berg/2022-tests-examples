/* eslint-disable no-undef */
import Interpolate from './Interpolate';
import Resampler from './Resampler';

describe('ResamplerTest', () => {
  function testOp(origTimeline, origValues, newTimeline, newValues, tr) {
    const orig = { timestamps: origTimeline, values: origValues };
    const actual = tr(orig);
    const expected = { timestamps: newTimeline, values: newValues };
    expect(actual).toEqual(expected);
  }

  function testResample(origTimeline, origValues, newTimeline, newValues) {
    testOp(origTimeline, origValues, newTimeline, newValues,
      (orig) => Resampler.resample(orig, newTimeline, Interpolate.LINEAR));
  }

  it('resample', () => {
    {
      const graphData = {
        timestamps: [100, 200, 300, 400],
        values: [1, 3, 2, 4],
      };
      const actual = Resampler.resample(graphData, graphData.timestamps, Interpolate.LINEAR);
      expect(actual).toEqual(graphData);
    }

    // simple average
    testResample(
      [100, 200], [1, 2],
      [150], [1.5],
    );
    // skip point
    testResample(
      [100, 200, 300], [1, 3, 2],
      [100, 300], [1, 2],
    );
    // outer points
    testResample(
      [100, 200], [1, 3],
      [0, 50, 250, 300], [NaN, 1, 3, NaN],
    );
    // linear
    testResample(
      [100, 400], [1, 4],
      [100, 200, 300, 400], [1, 2, 3, 4],
    );

    testResample(
      [], [],
      [1000, 2000, 3000], [NaN, NaN, NaN],
    );
  });
});
