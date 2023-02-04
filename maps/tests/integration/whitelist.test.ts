import {URL} from 'url';
import {expect} from 'chai';
import {whitelist} from 'app/lib/whitelist';

describe('whitelist loading', () => {
    it('should load empty config', () => {
        expect(whitelist.isReferrerAllowed(new URL('http://example.org'))).to.be.false;
    });

    it('should load config with comments, skipping empty lines', () => {
        expect(whitelist.isReferrerAllowed(new URL('http://example.org'))).to.be.false;
        expect(whitelist.isReferrerAllowed(new URL('http://yandex.com.tr'))).to.be.true;
        expect(whitelist.isReferrerAllowed(new URL('http://yandex.com'))).to.be.true;
        expect(whitelist.isReferrerAllowed(new URL('http://yandex.ru'))).to.be.true;
    });
});
