'use strict';

const fs = require('fs');
const jsYaml = require('js-yaml');

const {prepareQtoolsConfig} = require('../../lib/utils');
const validate = require('../../lib/validate');

describe('validate', () => {
    it('should return undefined', () => {
        const config = jsYaml.safeLoad(fs.readFileSync(__dirname + '/fixtures/qtools-config-good.yaml'));
        expect(
            validate(prepareQtoolsConfig(config, null, {skipValidation: true}))
        ).toBeUndefined();
    });

    it('should throw error', () => {
        const config = jsYaml.safeLoad(fs.readFileSync(__dirname + '/fixtures/qtools-config-bad.yaml'));
        expect(
            () => validate(prepareQtoolsConfig(config, null, {skipValidation: true}))
        ).toThrowErrorMatchingSnapshot('validate.ValidationError');
    });
});
