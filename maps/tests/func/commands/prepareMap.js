/**
 * process.env.TYPE = REFERENCE|ACTUAL
 * process.env.ENVIRONMENT_REFERENCE = PRODUCTION|TESTING|TESTING_OVERRIDDEN_HOSTS
 * process.env.ENVIRONMENT_ACTUAL = PRODUCTION|TESTING|TESTING_OVERRIDDEN_HOSTS
 * process.env.API_VERSION_REFERENCE
 * process.env.API_VERSION_ACTUAL
 *
 */
const url = require('url');
const {URLSearchParams} = url;

const config = require('../config');
const env = process.env;
const type = env.TYPE;
const mode = env['ENVIRONMENT_' + type] || config.environment.testing;
const apiVersion = env['API_VERSION_' + type] || config.apiVersion;
const apiUrl = config[mode].apiUrl.replace('{version}', apiVersion);

const apiVersionReference = env.API_VERSION_REFERENCE || config.apiVersion;
const apiVersionActual = env.API_VERSION_ACTUAL || config.apiVersion;
const modeReference = env.ENVIRONMENT_REFERENCE || config.environment.testing;
const modeActual = env.ENVIRONMENT_ACTUAL || config.environment.testing;
const actualHosts = env.API_HOSTS
const apiUrlReference = config[modeReference].apiUrl.replace('{version}', apiVersionReference);
const apiUrlActual = config[modeActual].apiUrl.replace('{version}', apiVersionActual);

const MAP_LINK_ADDITIONAL_HOST_ENVS = [
    config.environment.testingWithProdTiles,
    config.environment.overriddenHosts
];

/**
 * @name browser.prepareMap
 * @param {Object} options
 */
module.exports = async function (options) {
    let mapsLink = url.format({
        host: config.mapsHost[mode],
        search: new URLSearchParams({
            'jsapi-version': apiVersion,
            ll: options.center.slice().reverse(),
            z: options.zoom
        }).toString()
    });
    const referenceType = Number(process.env.VECTOR_ALL) ? 'VECTOR' : 'RASTER';
    const actualType = Number(process.env.VECTOR) ? 'VECTOR' : 'RASTER';

    if (MAP_LINK_ADDITIONAL_HOST_ENVS.includes(mode)) {
        mapsLink += config.TESTING_OVERRIDDEN_HOSTS.hosts;
    }

    const browser = this;

    if (process.env.NIRVANA) {
        await browser.setMeta('night', process.env.NIGHT);
    } else {
        await browser.setMeta('maps', mapsLink);
        await browser.setMeta('apiReference', apiUrlReference);
        await browser.setMeta('apiActual', apiUrlActual);
        await browser.setMeta('envReference', referenceType + ', ' + modeReference);
        await browser.setMeta('envActual', actualType + ', ' + modeActual);
        await browser.setMeta('actualHosts', actualHosts);;
    }
    await browser.pause(100);
    await browser.execute((apiUrl) => {
        const head = document.getElementsByTagName('head')[0];
        const script = document.createElement('script');

        script.type = 'text/javascript';
        script.charset = 'utf-8';
        script.src = apiUrl;

        head.appendChild(script);

        return true;
    }, apiUrl);
    await browser.pause(1000);
};
