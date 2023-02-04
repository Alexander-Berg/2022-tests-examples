'use strict';

const utils = require('../../lib/utils');

describe('utils', () => {
    describe('#getDockerImageName()', () => {
        it('should return image name without registry', () => {
            const result = utils.getDockerImageName({registry: {
                repository: 'package-name',
                tag: 'package-version'
            }});
            const expected = 'registry.yandex.net/package-name:package-version';
            expect(result).toBe(expected);
        });

        it('should return image name', () => {
            const result = utils.getDockerImageName({registry: {
                prefix: 'junk',
                repository: 'package-name',
                tag: 'package-version'
            }});
            const expected = 'registry.yandex.net/junk/package-name:package-version';
            expect(result).toBe(expected);
        });
    });
});
