const coords = [35.661703, 51.367734];

describe(`Tehran ${coords}`, () => {
    for (let i = 10; i < 20; i++) {
        it(`zoom : ${i}`, async ({browser}) => {
            await browser.openMap({center: coords, zoom: i});
            await browser.verifyScreenshot('zoom-' + i, PO.map());
        });
    }
});
