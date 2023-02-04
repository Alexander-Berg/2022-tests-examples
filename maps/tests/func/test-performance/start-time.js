const coords = [55.75578054467244, 37.61863065792959];

describe('Test start time', () => {
    for (let j = 0; j < 5; j++) {
        for (let i = 0; i < 20; i++) {
            it(`${j} ${i}`, async ({browser}) => {
                await browser.openMapPerf({filename: 'full-viewport', center: coords, zoom: 15});
            });
        }
    }
});
