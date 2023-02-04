const pageObject = require('../../page-object.js'),
    { DRAW_TIMEOUT, UPDATE_TIMEOUT } = require('../../tools/constants'),
    MAX_ZOOM = 21;

require('../common.js')(beforeEach, afterEach);

describe('ruler', function() {
    beforeEach(function() {
        return this.browser
            .initNmaps('common')
            .setMapCenterByTestNumber(MAX_ZOOM)
            .pause(UPDATE_TIMEOUT);
    });

    it('control is visible', function() {
        return this.browser
            .waitForVisible(pageObject.rulerIcon())
            .verifyScreenshot(pageObject.mapControlsViewRuler(), 'ruler-control-off');
    });

    it('control is highlighted on click', function() {
        return this.browser
            .waitForVisible(pageObject.rulerIcon())
            .pointerClick(pageObject.rulerIcon())
            .waitForVisible(pageObject.nkButtonChecked() + ' ' + pageObject.rulerIcon())
            .verifyScreenshot(pageObject.mapControlsViewRuler(), 'ruler-control-on');
    });

    it('balloon shows total distance', function() {
        return drawRulerLine.call(this.browser)
            .waitForVisible(pageObject.ymaps.rulerDist())
            .verifyMapScreenshot(300, 300, 200, 200, 'ruler-line-drawn');
    });

    it('line is changed by drag', function() {
        return drawRulerLine.call(this.browser)
            .moveToObject('body', 400, 350)
            .mouseDrag([400, 350], [400, 300])
            .pause(UPDATE_TIMEOUT)
            .pointerClick(400, 300)
            .pause(UPDATE_TIMEOUT)
            .verifyMapScreenshot(300, 250, 200, 225, 'ruler-line-changed');
    });

    it('vertex is deleted by double click', function() {
        return drawRulerLine.call(this.browser)
            .moveToObject('body', 450, 350)
            .pause(UPDATE_TIMEOUT)
            .leftClick('body', 450, 350)
            .leftClick('body', 450, 350)
            .pause(UPDATE_TIMEOUT)
            .verifyMapScreenshot(300, 300, 200, 200, 'ruler-vertex-deleted');
    });

    it('close alert is appeared', function() {
        return drawRulerLine.call(this.browser)
            .waitForVisible(pageObject.ymaps.rulerDist())
            .pointerClick(pageObject.ymaps.balloon.closeBtn())
            .debugLog('Accept alert')
            .alertAccept()
            .debugLog('Alert is accepted');
    });
});

function drawRulerLine() {
    return this
        .setMapCenter([0, 0])
        .pause(UPDATE_TIMEOUT)
        .waitForVisible(pageObject.rulerIcon())
        .pointerClick(pageObject.rulerIcon())
        .waitForVisible(pageObject.nkButtonChecked() + ' ' + pageObject.rulerIcon())
        .pause(UPDATE_TIMEOUT)
        .debugLog('Starting to measure with ruler...')
        .pointerClick(350, 350)
        .pause(DRAW_TIMEOUT)
        .pointerClick(450, 350)
        .pause(DRAW_TIMEOUT)
        .pointerClick(450, 450)
        .pause(DRAW_TIMEOUT);
}
