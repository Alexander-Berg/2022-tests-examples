const coords = [32.077979, 34.774133];

describe(`Tel Aviv ${coords}`, () => {
    for (let i = 10; i < 20; i++) {
        it(`zoom : ${i}`, async ({browser}) => {
            await browser.openMap({center: coords, zoom: i});
            await browser.verifyScreenshot('zoom-' + i, PO.map());
        });
    }
});
