const coords = [53.9126118675139, 108.52991104943004];

describe('Water borders: Baikal', () => {
    for (let i = 6; i < 20; i++) {
        it(`zoom : ${i}`, async ({browser}) => {
            await browser.openMap({center: coords, zoom: i});
            await browser.verifyScreenshot('zoom-' + i, PO.map());
        });
    }
});
