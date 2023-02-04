/**
 * @name browser.gridView
 */
module.exports = async function() {
    await this.execute(function() {utils.renderGrid(container);});
    await this.assertViewPanorama('tmp-for-grid-review');
};
