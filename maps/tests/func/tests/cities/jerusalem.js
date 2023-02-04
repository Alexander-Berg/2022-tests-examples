const coords = [31.776800, 35.205648];

describe(`Jerusalem ${coords}`, () => {
    for (let i = 10; i < 20; i++) {
        it(`zoom : ${i}`, async ({browser}) => {
            await browser.openMap({center: coords, zoom: i});
            await browser.verifyScreenshot('zoom-' + i, PO.map());
        });
    }
});
