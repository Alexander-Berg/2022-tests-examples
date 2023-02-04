'use strict';

const sanitizer = require('../index');

describe('Interface for changing the allowed tags in santizeHtml()', () => {
    it('should change the default tag list', () => {
        const customTags = sanitizer.getDefaultAllowedTags();
        customTags.add('strange');

        expect(customTags.has('br')).toBe(true);
        customTags.delete('br');

        const strangeHtml = '<strange>1</strange><br/>';

        expect(sanitizer.sanitizeHtml(strangeHtml, {allowedTags: customTags}))
            .toEqual('<strange>1</strange>');
    });

    it('should not change the default tag list', () => {
        const customTags = sanitizer.getDefaultAllowedTags();
        customTags.add('strange');

        expect(customTags.has('br')).toBe(true);
        customTags.delete('br');

        const strangeHtml = '<strange>1</strange><br/>';

        expect(sanitizer.sanitizeHtml(strangeHtml)).toEqual('1<br />');
    });

    it('should preserve the ability to pass an array to the allowedTags options', () => {
        const strangeHtml = '<strange>1</strange>';

        expect(sanitizer.sanitizeHtml(strangeHtml, {allowedTags: ['strange']}))
            .toEqual(strangeHtml);
    });
});
