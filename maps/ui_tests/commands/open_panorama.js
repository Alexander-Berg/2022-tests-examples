const host = process.env.HOST;

if (!host) {
    throw new Error('No HOST env set');
}

/**
 * @name browser.openPanorama
 */
module.exports = async function() {
    await this.url(host);
    await this.waitUntil(async () => {
        const body = await this.$('body');
        const status = await body.getAttribute('status');
        return status === 'INITIALIZED';
    }, {
        timeout: 60000,
        timeoutMsg: 'panorama player was not initialized in time'
    });
};
