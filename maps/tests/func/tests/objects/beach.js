describe('Objects -beach', () => {
    for (let i = 12; i < 20; i++) {
        it(`zoom : ${i}`, async ({browser}) => {
            await browser.openMap({center: [27.217726895516666, 33.84157358590984], zoom: i});
            await browser.verifyScreenshot('zoom-' + i, PO.map());
        });
    }
});
