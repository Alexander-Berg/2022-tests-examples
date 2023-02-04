const zoom = 17;
const {WAIT_FOR_UPDATE} = require('../../constants');
const cases = [
    {name: 'Охотный ряд', coords: [55.75735630854855, 37.61503342587282]},
    {name: 'Павелецкий', coords: [55.72725771214265, 37.640390506634006]},
    {name: 'Кастаневская', coords: [55.734608, 37.473742]}
];

describe('Wrong tiles\' type', () => {
    for (let i = 0; i < cases.length; i++) {
        it(cases[i].name, async ({browser}) => {
            await browser.openMap({center: cases[i].coords, zoom: zoom, controls: ['default']});
            await browser.verifyScreenshot(i + '-zoom-' + zoom, PO.map());
            await browser.waitForVisible(PO.map.controls.zoom.minus());
            await browser.click(PO.map.controls.zoom.minus());
            await browser.pause(WAIT_FOR_UPDATE);
            await browser.click(PO.map.controls.zoom.minus());
            await browser.pause(WAIT_FOR_UPDATE);
            await browser.click(PO.map.controls.zoom.minus());
            await browser.pause(WAIT_FOR_UPDATE);
            await browser.click(PO.map.controls.zoom.minus());
            await browser.pause(WAIT_FOR_UPDATE);
            await browser.waitForTilesLoaded();
            await browser.verifyNoErrors();
            await browser.verifyScreenshot(i + '-zoom-' + (zoom - 4), PO.map());
        });
    }
});
