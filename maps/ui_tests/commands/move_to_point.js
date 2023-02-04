/**
 * Move the mouse by an offset of the specificed point. 
 *
 * @see browser.gridView to get understanding of the coordinates grid of the player.
 * @name browser.moveToPoint
 */
module.exports = async function(x, y) {
    const container = await this.$('#panorama-container');
    await container.moveTo({
        xOffset: x, 
        yOffset: y
    });
}