/**
 * @name browser.clearLocalStorage
 */
module.exports = function() {
    return this.execute(function() {
        localStorage.clear();
        return true;
    });
};
