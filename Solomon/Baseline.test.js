/* eslint-disable no-undef */
import Baseline from './Baseline';
import GraphDataToBaseline from './GraphDataToBaseline';
import Interpolate from './Interpolate';

describe('BaselineTest', () => {
  it('resamplePointBetween', () => {
    const orig = Baseline.forTest(
      [1000, 2000],
      [
        [0, 10, 10],
        [20, 20, 0],
      ],
    );

    const expected = Baseline.forTest(
      [1500],
      [[0, 15, 0]],
    );

    const actual = orig.resample(expected.timestamps, Interpolate.LINEAR);
    expect(actual).toEqual(expected);
  });

  it('resampleLeftRight', () => {
    const orig = Baseline.forTest(
      [1000, 2000],
      [
        [0, 1000, 10],
        [20, 3000, 0],
      ],
    );

    const expected = Baseline.forTest(
      [1500],
      [[0, 15, 0]],
    );

    const actual = orig.resample(expected.timestamps, Interpolate.LINEAR);
    expect(actual).toEqual(expected);
  });

  it('resamplePointsOutside', () => {
    const orig = Baseline.forTest(
      [1000, 2000],
      [
        [0, 1000, 10],
        [20, 3000, 0],
      ],
    );

    // not sure it is correct
    const expected = Baseline.forTest(
      [500, 1500, 2500],
      [
        [0, 0, 0],
        [15, 15, 15],
        [0, 0, 0],
      ],
    );

    const actual = orig.resample(expected.timestamps, Interpolate.LINEAR);
    expect(actual).toEqual(expected);
  });

  it('resampleLeftShouldZeroOuterPoints', () => {
    const graphData = {
      timestamps: [2000, 3000],
      values: [2, 3],
    };

    const orig = GraphDataToBaseline.graphDataToBaseline(graphData, Interpolate.LEFT);
    const expected1 = Baseline.forTest([2000, 3000],
      [
        [0, 2, 2],
        [2, 3, 0],
      ]);

    expect(orig).toEqual(expected1);

    const resample = expected1.resample([2000, 3000, 4000], Interpolate.LEFT);

    const expected = Baseline.forTest([2000, 3000, 4000],
      [
        [0, 2, 2],
        [2, 3, 0],
        [0, 0, 0],
      ]);

    expect(resample).toEqual(expected);
  });
});
