const chalk = require('chalk'),
    vow = require('vow');

/**
 * @name browser.debugLog
 */
module.exports = function() {
    process.env.VERBOSE && this.call(() => vow.all([
        this.getMeta('testStarted'),
        this.getMeta('testNumber')
    ]).spread((time, number) => {
        console.log(
            chalk.dim(' - '),
            chalk.bold((Math.round((+new Date() - time) / 100) / 10 + ' s     ').substr(0, 6)),
            chalk.bold(chalk.dim('[ ' + number + ' ]')),
            chalk.dim.apply(chalk, [].slice.call(arguments)));
    }));
    return true;
};