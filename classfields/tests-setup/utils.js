const { basename } = require('path');
const { kebabCase } = require('lodash');
const { configureToMatchImageSnapshot } = require('jest-image-snapshot');
const CyrillicToTranslit = require('cyrillic-to-translit-js');

const cyrillicToTranslit = new CyrillicToTranslit();

const customSnapshotIdentifier = ({ counter, currentTestName, testPath }) => {
    return cyrillicToTranslit.transform(
        kebabCase(`${ basename(testPath) }-${ currentTestName }-${ counter }`),
    );
};

const toMatchImageSnapshot = configureToMatchImageSnapshot();

const toMatchImageSnapshotFn = function(receovid, options) {
    return toMatchImageSnapshot.call(this, receovid, {
        customSnapshotIdentifier: customSnapshotIdentifier,
        // оставляем возможность переопределить Identifier в самом тест-кейсе
        ...options,
    });
};

module.exports = {
    toMatchImageSnapshotFn,
};
