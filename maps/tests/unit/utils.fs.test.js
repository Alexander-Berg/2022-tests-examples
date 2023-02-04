'use strict';

const fsUtils = require('../../lib/utils/fs');

describe('utils/fs', () => {
    afterEach(() => {
        // Drop cache from require.
        require.cache = {};
    });

    describe('#loadFile()', () => {
        it('should return undefined for non-existent file', () => {
            const result = fsUtils.loadFile(`${__dirname}/fixtures/fs/non-existent-file`);
            expect(result).toBeUndefined();
        });

        ['json', 'yaml'].forEach((ext) => {
            it(`should load ${ext} file`, () => {
                const result = fsUtils.loadFile(`${__dirname}/fixtures/fs/config.${ext}`);
                expect(result).toEqual({ext, key: 'value'});
            });
        });
    });
});
