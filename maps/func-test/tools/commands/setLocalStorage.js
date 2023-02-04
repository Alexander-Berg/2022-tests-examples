/**
 * @name browser.setLocalStorage
 */
module.exports = function() {

    return this.execute(function() {
        localStorage.setItem('nk:app-state', JSON.stringify({
            eventsMonitor: {
                promptShown: false,
                hintShown: false
            }
        }));
        return true;
    });
};
