'use strict';

const {mkdtempSync} = require('fs');
const fsUtils = require('../../lib/utils/fs');
const path = require('path');
const {execSync} = require('child_process');

const SOURCE = './project';
const TARGET = '/tmp/qtools-';

const QTOOLS_BIN = path.resolve(__dirname, '../../bin/qtools.js');
const QTOOLS = `node ${QTOOLS_BIN}`;

module.exports = {
    run,

    runQtools: (command) => run(`${QTOOLS} ${command}`),

    copyTarget: () => {
        const tmpFolder = mkdtempSync(TARGET);
        run(`cp -R ${SOURCE}/. ${tmpFolder}`, {cwd: __dirname});

        const dir = process.cwd();
        process.chdir(tmpFolder);

        return dir;
    },

    removeTarget: (dir) => {
        process.chdir(dir);
    },

    loadConfig: (filepath) => fsUtils.loadFile(filepath),

    writeConfig: (config) => fsUtils.writeQtoolsConfig(config)
};

function run(command, options) {
    return execSync(command, Object.assign({
        encoding: 'utf8',
        stdio: 'pipe',
        cwd: process.cwd()
    }, options));
}
