'use strict';

const sanitizer = require('../index');

describe('HTML sanitizier interface restrictions', () => {
    it('shouldn\'t permit allowing all tags', () => {
        const dirtyStr = 'hey hallo <textarea>something</textarea> hallo';

        expect(sanitizer.sanitizeHtml(dirtyStr, {allowedTags: false})).toEqual('hey hallo  hallo');
    });

    it('shouldn\'t permit allowing all attributes', () => {
        const dirtyStr = 'hey hallo <p custom-attribute="custom-value">something</p> hallo';

        expect(sanitizer.sanitizeHtml(dirtyStr, {allowedAttributes: false}))
            .toEqual('hey hallo <p>something</p> hallo');
    });

    it('shouldn\'t allow using wildcards in attribute names', () => {
        const dirtyStr = 'hey hallo <p custom-attribute="custom-value">something</p> hallo';

        expect(sanitizer.sanitizeHtml(dirtyStr, {allowedAttributes: {p: ['custom-*']}}))
            .toEqual('hey hallo <p>something</p> hallo');
    });

    it('should allow usage of allowedAttribues only on concrete names', () => {
        const dirtyStr = 'hey hallo <p custom-attribute="custom-value">something</p> hallo';

        expect(sanitizer.sanitizeHtml(dirtyStr, {allowedAttributes: {p: ['custom-attribute']}}))
            .toEqual(dirtyStr);
    });

    it('shouldn\'t allow extending the list of schemes in URLs', () => {
        const dirtyStr = 'here\'s a naughty <a href="naughttp://mm.mm">link</a>';

        expect(sanitizer.sanitizeHtml(dirtyStr, {allowedSchemes: ['naughttp']}))
            .toEqual('here\'s a naughty <a>link</a>');
    });

    it('shouldn\'t allow extending the list of schemes in URLs for certain tags', () => {
        const dirtyStr = 'here\'s a naughty <a href="naughttp://mm.mm">link</a>';

        expect(sanitizer.sanitizeHtml(dirtyStr, {allowedSchemesByTag: {a: ['naughttp']}}))
            .toEqual('here\'s a naughty <a>link</a>');
    });

    it('should handle nullable input', () => {
        expect(sanitizer.sanitizeHtml(undefined)).toEqual('');
        expect(sanitizer.sanitizeHtml(null)).toEqual('');
    });
});
