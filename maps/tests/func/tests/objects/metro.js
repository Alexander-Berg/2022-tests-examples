describe('Objects -metro Ulitsa Dmitriyevskogo', () => {
    for (let i = 12; i < 20; i++) {
        it(`zoom : ${i}`, async ({browser}) => {
            await browser.openMap({center: [55.710311, 37.879045], zoom: i});
            await browser.verifyScreenshot('zoom-' + i, PO.map());
        });
    }
});
