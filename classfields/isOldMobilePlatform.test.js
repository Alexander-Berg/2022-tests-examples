const isOldMobilePlatform = require('./isOldMobilePlatform');

function generateTestConfig(OSFamily, OSVersion) {
    return {
        browser: {
            OSFamily: OSFamily,
            OSVersion: OSVersion,
        },
    };
}

describe('isOldMobilePlatform должна вернуть', () => {
    describe('для Android', () => {
        const OSFamily = 'Android';

        it('false на версию выше 4.4', () => {
            expect(isOldMobilePlatform(generateTestConfig(OSFamily, '8.0.0'))).toBe(false);
        });

        it('true на версию 4.4', () => {
            expect(isOldMobilePlatform(generateTestConfig(OSFamily, '4.4.1'))).toBe(true);
        });

        it('true на версию ниже 4.4 и выше 4.0', () => {
            expect(isOldMobilePlatform(generateTestConfig(OSFamily, '4.2'))).toBe(true);
        });

        it('true на версию ниже 4.0', () => {
            expect(isOldMobilePlatform(generateTestConfig(OSFamily, '3.1'))).toBe(true);
        });
    });

    describe('для iOS', () => {
        const OSFamily = 'iOS';

        it('false на версию выше 10', () => {
            expect(isOldMobilePlatform(generateTestConfig(OSFamily, '13.2.2'))).toBe(false);
        });

        it('true на версию 10', () => {
            expect(isOldMobilePlatform(generateTestConfig(OSFamily, '10.2.2'))).toBe(true);
        });

        it('true на версию ниже 10', () => {
            expect(isOldMobilePlatform(generateTestConfig(OSFamily, '9.0'))).toBe(true);
        });
    });
});
