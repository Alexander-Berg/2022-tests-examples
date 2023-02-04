describe.skip('Layers -carparks', () => {
    for (let i = 5; i < 19; i++) {
        it(`zoom : ${i}`, async ({browser}) => {
            await browser.openMap({center: [55.753930, 37.621401], zoom: i});
            await browser.execute(addCarparksControl).catch((err) => new Error(err));
            await browser.click(PO.map.controls.button1());
            await browser.waitForTilesLoaded();
            await browser.verifyScreenshot('carparks-selected-zoom-' + i, PO.map());
            await browser.click(PO.map.controls.button1());
            await browser.waitForTilesLoaded();
            await browser.verifyScreenshot('carparks-deselected-zoom-' + i, PO.map());
        });
    }

    function addCarparksControl() {
        return ymaps.modules.require(['carParks.Provider']).then((modules) => {
            const CarParksProvider = modules[0];
            var carParkProvider = new CarParksProvider();

            const carparks = new ymaps.control.Button({
                data: {
                    content: 'carparks'
                },
                options: {
                    selectOnClick: true,
                    maxWidth: [1000]
                }
            });
            carparks.events
                .add('select', () => {
                    carParkProvider.setMap(myMap);
                })
                .add('deselect', () => {
                    carParkProvider.setMap(null);
                });

            return myMap.controls.add(carparks);
        });
    }
});
