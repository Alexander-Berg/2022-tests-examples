'use strict';

const path = require('path');
const {
    run,
    runQtools,
    copyTarget,
    removeTarget,
    loadConfig
} = require('./utils');

describe('qtools init', () => {
    const REGISTRY_PREFIX = 'maps';
    const REPOSITORY_NAME = 'front-pano-ugc';
    const ABC_SERVICE_NAME = `${REGISTRY_PREFIX}-${REPOSITORY_NAME}`;
    const INCORRECT_ABC_SERVICE_NAME = 'maps-front-junk';

    let initialDir;

    beforeEach(() => {
        initialDir = copyTarget();
    });

    afterEach(() => {
        removeTarget(initialDir);
    });

    it('should fail with existed qtools config', () => {
        expect(() => runQtools(`init --abc=${ABC_SERVICE_NAME}`))
            .toThrowError('Qtools config already exists');
    });

    it('should fail without an "--abc" option', () => {
        run('rm .qtools.json');
        expect(() => runQtools('init')).toThrowError('Missing required arguments: abc');
    });

    it('should fail with non-existent abc service', () => {
        run('rm .qtools.json');
        expect(() => runQtools(`init --abc=${INCORRECT_ABC_SERVICE_NAME}`))
            .toThrowError(`Cannot find ABC service by "${INCORRECT_ABC_SERVICE_NAME}".`);
    });

    it('should create initial config with correct "--abc" option', () => {
        run('rm .qtools.json');
        const expectedConfig = loadConfig(path.resolve(__dirname, '../../defaults/.qtools.json'));
        expectedConfig.registry.prefix = REGISTRY_PREFIX;
        expectedConfig.registry.repository = REPOSITORY_NAME;
        expectedConfig.abcServiceName = ABC_SERVICE_NAME;

        expect(() => runQtools(`init --abc=${ABC_SERVICE_NAME}`)).not.toThrowError();

        const resultConfig = loadConfig('.qtools.json');
        expect(resultConfig).toEqual(expectedConfig);
    });
});
