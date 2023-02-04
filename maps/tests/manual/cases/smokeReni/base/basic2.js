function init(ym) {

    var map = new ym.Map('map', {
        center: [55.80, 37.64],
        zoom: 15,
        behaviors: ['default', 'scrollZoom'],
        controls: []
    }, {
        backgroundVisible: false,
        yandexMapDisablePoiInteractivity: true
    });

    var myPlacemark1 = new ymaps.Placemark([55.80, 37.64], {iconCaption: '<b>fdsafasd</b>fasfasdasd', iconContent: 'Очень длиннный, но невероятно интересный текс.'}, {preset: 'islands#sportCircleIcon', hasBalloon: false});

    map.geoObjects
        .add(myPlacemark1);
}