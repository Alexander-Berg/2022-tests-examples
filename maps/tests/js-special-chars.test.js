'use strict';

const sanitizer = require('../index');

// @see {http://stackoverflow.com/questions/2965293/javascript-parse-error-on-u2028-unicode-character}
const UNSUPPORTED_CHARS = [
    0x0, 0x1, 0x2, 0x3, 0x4, 0x5, 0x6, 0x7, 0x8, 0xb, 0xc, // u0000-u0008, u000b, u000c
    0xe, 0xf, 0x10, 0x11, 0x12, 0x13, 0x14, 0x15, 0x16, 0x17,
    0x18, 0x19, 0x1a, 0x1b, 0x1c, 0x1d, 0x1e, 0x1f, // u000e-u001f
    0x7f, 0x80, 0x81, 0x82, 0x83, 0x84, 0x85, 0x86, 0x87,
    0x88, 0x89, 0x8a, 0x8b, 0x8c, 0x8d, 0x8e, 0x8f,
    0x90, 0x91, 0x92, 0x93, 0x94, 0x95, 0x96, 0x97,
    0x98, 0x99, 0x9a, 0x9b, 0x9c, 0x9d, 0x9e, 0x9f, // u007f-u009f
    0xad, // u00ad
    0x600, 0x601, 0x602, 0x603, 0x604, // u0600-u0604
    0x70f, 0x17b4, 0x17b5, // u070f, u17b4, u17b5
    0x200c, 0x200d, 0x200e, 0x200f, // u200c-u200f
    0x2028, 0x2029, 0x202a, 0x202b, 0x202c, 0x202d, 0x202e, 0x202f, // u202c-u202f
    0x2060, 0x2061, 0x2062, 0x2063, 0x2064, 0x2065, 0x2066, 0x2067,
    0x2068, 0x2069, 0x206a, 0x206b, 0x206c, 0x206d, 0x206e, 0x206f, // u2060-u206f
    0xfeff, // ufeff
    0xfff0, 0xfff1, 0xfff2, 0xfff3, 0xfff4, 0xfff5, 0xfff6, 0xfff7,
    0xfff8, 0xfff9, 0xfffa, 0xfffb, 0xfffc, 0xfffd, 0xfffe, 0xffff // ufff0-uffff
];

describe('JS/JSON sanitizer', () => {
    it('should remove only the characters that are in unspupported chars list', () => {
        let inputStr = '';
        const expectedArr = [];

        for (let i = 0; i <= 0xffff; i++) {
            // create a string with the full UTF-8 range
            inputStr += String.fromCharCode(i);

            // populate an array with only the supported UTF-8 characters
            if (UNSUPPORTED_CHARS.indexOf(i) === -1) {
                expectedArr.push(i);
            }
        }

        const resultArr = sanitizer.removeUtfSpecialChars(inputStr)
            .split('')
            .map((el) => el.charCodeAt(0));

        expect(expectedArr).toEqual(resultArr);
    });

    it('should leave a clean string as is', () => {
        const inputStr = `Lorem ipsum dolor sit amet, consectetur adipiscing elit.
            Aliquam mattis mi in sem facilisis, sit amet placerat elit accumsan.`;

        const resultStr = sanitizer.removeUtfSpecialChars(inputStr);

        expect(inputStr).toEqual(resultStr);
    });

    it('should handle nullable input', () => {
        expect(sanitizer.removeUtfSpecialChars(undefined)).toEqual('');
        expect(sanitizer.removeUtfSpecialChars(null)).toEqual('');
    });
});
