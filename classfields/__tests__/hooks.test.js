const { translitSiteUrl } = require('realty-router/lib/hooks');

describe('hooks', () => {
    describe('Translit site url - invalid prefixes', () => {
        const PREFIXES = [ 'z', 'metro', 'mcd', 'st', 'dist', 'ao', 'railway' ];
        const INVALID_PREFIXES_TEST_CASES = PREFIXES.map(prefix => ({
            siteName: `${prefix} Nickolas apartments`,
            validUrl: `${prefix.toLowerCase()}nickolas-apartments`,
        }));

        INVALID_PREFIXES_TEST_CASES.map(testCase =>
            it(testCase.siteName, () => {
                const url = translitSiteUrl(testCase.siteName);

                expect(url).toBe(testCase.validUrl);
            })
        );
    });
});
