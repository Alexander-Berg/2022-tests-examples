const coords = [55.75578054467244, 37.61863065792959];

describe(`Moscow ${coords}`, () => {
    for (let i = 0; i < 20; i++) {
        it(`zoom : ${i}`, async ({browser}) => {
            await browser.openMap({center: coords, zoom: i});
            await browser.verifyScreenshot('zoom-' + i, PO.map());
        });
    }
});
