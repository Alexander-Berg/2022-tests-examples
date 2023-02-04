const chalk = require('chalk');

/**
 * @name browser.debugLog
 */
module.exports = function() {
    const text = [].slice.call(arguments);
    process.env.VERBOSE && this.call(() => Promise.all([
        this.getMeta('testStarted'),
        this.getMeta('testNumber')
    ]).spread((time, number) => {
        console.log(
            chalk.dim(' - '),
            chalk.bold((Math.round((+new Date() - time) / 100) / 10 + ' s     ').substr(0, 6)),
            chalk.bold(chalk.dim('[ ' + number + ' ]')),
            chalk.dim.apply(chalk, text));
    }));
    return this.getMeta('steps').then((steps = '') =>
        this.setMeta('steps', steps += ' =======> ' + text)
    );
};
