import convolutionOfRanges from '../lib/convolution-of-ranges';

test('convolutionOfRanges with empty array', () => {
    expect(convolutionOfRanges([])).toBe('');
});

test('convolutionOfRanges with simple array', () => {
    expect(convolutionOfRanges([1, 2, 3])).toBe('1..3');
});

test('convolutionOfRanges with not simple array', () => {
    expect(convolutionOfRanges([1, 2, 3, 5, 6, 7])).toBe('1..3, 5..7');
});

test('convolutionOfRanges with single value', () => {
    expect(convolutionOfRanges([1])).toBe('1');
});

test('convolutionOfRanges with single value in the middle', () => {
    expect(convolutionOfRanges([1, 2, 3, 5, 7, 8])).toBe('1..3, 5, 7..8');
});

test('convolutionOfRanges with unordered array', () => {
    expect(convolutionOfRanges([2, 1, 6, 5, 3])).toBe('1..3, 5..6');
});
