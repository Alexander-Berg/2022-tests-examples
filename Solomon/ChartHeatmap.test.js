/* eslint-disable no-undef */
import ChartHeatmap from './ChartHeatmap';

it('computeSquareSize', () => {
  expect(ChartHeatmap.computeSquareSize(1, 10, 10)).toEqual(10);
  expect(ChartHeatmap.computeSquareSize(4, 10, 10)).toEqual(5);
  expect(ChartHeatmap.computeSquareSize(8, 10, 20)).toEqual(5);
  expect(ChartHeatmap.computeSquareSize(1, 3, 4)).toEqual(3);
  expect(ChartHeatmap.computeSquareSize(9, 10, 12)).toEqual(3);
  expect(ChartHeatmap.computeSquareSize(11, 300, 400)).toEqual(100);
});
