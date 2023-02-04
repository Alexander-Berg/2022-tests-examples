
const fs = require('fs');
const path = require('path');
const identity = require('lodash/identity');
const noop = require('lodash/noop');
const makeDir = require('make-dir');

const createCacher = ({
    buildPath,
    dataToContent = identity,
    contentToData = identity
}) => ({
    read: opts => {
        const filePath = buildPath(opts);

        return fs.existsSync(filePath) ? contentToData(fs.readFileSync(filePath, 'utf-8')) : null;
    },
    write: (opts, data) => {
        const filePath = buildPath(opts);

        return makeDir(path.dirname(filePath)).then(() => {
            fs.writeFile(filePath, dataToContent(data), noop);
        });
    }
});

module.exports = createCacher;

