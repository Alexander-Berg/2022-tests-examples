const path = require('path');
const createCache = require('realty-core/app/test-utils/cache');
const resource = require('realty-core/app/resource');

const noCache = {
    read() {},
    write() {}
};

function getCache(cache, defaultCache) {
    if (cache) {
        return cache;
    }

    if (cache === false) {
        return noCache;
    }

    return defaultCache;
}

module.exports = async function callResource(resourcePath, opts, instanceConfig = {}, testOptions = {}) {
    const defaultCache = createCache({
        buildPath: () => path.join(__dirname, '__cache__', resourcePath, `${JSON.stringify(opts)}.json`),
        dataToContent: JSON.stringify,
        contentToData: JSON.parse
    });

    const cache = getCache(testOptions.cache, defaultCache);

    const cachedData = cache.read();

    if (cachedData) {
        return cachedData;
    }

    const res = await resource(resourcePath, opts, {
        isMandatory: true,
        ...instanceConfig
    });

    cache.write(opts, res);
    return res;
};
