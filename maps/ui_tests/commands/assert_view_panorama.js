/**
 * @name browser.assertViewPanorama
 */
module.exports = async function(name) {
    await this.assertView(name, '#panorama-container');
};
