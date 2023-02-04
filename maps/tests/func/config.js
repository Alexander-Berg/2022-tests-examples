const lang = process.env.API_LANG;
const apiVersion = process.env.API_VERSION;

function getHosts(environment) {
    const objHosts = require(`./hosts/${environment}`);
    let intHosts = {};

    try {
        intHosts = require(`./inthosts/${environment}`);
    } catch (e) {
        // it's ok if inthosts are missing for some env
    }

    let hostConfigs = '';

    if (process.env.DESIGN_EXP) {
        const exp = process.env.DESIGN_EXP;
        for (const i in objHosts) {
            objHosts[i] += '&' + exp;
        }
    }

    if (process.env.VECTOR_INDEX) {
        objHosts.vectorIndex = process.env.VECTOR_INDEX;
    }

    for (const i in objHosts) {
        hostConfigs += '&host_config[hosts][' + i + ']=' + encodeURIComponent(objHosts[i]);
    }

    for (const j in intHosts) {
        hostConfigs += '&host_config[inthosts][' + j + ']=' + encodeURIComponent(objHosts[j]);
    }

    return hostConfigs;
}

module.exports = {
    apiVersion: apiVersion,
    types: {
        reference: 'REFERENCE',
        actual: 'ACTUAL'
    },
    environment: {
        testing: 'TESTING',
        production: 'PRODUCTION',
        overriddenHosts: 'TESTING_OVERRIDDEN_HOSTS',
        testingWithProdTiles: 'TESTING_WITH_PROD_TILES'
    },

    TESTING: {
        apiUrl: `https://api-maps.tst.c.maps.yandex.ru/{version}/?lang=${lang}&mode=debug` +
        '&apikey=b027f76e-cc66-f012-4f64-696c7961c395'
    },
    PRODUCTION: {
        apiUrl: `https://api-maps.yandex.ru/{version}/?lang=${lang}&mode=debug` +
        '&apikey=86f7adc8-b86b-4d85-a48d-31ce3e44f592'
    },
    TESTING_OVERRIDDEN_HOSTS: {
        hosts: getHosts(process.env.API_HOSTS),
        apiUrl: `https://api-maps.tst.c.maps.yandex.ru/{version}/?lang=${lang}&mode=debug` +
        '&apikey=b027f76e-cc66-f012-4f64-696c7961c395' + getHosts(process.env.API_HOSTS)
    },
    TESTING_WITH_PROD_TILES: {
        hosts: getHosts('prod'),
        apiUrl: `https://api-maps.tst.c.maps.yandex.ru/{version}/?lang=${lang}&mode=debug` +
        '&apikey=b027f76e-cc66-f012-4f64-696c7961c395' + getHosts('prod')
    },
    mapsHost: {
        TESTING: 'https://l7test.yandex.ru/maps/',
        TESTING_OVERRIDDEN_HOSTS: 'https://l7test.yandex.ru/maps/',
        PRODUCTION: 'https://l7test.yandex.ru/maps-prod/',
        TESTING_WITH_PROD_TILES: 'https://l7test.yandex.ru/maps/'
    }
};
