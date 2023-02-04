const { execSync } = require('child_process');
const crypto = require('crypto');
const { join, dirname } = require('path');
const { mkdirSync, writeFileSync } = require('fs');
const kebabCase = require('lodash/kebabCase');
const { configureToMatchImageSnapshot } = require('jest-image-snapshot');
const CyrillicToTranslit = require('cyrillic-to-translit-js');

const cyrillicToTranslit = new CyrillicToTranslit();

const SCREENSHOTS_FOLDER = '__image_snapshots__';
const PRESERVE_FAILED_SCREENSHOTS = process.env.PRESERVE_FAILED_SCREENSHOTS;
const FAILED_SCREENSHOTS_FOLDER = process.env.FAILED_SCREENSHOTS_FOLDER;

// В именах тестов могут встречаться всякие символы, которые не хотим видеть в имени файла,
// но которые, возможно, нужны для уникальности этого имени
const specialCharacterRegexp = /[^a-zA-Z\d]/g;
const specialCharacterMap = {
    '№': 'number'
};

const customSnapshotIdentifier = ({ currentTestName, counter }) => {
    return cyrillicToTranslit
        .transform(
            kebabCase(`${ currentTestName }-${ counter }`)
        )
        .replace(
            specialCharacterRegexp,
            match => specialCharacterMap[match] || match
        );
};

const toMatchImageSnapshot = configureToMatchImageSnapshot();

const toMatchImageSnapshotFn = function(received, options) {
    const result = toMatchImageSnapshot.call(this, received, {
        ...options,
        customSnapshotIdentifier
    });

    if (! result.pass && PRESERVE_FAILED_SCREENSHOTS) {
        if (! FAILED_SCREENSHOTS_FOLDER) {
            // eslint-disable-next-line max-len
            throw new Error('Для сохранения упавших скриншотов нужно указать переменную окружения FAILED_SCREENSHOTS_FOLDER');
        }
        const { currentTestName, testPath, snapshotState } = expect.getState();
        const counter = snapshotState._counters.get(currentTestName);

        const imageName = customSnapshotIdentifier({
            currentTestName,
            counter
        });

        const projectName = process.cwd().match(/[^\/]*$/);
        const testPathFromProjectRoot = dirname(testPath).replace(process.cwd(), '');
        const testPathFromRepositoryRoot = [ projectName, testPathFromProjectRoot ].join('/');
        const relativeImagePath = join(testPathFromRepositoryRoot, SCREENSHOTS_FOLDER, `${imageName}-snap.png`);
        const fullImagePath = join(process.cwd(), FAILED_SCREENSHOTS_FOLDER, relativeImagePath);

        mkdirSync(dirname(fullImagePath), { recursive: true });
        writeFileSync(fullImagePath, Buffer.from(received));
    }

    return result;
};

const getChangedFiles = () => {
    let files = [];

    try {
        const filesOut = execSync('arc config core.quotepath false && arc diff --name-only HEAD^');
        const prefixOut = execSync('arc config core.quotepath false && arc rev-parse --show-toplevel');

        const prefix = prefixOut.toString().split('\n')[0];

        files = filesOut.toString().split('\n');
        files = files.map(f => join(prefix, f));
    } catch (e) {
        /* eslint-disable-next-line  no-console */
        console.warn(e);
    }

    return files;
};

const getBundleName = testPath => crypto.createHash('sha1').update(testPath).digest('hex');

module.exports = {
    customSnapshotIdentifier,
    getChangedFiles,
    getBundleName,
    toMatchImageSnapshotFn
};
