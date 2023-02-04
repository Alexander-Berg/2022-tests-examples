/* eslint-disable no-undef */
import GradientColor from './GradientColor';
import GradientZone from './GradientZone';
import ColorScheme from './ColorScheme';
import ColorSchemeType from './ColorSchemeType';
import LineColor from './LineColor';

describe('colorScheme', () => {
  it('testGetColor', () => {
    const colors = {};
    colors[GradientColor.GREEN] = 1;
    colors[GradientColor.YELLOW] = 3;
    const labels = ['a0', 'a1', 'a2', 'a3', 'a4'];

    const colorScheme = new ColorScheme(ColorScheme.constructColorMap(colors, labels));

    expect(colorScheme.getColorOrAutoColor('a0')).toEqual(LineColor.gradientColor(GradientZone.GREEN_YELLOW, 0));
    expect(colorScheme.getColorOrAutoColor('a1')).toEqual(LineColor.gradientColor(GradientZone.GREEN_YELLOW, 0));
    expect(colorScheme.getColorOrAutoColor('a2')).toEqual(LineColor.gradientColor(GradientZone.GREEN_YELLOW, 0.5));
    expect(colorScheme.getColorOrAutoColor('a3')).toEqual(LineColor.gradientColor(GradientZone.YELLOW_RED, 0));
    expect(colorScheme.getColorOrAutoColor('a4')).toEqual(LineColor.gradientColor(GradientZone.YELLOW_RED, 0));
    expect(colorScheme.getColorOrAutoColor('a5')).toEqual(LineColor.autoColor());
  });

  it('testGetInputSetGradientColorMap', () => {
    const map = {};
    map[GradientColor.YELLOW] = 'b';
    map[GradientColor.RED] = 'c';
    map[GradientColor.VIOLET] = 'q';
    const strings = ['a', 'aa', 'b', 'c', 'd', 'f', 'q', 'r'];
    const actual = ColorScheme.getInputSetGradientColorMap(map, strings);

    expect(actual[GradientColor.GREEN]).toEqual(undefined);
    expect(actual[GradientColor.YELLOW]).toEqual(2);
    expect(actual[GradientColor.RED]).toEqual(3);
    expect(actual[GradientColor.VIOLET]).toEqual(6);
  });

  it('testGetUniqueGradientedLabels', () => {
    const map = {};
    map[GradientColor.YELLOW] = 'b';
    map[GradientColor.RED] = 'c';
    map[GradientColor.VIOLET] = 'q';
    const labels = ['aa', 'b', 'd', 'f', 'r'];
    const actual = ColorScheme.getUniqueGradientedLabels(labels, map);

    const expected = ['aa', 'b', 'c', 'd', 'f', 'q', 'r'];
    expect(actual).toEqual(expected);
  });

  it('testGetGradientColorLabels', () => {
    const expected = {};
    expected[GradientColor.YELLOW] = 'b';
    expected[GradientColor.RED] = 'c';
    expected[GradientColor.VIOLET] = 'q';
    const params = {
      type: ColorSchemeType.GRADIENT, green: '', yellow: 'b', red: 'c', violet: 'q',
    };
    const actual = ColorScheme.getGradientColorLabels(params);
    expect(actual).toEqual(expected);
  });

  it('testConstructGradientColorMapWithInnerIndexesSet', () => {
    const input = {};
    input[GradientColor.GREEN] = 1;
    input[GradientColor.RED] = 8;
    const expected = {};
    expected[GradientColor.GREEN] = 1;
    expected[GradientColor.YELLOW] = 4;
    expected[GradientColor.RED] = 8;
    expected[GradientColor.VIOLET] = 12;
    const actual = ColorScheme.addMissingPointsToGradientMap(input, 12);
    expect(actual).toEqual(expected);
  });

  it('testConstructGradientColorMapWithSingleColorSet', () => {
    const expected = {};
    expected[GradientColor.GREEN] = 0;
    expected[GradientColor.YELLOW] = 3;
    expected[GradientColor.RED] = 6;
    expected[GradientColor.VIOLET] = 10;
    const map = {};
    map[GradientColor.YELLOW] = 3;
    const actual = ColorScheme.addMissingPointsToGradientMap(map, 10);
    expect(actual).toEqual(expected);
  });

  it('testGetDefaultGradientColorMap', () => {
    const actual = ColorScheme.addMissingPointsToGradientMap({}, 10);
    const expected = {};
    expected[GradientColor.GREEN] = 0;
    expected[GradientColor.YELLOW] = 3;
    expected[GradientColor.RED] = 6;
    expected[GradientColor.VIOLET] = 10;
    expect(actual).toEqual(expected);
  });

  it('testIsUniqueGradientedLabels', () => {
    function testGradientedLabels(labels, expected) {
      const actual = ColorScheme.isUniqueGradientedLabels(labels);
      expect(actual).toEqual(expected);
    }

    testGradientedLabels([], false);
    testGradientedLabels(['label'], false);
    testGradientedLabels(['label1', 'label'], false);
    testGradientedLabels(['pre1', 'pre1suf'], false);
    testGradientedLabels(['1', 'pre1'], false);
    testGradientedLabels(['1', '1suff'], false);
    testGradientedLabels(['pre1min2', 'pre1min'], false);
    testGradientedLabels(['pre1mid1', 'pre2mid1'], false);

    testGradientedLabels(['1', '2'], true);
    testGradientedLabels(['pre1', 'pre2'], true);
    testGradientedLabels(['1suf', '2suf'], true);
    testGradientedLabels(['pre1suf', 'pre2suf'], true);
    testGradientedLabels(['pre1mid1', 'pre1mid2'], true);
    testGradientedLabels(['pre1mid1suf', 'pre1mid2suf'], true);
    testGradientedLabels(['pre1mid1', 'pre1mid2'], true);

    testGradientedLabels(['inf', 'pre1mid1', 'pre1mid2'], true);
    testGradientedLabels(['inf', '1', '2', '100ms', '200ms'], false);
  });

  it('testLabelPrefixAndSuffixOrNull', () => {
    function testPrefixAndSuffix(label, expected) {
      const actual = ColorScheme.labelPrefixAndSuffixOrNull(label);
      expect(actual).toEqual(expected);
    }

    testPrefixAndSuffix('label', null);
    testPrefixAndSuffix('pre1', ['pre', '']);
    testPrefixAndSuffix('1suf', ['', 'suf']);
    testPrefixAndSuffix('1', ['', '']);
    testPrefixAndSuffix('pre1mid2suf', ['pre1mid', 'suf']);
    testPrefixAndSuffix('pre1mid2', ['pre1mid', '']);
    testPrefixAndSuffix('1mid2', ['1mid', '']);
    testPrefixAndSuffix('1mid2suf', ['1mid', 'suf']);
  });
});
