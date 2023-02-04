const zoom = 9;
const {WAIT_FOR_UPDATE} = require('../../constants');

describe('Actions -autopan-route', () => {
    it('autopan-route', async ({browser}) => {
        await browser.openMap({center: [60.3, 30.3], zoom: zoom});
        await browser.execute(addRoute).catch((err) => new Error(err));
        await browser.verifyNoErrors();
        await browser.waitForTilesLoaded();
        await browser.pause(WAIT_FOR_UPDATE);
        await browser.verifyScreenshot('autopan-route', PO.map());
    });

    function addRoute() {
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
                routingMode: 'masstransit'
            }
        }, {
            boundsAutoApply: true
        });

        myMap.geoObjects.add(new ymaps.Placemark([55.752972, 37.600642], {}, {
            preset: 'islands#icon',
            iconColor: '#735184'
        }));

        myMap.geoObjects.add(multiRoute);
        multiRoute.model.events.once('requestsuccess', () => {
            multiRoute.getRoutes().get(0).balloon.open();
        });

        return true;
    }
});
