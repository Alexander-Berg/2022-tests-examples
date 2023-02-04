/**
 *
 * @name browser.crSelectLastTab
 */
module.exports = function () {
    return this
        .getTabIds().then((handles) => {
            return this.switchTab(handles[handles.length - 1]);
        });
};
