const coords = [41.01061954269186, 28.968390664083405];

describe(`Istanbul ${coords}`, () => {
    for (let i = 7; i < 20; i++) {
        it(`zoom : ${i}`, async ({browser}) => {
            await browser.openMap({center: coords, zoom: i});
            await browser.verifyScreenshot('zoom-' + i, PO.map());
        });
    }
});
