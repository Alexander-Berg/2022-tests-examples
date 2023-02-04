'use strict';

const path = require('path');
const {runQtools, copyTarget, removeTarget, loadConfig, writeConfig} = require('./utils');

describe('qtools validate', () => {
    let initialDir;

    beforeEach(() => {
        initialDir = copyTarget();
    });

    afterEach(() => {
        removeTarget(initialDir);
    });

    it('should return 0 for valid config', () => {
        expect(() => runQtools('validate')).not.toThrowError();
    });

    it('should return 1 for invalid config', () => {
        const invalidConfig = loadConfig(path.resolve(__dirname, './fixtures/validate/invalid.qtools.json'));
        writeConfig(invalidConfig);

        expect(() => runQtools('validate -q')).toThrowError();
    });
});
