describe('Objects -swamp', () => {
    for (let i = 12; i < 19.5; i += 0.5) {
        it(`zoom : ${i}`, async ({browser}) => {
            await browser.openMap({center: [53.050037, 25.368790], zoom: i});
            await browser.verifyScreenshot('zoom-' + i, PO.map());
        });
    }
});
