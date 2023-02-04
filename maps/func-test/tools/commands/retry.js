/**
 * @name browser.retry
 * @param command
 * @param preparation
 * @param times
 */
module.exports = function async(command, preparation, times) {
    if(!times) {
        times = preparation;
        preparation = null;
    }
    let res = command.call(this);

    for(let i = 0; i < times; i++) {
        res = res.then(res => res, () => {
            this.debugLog('Retry ' + i + ' ' + command);
            return preparation?
                preparation.call(this).then(() => command.call(this)) :
                command.call(this);
        });
    }

    return res;
};
