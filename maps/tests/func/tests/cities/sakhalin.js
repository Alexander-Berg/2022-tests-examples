const coords = [50.1508303496465, 142.75079699999995];

describe(`Sakhalin ${coords}`, () => {
    for (let i = 3; i < 20; i++) {
        it(`zoom : ${i}`, async ({browser}) => {
            await browser.openMap({center: coords, zoom: i});
            await browser.verifyScreenshot('zoom-' + i, PO.map());
        });
    }
});
