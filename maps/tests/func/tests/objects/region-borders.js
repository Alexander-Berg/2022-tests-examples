describe('Objects -region\'s borders', () => {
    for (let i = 5; i < 9; i += 0.5) {
        it(`zoom : ${i}`, async ({browser}) => {
            await browser.openMap({center: [58.97315984446507, 30.645461491931144], zoom: i});
            await browser.verifyScreenshot('zoom-' + i, PO.map());
        });
    }
});
