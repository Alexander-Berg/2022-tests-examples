/* eslint-disable no-undef */
import GraphDataToBaseline from './GraphDataToBaseline';
import Baseline from './Baseline';
import Interpolate from './Interpolate';

describe('GraphDataToBaselineTest', () => {
  it('graphDataToBaselineLinear', () => {
    const baseline = GraphDataToBaseline.graphDataToBaseline({
      timestamps: [2000, 3000],
      values: [3, 4],
    }, Interpolate.LINEAR);
    const expected = Baseline.forTest(
      [2000, 3000],
      [
        [0, 3, 3],
        [4, 4, 0],
      ],
    );
    // eslint-disable-next-line no-undef
    expect(baseline).toEqual(expected);
  });

  it('graphDataToBaselineLeft', () => {
    const graphData = {
      timestamps: [2000, 3000],
      values: [3, 4],
    };
    const baseline = GraphDataToBaseline.graphDataToBaseline(graphData, Interpolate.LEFT);
    const expected = Baseline.forTest(
      [2000, 3000],
      [
        [0, 3, 3],
        [3, 4, 0],
      ],
    );
    expect(baseline).toEqual(expected);
  });

  it('graphDataToBaselineRight', () => {
    const graphData = {
      timestamps: [2000, 3000],
      values: [3, 4],
    };
    const baseline = GraphDataToBaseline.graphDataToBaseline(graphData, Interpolate.RIGHT);
    const expected = Baseline.forTest(
      [2000, 3000],
      [
        [0, 3, 4],
        [4, 4, 0],
      ],
    );
    expect(baseline).toEqual(expected);
  });

  it('graphDataToBaselineNone', () => {
    const graphData = {
      timestamps: [2000, 3000],
      values: [3, 4],
    };
    const baseline = GraphDataToBaseline.graphDataToBaseline(graphData, Interpolate.NONE);
    const expected = Baseline.forTest(
      [2000, 3000],
      [
        [0, 3, 0],
        [0, 4, 0],
      ],
    );
    expect(baseline).toEqual(expected);
  });

  it('bug1Left', () => {
    const graphData = {
      timestamps: [1000, 1500, 2000, 2500, 3000],
      values: [NaN, 3, 3, 5, NaN],
    };
    const expected = Baseline.forTest(graphData.timestamps,
      [
        [0, 0, 0],
        [0, 3, 3],
        [3, 3, 3],
        [3, 5, 0],
        [0, 0, 0],
      ]);
    const baseline = GraphDataToBaseline.graphDataToBaseline(graphData, Interpolate.LEFT);
    expect(baseline).toEqual(expected);
  });
});
