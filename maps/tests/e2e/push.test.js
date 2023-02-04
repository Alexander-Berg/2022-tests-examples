'use strict';

const got = require('got');
const promiseRetry = require('promise-retry');
const {
    run,
    runQtools,
    copyTarget,
    removeTarget,
    loadConfig,
    writeConfig
} = require('./utils');
const {hosts} = require('../../lib/config');

jest.setTimeout(60000);

const HEADERS = {
    Accept: 'application/vnd.docker.distribution.manifest.v2+json',
    Authorization: `OAuth ${process.env.QTOOLS_TOKEN}`
};

describe('qtools push', () => {
    const IMAGE_NAME = 'junk/qtools-test-project';
    const TAG = String(Date.now());
    const FULL_IMAGE_NAME = `${hosts.registry}/${IMAGE_NAME}:${TAG}`;

    function removeImage() {
        const apiUrl = `https://${hosts.registry}/v2/${IMAGE_NAME}/manifests`;
        return got(`${apiUrl}/${TAG}`, {headers: HEADERS})
            .then((res) => got(`${apiUrl}/${res.headers['docker-content-digest']}`, {
                method: 'DELETE',
                headers: HEADERS
            }));
    }

    let initialDir;

    beforeAll(() => {
        initialDir = copyTarget();

        // Load config from temporary directory.
        const qtoolsYaml = loadConfig('.qtools.json');
        qtoolsYaml.registry.tag = TAG;
        writeConfig(qtoolsYaml);

        removeImage().catch(() => {});
        run(`docker build -t ${FULL_IMAGE_NAME} .`);
    });

    afterEach(() => {
        return promiseRetry((retry) => removeImage().catch(retry), {
            retries: 3
        }).catch(() => {});
    });

    afterAll(() => {
        run(`docker rmi -f ${FULL_IMAGE_NAME}`);
        removeTarget(initialDir);
    });

    it('should upload image to registry', () => {
        expect(() => runQtools('push')).not.toThrowError();
    });

    it('shouldn\'t upload image to the registry, if it\'s already exist', () => {
        expect(() => runQtools('push')).not.toThrowError();

        expect(() => runQtools('push')).toThrowError();
    });
});
