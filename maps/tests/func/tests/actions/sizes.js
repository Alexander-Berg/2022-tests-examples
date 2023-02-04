const sizes = [[100, 100], [300, 300], [500, 500], [500, 1500], [1000, 1000], [1500, 1500], [1800, 1800], [2200, 2200]];
const center = [55.753930, 37.621401];
const zoom = 16;

describe('Sizes', () => {
    for (let i = 0; i < sizes.length; i++) {
        hermione.enable.in(['chrome_50', 'chrome_53', 'yandex']);
        it(`${sizes[i][0]}x${sizes[i][1]}`, async ({browser}) => {
            await browser.setWindowSize(sizes[i][0], sizes[i][1]);
            await browser.openMap({filename: 'full-viewport', center, zoom, controls: ['default']});
            await browser.waitForTilesLoaded();
            await browser.verifyScreenshot(`${sizes[i][0]}x${sizes[i][1]}`, PO.map());
        });
    }
});
