/* eslint-disable no-undef */
import isEqual from 'lodash/isEqual';
import { findNonUniqueLabels } from './labels';

describe('findNonUniqueLabels', () => {
  it('empty', () => {
    expect(findNonUniqueLabels([])).toEqual(new Set());
  });

  it('single series', () => {
    const series = [
      { labels: { sensor: 'sensor' } },
    ];
    const expectedLabels = new Set();
    expect(findNonUniqueLabels(series)).toEqual(expectedLabels);
  });

  it('single label', () => {
    const series = [
      { labels: { sensor: 'sensor', host: 'cluster' } },
      { labels: { sensor: 'sensor', host: 'Man' } },
      { labels: { sensor: 'sensor', host: 'Myt' } },
      { labels: { sensor: 'sensor', host: 'Sas' } },
    ];
    const expectedLabels = new Set(['host']);
    expect(isEqual(findNonUniqueLabels(series), expectedLabels)).toBeTruthy();
  });

  it('several labels', () => {
    const series = [
      { labels: { sensor: 'sensor1', host: 'cluster' } },
      { labels: { sensor: 'sensor1', host: 'Man' } },
      { labels: { sensor: 'sensor2', host: 'Myt' } },
      { labels: { sensor: 'sensor2', host: 'Sas' } },
    ];
    const expectedLabels = new Set(['sensor', 'host']);
    expect(isEqual(findNonUniqueLabels(series), expectedLabels)).toBeTruthy();
  });

  it('absent labels', () => {
    const series = [
      { labels: { sensor: 'sensor', label1: 'value1' } },
      { labels: { sensor: 'sensor', label2: 'value2' } },
      { labels: { sensor: 'sensor', label2: 'value2' } },
      { labels: { sensor: 'sensor' } },
    ];
    const expectedLabels = new Set(['label1', 'label2']);
    expect(isEqual(findNonUniqueLabels(series), expectedLabels)).toBeTruthy();
  });
});
