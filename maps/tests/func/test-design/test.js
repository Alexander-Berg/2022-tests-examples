const cases = require('./cases.js');

describe('Map design', () => {
    for (const set in cases) {
        describe(`Set - ${set}`, () => {
            for (const i in cases[set]) {
                const cs = cases[set][i];
                describe(`${cs.name}`, () => {
                    for (let z = cs.zmin; z <= cs.zmax; z++) {
                        it(`zoom : ${z}`, async ({browser}) => {
                            await browser.openMap({center: cs.center, zoom: z});
                            await browser.pause(1000);
                            await browser.verifyScreenshot(`${set}-${cs.name.toLowerCase().split(' ').join('_')}-z-${z}`, PO.map());
                        });
                    }
                });
            }
        });
    }
});
