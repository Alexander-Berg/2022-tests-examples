/* eslint-disable no-undef */
import BinarySearch from './BinarySearch';

describe('BinarySearch', () => {
  function lowerBound(array, targetValue) {
    return BinarySearch.lowerBound(array.length, (index) => array[index], targetValue);
  }

  it('testLowerBound', () => {
    const testDataArray = [0, 1, 2, 3, 3, 4];

    expect(lowerBound(testDataArray, -1)).toEqual(0);
    expect(lowerBound(testDataArray, 0)).toEqual(0);
    expect(lowerBound(testDataArray, 0.5)).toEqual(1);
    expect(lowerBound(testDataArray, 1.0)).toEqual(1);
    expect(lowerBound(testDataArray, 1.5)).toEqual(2);
    expect(lowerBound(testDataArray, 2.0)).toEqual(2);
    expect(lowerBound(testDataArray, 3.0)).toEqual(3);
    expect(lowerBound(testDataArray, 4.0)).toEqual(5);
    expect(lowerBound(testDataArray, 5.0)).toEqual(6);

    expect(lowerBound([], 0)).toEqual(0);
  });

  // Copied from BinarySearchTest.firstIndex
  it('testFirstIndex', () => {
    expect(BinarySearch.firstIndex(10, () => true)).toEqual(0);
    expect(BinarySearch.firstIndex(10, (i) => i >= 2)).toEqual(2);
    expect(BinarySearch.firstIndex(10, () => false)).toEqual(10);
    expect(BinarySearch.firstIndex(0, () => { throw new Error(''); })).toEqual(0);
  });
});
