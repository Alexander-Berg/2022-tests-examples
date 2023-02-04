'use strict';

const util = require('util');
const sanitizer = require('../index');

describe('HTML sanitizier schema validation', () => {
    it('should restrict schemes used in href to a predefined list', () => {
        expect(sanitizer.sanitizeHtml('<a href="naughty:asdbsd">click me</a>'))
            .toEqual('<a>click me</a>');
    });

    it('should restrict schemes used in src to a predefined list', () => {
        const customTags = sanitizer.getDefaultAllowedTags();
        customTags.add('img');
        expect(sanitizer.sanitizeHtml('<img src="image://asd"/>', {allowedTags: customTags}))
            .toEqual('<img />');
    });

    it('should allow certain schemes in href', () => {
        const linkTempl = '<a href="%s://click-me">click me</a>';
        const httpLink = util.format(linkTempl, 'http');
        expect(sanitizer.sanitizeHtml(httpLink)).toEqual(httpLink);

        const httpsLink = util.format(linkTempl, 'https');
        expect(sanitizer.sanitizeHtml(httpsLink)).toEqual(httpsLink);

        const ftpLink = util.format(linkTempl, 'ftp');
        expect(sanitizer.sanitizeHtml(ftpLink)).toEqual(ftpLink);

        const mailtoLink = util.format(linkTempl, 'mailto');
        expect(sanitizer.sanitizeHtml(mailtoLink)).toEqual(mailtoLink);

        const telLink = util.format(linkTempl, 'tel');
        expect(sanitizer.sanitizeHtml(telLink)).toEqual(telLink);
    });

    it('should allow certain schemes in src', () => {
        const customTags = sanitizer.getDefaultAllowedTags();
        customTags.add('img');

        const imgTempl = '<img src="%s:something" />';
        const httpImg = util.format(imgTempl, 'http');
        expect(sanitizer.sanitizeHtml(httpImg, {allowedTags: customTags}))
            .toEqual(httpImg);

        const httpsImg = util.format(imgTempl, 'https');
        expect(sanitizer.sanitizeHtml(httpsImg, {allowedTags: customTags}))
            .toEqual(httpsImg);

        const ftpImg = util.format(imgTempl, 'ftp');
        expect(sanitizer.sanitizeHtml(ftpImg, {allowedTags: customTags}))
            .toEqual(ftpImg);

        const mailtoImg = util.format(imgTempl, 'mailto');
        expect(sanitizer.sanitizeHtml(mailtoImg, {allowedTags: customTags}))
            .toEqual(mailtoImg);

        const telImg = util.format(imgTempl, 'tel');
        expect(sanitizer.sanitizeHtml(telImg, {allowedTags: customTags}))
            .toEqual(telImg);
    });
});
