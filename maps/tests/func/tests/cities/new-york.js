const coords = [40.716012, -74.002853];

describe(`Nyc ${coords}`, () => {
    for (let i = 0; i < 20; i++) {
        it(`zoom : ${i}`, async ({browser}) => {
            await browser.openMap({center: coords, zoom: i});
            await browser.verifyScreenshot('zoom-' + i, PO.map());
        });
    }
});
