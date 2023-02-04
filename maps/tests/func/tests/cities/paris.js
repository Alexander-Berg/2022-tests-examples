const coords = [48.85743488717354, 2.3492902078312827];

describe(`Paris ${coords}`, () => {
    for (let i = 3; i < 20; i++) {
        it(`zoom : ${i}`, async ({browser}) => {
            await browser.openMap({center: coords, zoom: i});
            await browser.verifyScreenshot('zoom-' + i, PO.map());
        });
    }
});
