const coords = [39.903960, 116.391289];

describe(`Beijing ${coords}`, () => {
    for (let i = 10; i < 20; i++) {
        it(`zoom : ${i}`, async ({browser}) => {
            await browser.openMap({center: coords, zoom: i});
            await browser.verifyScreenshot('zoom-' + i, PO.map());
        });
    }
});
