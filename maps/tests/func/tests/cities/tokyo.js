const coords = [35.682418, 139.753146];

describe(`Tokyo ${coords}`, () => {
    for (let i = 10; i < 20; i++) {
        it(`zoom : ${i}`, async ({browser}) => {
            await browser.openMap({center: coords, zoom: i});
            await browser.verifyScreenshot('zoom-' + i, PO.map());
        });
    }
});
