ymaps.modules.define(util.testfile(), [
    "multiRouter.service.yMapsJsonToGeoJson"
], function (provide, yMapsJsonToGeoJson) {
    describe("multiRouter.service.yMapsJsonToGeoJson", function () {
        it("Парсинг стороннего json", function (done) {
            var json = {"type":"FeatureCollection","properties":{"RouterMetaData":{"Waypoints":[{"address":"","name":"парк имени 50-летия Октября","coordinates":[37.50814,55.679656]},{"address":"","name":"улица Удальцова, 57","coordinates":[37.503376,55.676965]}]}},"features":[{"type":"FeatureCollection","properties":{"RouteMetaData":{"type":"masstransit","Duration":{"value":507,"text":"8 мин"}},"boundedBy":[[37.503376,55.676965],[37.50814,55.679656]]},"features":[{"type":"FeatureCollection","properties":{"PathMetaData":{"Duration":{"value":507,"text":"8 мин"},"Distance":{"value":423,"text":"423 м"}},"encodedCoordinates":"LFQ8AqiaUQNk7f__ffX__w=="},"features":[{"type":"FeatureCollection","properties":{"SegmentMetaData":{"Walk":true,"Duration":{"value":494,"text":"8 мин"},"Distance":{"value":423,"text":"423 м"}}},"features":[{"type":"Feature","properties":null,"geometry":{"type":"LineString","lodIndex":0}}]}]}]}]};

            var geoJson = yMapsJsonToGeoJson(json);

            expect(geoJson).to.be.ok();

            done();
        });
    });

    provide();
});
