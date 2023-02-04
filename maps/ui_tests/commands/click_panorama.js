/**
 * Click panorama's canvas at specific coordinate, origin (0, 0) is the top left point.
 *
 * @see browser.gridView to get understanding of the coordinates grid of the player.
 * @name browser.clickPanorama
 */
module.exports = async function(x, y) {
    const container = await this.$('#panorama-container');
    const size = await container.getSize();
    await container.click({
        button: 'left',
        x: x - size.width / 2,
        y: y - size.height / 2
    });
};
