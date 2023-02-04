'use strict';

const sanitizer = require('../index');

describe('CDATA santizier', () => {
    it('should escape CDATA forbidden sequences', () => {
        expect(sanitizer.sanitizeCdata('something ]]> dirty'))
            .toEqual('something &#x5d;&#x5d;&gt; dirty');
    });

    it('should not alter strings that do not contain CDATA forbidden sequence', () => {
        const inputStr = `Lorem ipsum dolor sit amet, consectetur adipiscing elit.
            Aliquam mattis mi in sem facilisis, sit amet placerat elit accumsan.`;

        const resultStr = sanitizer.sanitizeCdata(inputStr);

        expect(inputStr).toEqual(resultStr);
    });

    it('should handle nullable input', () => {
        expect(sanitizer.sanitizeCdata(undefined)).toEqual('');
        expect(sanitizer.sanitizeCdata(null)).toEqual('');
    });
});
