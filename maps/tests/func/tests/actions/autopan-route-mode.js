const zoom = 19;
const {WAIT_FOR_UPDATE} = require('../../constants');

describe('Actions -autopan-route-modes', () => {
    it('autopan-route', async ({browser}) => {
        await browser.openMap({center: [60.3, 30.3], zoom: zoom});
        await browser.execute(addRouteControls);
        await browser.verifyNoErrors();
        await browser.pause(WAIT_FOR_UPDATE);
        await browser.waitForTilesLoaded();
        await browser.verifyScreenshot('autopan-route-auto', PO.map());
        await browser.deleteTilesLoaded();
        await browser.execute(() => myMap.setZoom(19));
        await browser.waitForTilesLoaded();
        await browser.pause(WAIT_FOR_UPDATE);
        await browser.deleteTilesLoaded();
        await browser.click(PO.map.controls.button1());
        await browser.waitForTilesLoaded();
        await browser.pause(WAIT_FOR_UPDATE);
        await browser.verifyScreenshot('autopan-route-bicycle', PO.map());
        await browser.deleteTilesLoaded();
        await browser.execute(() => myMap.setZoom(19));
        await browser.waitForTilesLoaded();
        await browser.pause(WAIT_FOR_UPDATE);
        await browser.click(PO.map.controls.button2());
        await browser.waitForTilesLoaded();
        await browser.pause(WAIT_FOR_UPDATE);
        await browser.verifyScreenshot('autopan-route-pedestrian', PO.map());
        await browser.deleteTilesLoaded();
        await browser.click(PO.map.controls.button3());
        await browser.execute(() => myMap.setZoom(19));
        await browser.waitForTilesLoaded();
        await browser.pause(WAIT_FOR_UPDATE);
        await browser.verifyScreenshot('autopan-route-masstransit', PO.map());
        await browser.verifyNoErrors();
    });

    function addRouteControls() {
        const modes = ['masstransit', 'pedestrian', 'bicycle'];

        const multiRoute = new ymaps.multiRouter.MultiRoute({
            referencePoints: [
                'метро Киевская',
                'метро Охотный ряд',
                'метро Площадь Революции',
                'Москва, улица Льва Толстого'
            ],
            params: {
                viaIndexes: [1],
                results: 2,
                routingMode: 'auto'
            }
        }, {
            boundsAutoApply: true
        });

        myMap.geoObjects.add(multiRoute);
        multiRoute.model.events.once('requestsuccess', () => multiRoute.getRoutes().get(0).balloon.open());

        modes.forEach((mode) => {
            const button = new ymaps.control.Button({
                data: {
                    content: mode
                },
                options: {
                    maxWidth: 150,
                    selectOnClick: false
                }
            });

            button.events.add('click', () => {
                multiRoute.model.setParams({routingMode: mode}, true);
                multiRoute.model.events.once('requestsuccess', () =>
                    multiRoute.getMap().setBounds(multiRoute.getActiveRoute().getBounds())
                );
            });

            myMap.controls.add(button);
        });

        myMap.geoObjects.add(new ymaps.Placemark([55.752972, 37.600642], {}, {
            preset: 'islands#icon',
            iconColor: '#735184'
        }));

        return true;
    }
});
