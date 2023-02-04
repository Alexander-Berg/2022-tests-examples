/**
 * @name browser.setViewportSize
 */
module.exports = async function(width, height) {
    const diff = await this.execute('return {width: window.outerWidth - window.innerWidth,height: window.outerHeight - window.innerHeight};');
    const vw = width + diff.width;
    const vh = height + diff.height;
    await this.setWindowSize(vw, vh);
};
