describe('Objects -tram', () => {
    for (let i = 16; i < 20; i++) {
        it(`zoom : ${i}`, async ({browser}) => {
            await browser.openMap({center: [59.961503897584514, 30.40757594115414], zoom: i});
            await browser.verifyScreenshot('zoom-' + i, PO.map());
        });
    }
});
