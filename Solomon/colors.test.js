/* eslint-disable no-undef */
import { convertHslToRgb, convertRgbToHsl, getColorAlexPetrov } from './colors';

const ALEX_PETROV_COLORS = [
  'rgb(146,219,0)', 'rgb(0,146,219)', 'rgb(219,183,0)', 'rgb(219,0,0)', 'rgb(0,219,146)',
  'rgb(37,0,219)', 'rgb(121,236,50)', 'rgb(14,63,103)', 'rgb(242,231,28)', 'rgb(121,43,12)',
  'rgb(32,223,182)', 'rgb(139,9,59)', 'rgb(38,103,14)', 'rgb(28,121,242)', 'rgb(113,121,12)',
  'rgb(223,108,32)', 'rgb(9,139,126)', 'rgb(217,23,75)', 'rgb(60,242,28)', 'rgb(50,12,121)',
  'rgb(188,223,32)', 'rgb(139,76,9)', 'rgb(23,214,217)', 'rgb(142,21,39)', 'rgb(16,121,12)',
  'rgb(121,32,223)', 'rgb(100,139,9)', 'rgb(217,146,23)', 'rgb(21,126,142)', 'rgb(245,107,112)',
  'rgb(32,223,207)', 'rgb(85,9,139)', 'rgb(136,217,23)', 'rgb(142,112,21)', 'rgb(107,210,245)',
  'rgb(161,30,18)', 'rgb(70,139,9)', 'rgb(23,146,217)', 'rgb(142,126,21)', 'rgb(245,135,107)',
  'rgb(18,161,118)', 'rgb(232,105,164)', 'rgb(91,217,23)', 'rgb(21,84,142)', 'rgb(245,242,107)',
  'rgb(161,63,18)', 'rgb(105,232,208)', 'rgb(180,14,72)', 'rgb(49,142,21)', 'rgb(107,162,245)',
  'rgb(146,161,18)', 'rgb(232,160,105)', 'rgb(14,180,169)', 'rgb(235,86,121)', 'rgb(123,245,107)',
  'rgb(73,18,161)', 'rgb(204,232,105)', 'rgb(180,105,14)', 'rgb(86,228,235)', 'rgb(180,29,47)',
  'rgb(18,161,18)', 'rgb(168,105,232)', 'rgb(125,180,14)', 'rgb(235,185,86)', 'rgb(29,155,180)',
  'rgb(240,66,66)', 'rgb(105,232,225)', 'rgb(116,14,180)', 'rgb(168,235,86)', 'rgb(180,147,29)',
  'rgb(66,191,240)', 'rgb(200,45,25)', 'rgb(86,180,14)', 'rgb(86,176,235)', 'rgb(180,165,29)',
  'rgb(240,107,66)', 'rgb(25,200,153)', 'rgb(223,67,135)', 'rgb(133,235,86)', 'rgb(29,102,180)',
  'rgb(237,240,66)', 'rgb(200,86,25)', 'rgb(67,223,200)', 'rgb(103,9,39)', 'rgb(59,180,29)',
  'rgb(66,130,240)', 'rgb(176,200,25)', 'rgb(223,140,67)', 'rgb(9,103,100)', 'rgb(228,47,84)',
  'rgb(81,240,66)', 'rgb(98,25,200)', 'rgb(184,223,67)', 'rgb(103,64,9)', 'rgb(47,213,228)',
  'rgb(108,19,27)', 'rgb(25,200,176)', 'rgb(151,67,223)', 'rgb(69,103,9)', 'rgb(228,174,47)',
];

const rgbHslPairs = [
  [{ r: 0, g: 0, b: 0 }, { h: 0, s: 0, l: 0 }],
  [{ r: 255, g: 255, b: 255 }, { h: 0, s: 0, l: 1 }],
  [{ r: 255, g: 0, b: 0 }, { h: 0, s: 1, l: 0.5 }],
  [{ r: 0, g: 255, b: 0 }, { h: 1 / 3, s: 1, l: 0.5 }],
  [{ r: 0, g: 0, b: 255 }, { h: 2 / 3, s: 1, l: 0.5 }],
  [{ r: 255, g: 255, b: 0 }, { h: 1 / 6, s: 1, l: 0.5 }],
  [{ r: 0, g: 255, b: 255 }, { h: 0.5, s: 1, l: 0.5 }],
  [{ r: 255, g: 0, b: 255 }, { h: 5 / 6, s: 1, l: 0.5 }],
  [{ r: 192, g: 192, b: 192 }, { h: 0, s: 0, l: 0.75 }],
  [{ r: 128, g: 128, b: 128 }, { h: 0, s: 0, l: 0.5 }],
  [{ r: 128, g: 0, b: 0 }, { h: 0, s: 1, l: 0.25 }],
  [{ r: 128, g: 128, b: 0 }, { h: 1 / 6, s: 1, l: 0.25 }],
  [{ r: 0, g: 128, b: 0 }, { h: 1 / 3, s: 1, l: 0.25 }],
  [{ r: 128, g: 0, b: 128 }, { h: 5 / 6, s: 1, l: 0.25 }],
  [{ r: 0, g: 128, b: 128 }, { h: 0.5, s: 1, l: 0.25 }],
  [{ r: 0, g: 0, b: 128 }, { h: 2 / 3, s: 1, l: 0.25 }],
];

describe('colors', () => {
  it('getColorAlexPetrov', () => {
    const actualColors = [];

    for (let i = 0; i < 100; ++i) {
      actualColors.push(getColorAlexPetrov(i));
    }

    expect(actualColors).toEqual(ALEX_PETROV_COLORS);
  });

  it('convertRgbToHsl', () => {
    rgbHslPairs.forEach((pair) => {
      const rgb = pair[0];
      const hsl = pair[1];
      const convertedHsl = convertRgbToHsl(rgb.r, rgb.g, rgb.b);
      expect(convertedHsl.h).toBeCloseTo(hsl.h, 2);
      expect(convertedHsl.s).toBeCloseTo(hsl.s, 2);
      expect(convertedHsl.l).toBeCloseTo(hsl.l, 2);
    });
  });

  it('convertHslToRgb', () => {
    rgbHslPairs.forEach((pair) => {
      const rgb = pair[0];
      const hsl = pair[1];
      const convertedRgb = convertHslToRgb(hsl.h, hsl.s, hsl.l);
      expect(convertedRgb.r).toBeCloseTo(rgb.r, -1);
      expect(convertedRgb.g).toBeCloseTo(rgb.g, -1);
      expect(convertedRgb.b).toBeCloseTo(rgb.b, -1);
    });
  });
});
