/**
 * @name browser.waitUntilPanoramaLoaded
 */
module.exports = async function () {
    await this.waitUntil(async () => {
        const body = await this.$('body');
        const status = await body.getAttribute('status');
        return status === 'LOADED';
    }, {
        timeout: 30000,
        timeoutMsg: 'POV was not loaded in time'
    });
    await this.pause(1000); // tile fade-in animation
};
