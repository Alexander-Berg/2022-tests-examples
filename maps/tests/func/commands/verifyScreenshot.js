/**
 * @name browser.verifyScreenshot
 * @param state state name, should be unique within one test
 * @param selectors DOM-node selector that you need to capture
 * @param opts additional options, currently available:
 * "ignoreElements", "tolerance", "antialiasingTolerance", "allowViewportOverflow", "captureElementFromTop", "compositeImage", "screenshotDelay"
 * */
module.exports = function (state, selectors, opts) {
    return this.assertView(state, selectors, Object.assign({}, opts, {ignoreElements: 'ymaps[class$="-copyright"]'}));
};
