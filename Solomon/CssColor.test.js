/* eslint-disable no-undef */
import * as CssColor from './CssColor';

const colors = [
  ['red', { r: 255, g: 0, b: 0 }],
  ['#ab0', { r: 170, g: 187, b: 0 }],
  ['#ab0ffe', { r: 171, g: 15, b: 254 }],
  ['#CCC', { r: 204, g: 204, b: 204 }],
  ['#cccdd0', { r: 204, g: 205, b: 208 }],
  ['rgb(0, 255, 44)', { r: 0, g: 255, b: 44 }],
  ['darkdarkblue', null],
  ['#ab', null],
  ['#ab00', null],
  ['rgb(-1, 12, 44)', null],
  ['rgb(1, 256, 44)', null],
  ['hsl(120,50%,50%)', null],
];

describe('CssColor', () => {
  it('isValid', () => {
    colors.forEach((color) => {
      const name = color[0];
      const rgb = color[1];

      // Color name is valid <=> rgb is not null
      expect(CssColor.isValid(name)).toEqual(rgb !== null);
    });
  });

  it('parseHex6', () => {
    expect(CssColor.parseHex('#1002aB')).toEqual({ r: 0x10, g: 0x02, b: 0xab });
    expect(CssColor.parseHex('#F00')).toEqual({ r: 0xFF, g: 0x0, b: 0x0 });
  });

  it('parseColor', () => {
    colors.forEach((color) => {
      const name = color[0];
      const rgb = color[1];

      expect(CssColor.parseColorOrNull(name)).toEqual(rgb);
    });
  });
});
