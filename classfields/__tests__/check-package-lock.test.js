'use strict';

const {
    checkPackageLock,
} = require('../lib/check-package-lock');

describe('checkLockfileVersion', () => {
    let errorRe;
    beforeEach(() => {
        errorRe = /Your package-lock\.json is invalid/;
    });

    it('should throw error for invalid package-lock', () => {
        expect(() => checkPackageLock({}, 'package-lock.json')).toThrow(/"lockfileVersion" is undefined/);
    });

    it('should return error if lockfileVersion is 0', () => {
        expect(checkPackageLock({ lockfileVersion: 0, dependencies: {} }, 'package-lock.json')).toMatch(errorRe);
    });

    it('should return error if lockfileVersion is 1', () => {
        expect(checkPackageLock({ lockfileVersion: 1, dependencies: {} }, 'package-lock.json')).toMatch(errorRe);
    });

    it('should return error if lockfileVersion is null', () => {
        expect(checkPackageLock({ lockfileVersion: null, dependencies: {} }, 'package-lock.json')).toMatch(errorRe);
    });

    it('should return error if lockfileVersion === 2 and "dependencies" field presents', () => {
        expect(checkPackageLock({ lockfileVersion: 2, dependencies: {} }, 'package-lock.json')).toMatch(errorRe);
    });

    it('should not return error if lockfileVersion is 2', () => {
        expect(checkPackageLock({ lockfileVersion: 2, packages: {} }, 'package-lock.json')).toBeUndefined();
    });
});

describe('checkResolvedPaths', () => {
    let errorRe;
    beforeEach(() => {
        errorRe = /You have forbidden "resolved" fields in package-lock\.json./;
    });

    it('should return error for "resolved": "http://npm.yandex-team.ru/..."', () => {
        const packageLock = {
            lockfileVersion: 2,
            packages: {
                '@babel/code-frame': {
                    resolved: 'http://npm.yandex-team.ru/@babel%2fcode-frame/-/code-frame-7.14.5.tgz?rbtorrent=30377ed25ac679e0628fd8ff17dcb076d477e07b',
                },
            },
        };

        expect(checkPackageLock(packageLock, 'package-lock.json')).toMatch(errorRe);
    });

    it('should not return error for "resolved": "https://npm.yandex-team.ru/..."', () => {
        const packageLock = {
            lockfileVersion: 2,
            packages: {
                '@babel/code-frame': {
                    resolved: 'https://npm.yandex-team.ru/@babel%2fcode-frame/-/code-frame-7.14.5.tgz?rbtorrent=30377ed25ac679e0628fd8ff17dcb076d477e07b',
                },
            },
        };

        expect(checkPackageLock(packageLock, 'package-lock.json')).toBeUndefined();
    });

    it('should not return error for "version": "file:..."', () => {
        const packageLock = {
            lockfileVersion: 2,
            packages: {
                'www-desktop': {
                    version: 'file:www-desktop',
                },
            },
        };

        expect(checkPackageLock(packageLock, 'package-lock.json')).toBeUndefined();
    });

    it('should not skip errors if local symlink exists', () => {
        const packageLock = {
            lockfileVersion: 2,
            packages: {
                'www-desktop': {
                    version: 'file:www-desktop',
                },
                'node_modules/www-desktop': {
                    resolved: 'www-desktop',
                },
                xyz: {
                    resolved: 'http://npm.yandex-team.ru/xyz/-/xyz-7.14.5.tgz?rbtorrent=30377ed25ac679e0628fd8ff17dcb076d477e07b',
                },
            },
        };

        expect(checkPackageLock(packageLock, 'package-lock.json')).toMatch(errorRe);
    });

    it('should return error for "resolved": "https://registry.npmjs.org/..." (1 deep level)', () => {
        const packageLock = {
            lockfileVersion: 2,
            packages: {
                '@babel/code-frame': {
                    resolved: 'https://registry.npmjs.org/@babel/code-frame/-/code-frame-7.14.5.tgz',
                },
            },
        };

        expect(checkPackageLock(packageLock, 'package-lock.json')).toMatch(errorRe);
    });

    it('should return error for "resolved": "https://registry.npmjs.org/..." (2 deep level)', () => {
        const packageLock = {
            lockfileVersion: 2,
            packages: {
                '@babel/core': {
                    resolved: 'https://npm.yandex-team.ru/@babel/core/-/core-7.14.6.tgz',
                    dependencies: {
                        'source-map': {
                            resolved: 'https://registry.npmjs.org/source-map/-/source-map-0.5.7.tgz',
                        },
                    },
                },
            },
        };

        expect(checkPackageLock(packageLock, 'package-lock.json')).toMatch(errorRe);
    });

    it('should return error for "resolved": ".../_not_found/..."', () => {
        const packageLock = {
            lockfileVersion: 2,
            packages: {
                '@babel/core': {
                    resolved: 'https://npm.yandex-team.ru/_not_found/@babel/core/-/core-7.14.6.tgz',
                },
            },
        };

        expect(checkPackageLock(packageLock, 'package-lock.json')).toMatch(errorRe);
    });
});
