const fs = require('fs');
const path = require('path');

const hash = require('object-hash');

const getBaseFixturesDirectory = () => {
    const state = expect.getState();
    const { testPath } = state;

    return path.join(testPath, '..', '__e2e_fixtures__');
};

const getFixtureName = key => hash(key).substring(0, 10);

const getFixturePath = ({ key, orderNum = 0 }) => {
    const { resourceName, methodName } = key;
    const resourcePath = path.join(getBaseFixturesDirectory(), resourceName, methodName);
    const fixtureName = getFixtureName(key);

    if (!fs.existsSync(resourcePath)) {
        fs.mkdirSync(resourcePath, { recursive: true });
    }

    return path.join(resourcePath, `${fixtureName}-${orderNum}.json`);
};

module.exports = {
    getBaseFixturesDirectory,
    getFixtureName,
    getFixturePath,
};
