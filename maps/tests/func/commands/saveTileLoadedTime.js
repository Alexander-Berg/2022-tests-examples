const writeResult = require('../tools/write-test-time.js');

/**
 * @name browser.saveTileLoadedTime
 * */
module.exports = function (result, fileindex = 1) {
    return writeResult(result, fileindex);
};
