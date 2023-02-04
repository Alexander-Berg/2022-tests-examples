var config = {
    serversUrlTemplates: {
        local: location.href.split('tests/manual')[0],
        production: 'https://api-maps.yandex.ru/${version}/',
        testing: 'https://api-maps.tst.c.maps.yandex.ru/${version}/',
        // enterpriseTesting: 'https://enterprise.api-maps.tst.c.maps.yandex.ru/${version}/',
        // enterpriseProduction: 'https://enterprise.api-maps.yandex.ru/${version}/'
    },

    keys: {
        production: '86f7adc8-b86b-4d85-a48d-31ce3e44f592',
        default: 'b027f76e-cc66-f012-4f64-696c7961c395'
    },

    // Dynamically loaded parameters.
    fileList: [],
    versions: {
        production: [],
        testing: [],
        // enterpriseTesting: [],
        // enterpriseProduction: []
    }
};

function generateLocalApiUrl() {
    const url = location.href.split('tests/manual')[0]
    const isLocal = location.hostname === 'localhost' || location.hostname === '127.0.0.1';

    return url + isLocal ? '/init.js' : '';
}
