function iconShadowTests (Y) {return {
    testIconShadow: function () {
        ymaps.layout.storage.add(
            '#icon-layout1',
            ymaps.templateLayoutFactory.createClass(
                '<div class="icon-shadow" id="shadow-id" style="position:relative;' +
                        'top:-30px;left:-30px;width:30px;height:30px;' +
                        'background:rgba(0,0,0,0.5);font-size:8px;">' +
                        '$[options.iconShadowHref]' +
                '</div>'
            )
        );

        ymaps.layout.storage.add(
            '#icon-layout2',
            ymaps.templateLayoutFactory.createClass(
                '<div class="icon-shadow" id="shadow-id" style="position:relative;' +
                        'top:-30px;left:-30px;width:30px;height:30px;' +
                        'background:rgba(255,0,0,0.5);font-size:8px;">' +
                        '$[options.iconShadowHref]' +
                '</div>'
            )
        );

//        collection = new ymaps.GeoObjectCollection({}, {
//            geoObjectCursor: "pointer"
//        }),
        var geoObject = new ymaps.Placemark({
                type: "Point",
                coordinates: [10, 40]
            }, {
                iconContent: 'S'
            }, {
                preset: 'twirl#greenIcon',
                zIndex: 10,
                iconShadow: true,
                iconShadowLayout: '#icon-layout1',
                iconShadowHref: 'Shadow'
            });

//        collection.add(geoObject);

//        var i = 0,
//            interval = setInterval(function () {
//               geoObject.options.set('iconShadowLayout', '#icon-layout' + (i++%2 + 1));
//            }, 200);

        myMap.geoObjects.add(geoObject);
        shadowed = geoObject;

//        setTimeout(function () {
//            geoObject.options.set('geoObjectHasIconShadow', false);
//            setTimeout(function () {
//                geoObject.options.set('geoObjectHasIconShadow', true);
//            }, 1000);
//        }, 100);
//
//        eventEye.observe(geoObject, 'optionschange');
//
//        setTimeout(function () {
//            clearInterval(interval);
//            interval = null;
//        }, 3000);
//
//        Y.assert(
//            geoObject.options.get('geoObjectHasIconShadow'),
//            'Неверный результат getOptions'
//        );
//        Y.assert(
//            !!document.getElementById('shadow-id'),
//            'Тень иконки не найдена'
//        );
    }
};}
