ymaps.modules.define(util.testfile(), [
    "router.Service"
], function (provide, RouterService) {
    describe("router.Service", function () {
        it.skip("Проверка наличия AddressDetail в метаданных путевых точек", function () {
            var service = new RouterService();
            return service.route([
                "Москва, Ленинский проспект",
                "Москва, Льва Толстого, 16"
            ], {}).then(function (geoJson) {
                expect(geoJson.features[0].properties.GeocoderMetaData.AddressDetails).to.be.ok();
            });
        });
    });

    provide();
});
