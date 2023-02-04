/* eslint-disable no-undef,max-len */
import LogYAxisTransformer from './LogYAxisTransformer';

describe('LogYAxisTransformer', () => {
  it('testLogYAxisTwoSide', () => {
    let y;
    const trans = new LogYAxisTransformer(100 + (2 * LogYAxisTransformer.ZERO_AREA_MARGIN) + 1, -1500, 1500, 15);

    y = trans.yToChartY(0);
    expect(y).toEqual(50 + LogYAxisTransformer.ZERO_AREA_MARGIN);

    y = trans.yToChartY(15);
    expect(y).toEqual(50);

    y = trans.yToChartY(150);
    expect(y).toEqual(25);

    y = trans.yToChartY(1500);
    expect(y).toEqual(0);

    y = trans.yToChartY(-15);
    expect(y).toEqual(50 + (2 * LogYAxisTransformer.ZERO_AREA_MARGIN));

    y = trans.yToChartY(-150);
    expect(y).toEqual(75 + (2 * LogYAxisTransformer.ZERO_AREA_MARGIN));

    y = trans.yToChartY(-1500);
    expect(y).toEqual(100 + (2 * LogYAxisTransformer.ZERO_AREA_MARGIN));
  });

  it('testLogYAxisOnlyPositive', () => {
    const trans = new LogYAxisTransformer(50 + LogYAxisTransformer.ZERO_AREA_MARGIN + 1, 15, 1500, 1.5);

    let y;
    y = trans.yToChartY(0);
    expect(y).toEqual(50 + LogYAxisTransformer.ZERO_AREA_MARGIN);

    y = trans.yToChartY(15);
    expect(y).toEqual(50);

    y = trans.yToChartY(150);
    expect(y).toEqual(25);

    y = trans.yToChartY(1500);
    expect(y).toEqual(0);
  });

  it('testLogYAxisOnlyNegative', () => {
    const trans = new LogYAxisTransformer(50 + LogYAxisTransformer.ZERO_AREA_MARGIN + 1, -1500, -15, 15);

    let y;
    y = trans.yToChartY(0);
    expect(y).toEqual(0);

    y = trans.yToChartY(-15);
    expect(y).toEqual(LogYAxisTransformer.ZERO_AREA_MARGIN);

    y = trans.yToChartY(-150);
    expect(y).toEqual(25 + LogYAxisTransformer.ZERO_AREA_MARGIN);

    y = trans.yToChartY(-1500);
    expect(y).toEqual(50 + LogYAxisTransformer.ZERO_AREA_MARGIN);
  });

  /**
   * Test for scale with no decimal ticks on the displayed interval
   */
  it('testLogYAxisSmallRange', () => {
    let y;
    const trans = new LogYAxisTransformer(50 + LogYAxisTransformer.ZERO_AREA_MARGIN + 1, 30, 40, 30);

    y = trans.yToChartY(40);
    expect(y).toEqual(0);

    y = trans.yToChartY(30);
    expect(y).toEqual(50);

    y = trans.yToChartY(0);
    expect(y).toEqual(50 + LogYAxisTransformer.ZERO_AREA_MARGIN);
  });

  /**
   * Test for scale with custom selection range
   */
  it('testLogYAxisSmallCustomRange', () => {
    let y;
    const trans = new LogYAxisTransformer(50 + LogYAxisTransformer.ZERO_AREA_MARGIN + 1, 30, 40, 5);

    y = trans.yToChartY(40);
    expect(y).toEqual(0);

    y = trans.yToChartY(30);
    expect(y).toEqual(50);

    y = trans.yToChartY(0);
    expect(y).toEqual(50 + LogYAxisTransformer.ZERO_AREA_MARGIN);
  });

  it('testLogYAxisZeroes', () => {
    let y;
    const trans = new LogYAxisTransformer(100, 0, 0, 0);

    y = trans.yToChartY(0);
    expect(y).toEqual(50);

    y = trans.yToChartY(15);
    expect(y).toEqual(50);

    y = trans.yToChartY(-15);
    expect(y).toEqual(50);
  });
});
