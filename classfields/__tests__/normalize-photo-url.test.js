/* eslint-disable quotes */
jest.mock('realty-core/app/lib/offer-ban-reasons/reasons');

const { normalizePhotoUrl } = require('../parse-owner-offer');

describe('normalizePhotoUrl', () => {
    it('parses photo with relative protocol', () => {
        expect(normalizePhotoUrl('//www/image.jpg'))
            .toEqual('https://www/image.jpg');
    });

    it('parses photo with defined protocol', () => {
        expect(normalizePhotoUrl('http://www/image.jpg'))
            .toEqual('http://www/image.jpg');
    });

    it('escapes space, parentheses and quotes', () => {
        expect(normalizePhotoUrl(`http://www/"image" '(1)'.jpg`))
            .toEqual(`http://www/\\"image\\"\\ \\'\\(1\\)\\'.jpg`);
    });
});
