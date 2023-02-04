const URL = require('url');
const querystring = require('querystring');

describe('control/cases/getPromoLink.html', function () {

    beforeEach(function () {
        return this.browser
            .openUrl('control/cases/getPromoLink.html')
            .waitReady();
    });
    it('Прокидывается центр и зум карты', function () {
        return this.browser
            .waitAndClick('ymaps=getLink', 5, 5)
            .waitForVisible('body #logger')
            .getText('body #logger').then(function (text) {
                checkQuery(text, {
                    z: '10',
                    ll: '37.64039050663398,55.7272577121364',
                    from: 'api-maps',
                    length: 5,
                    origin: true
                });
            })
            .verifyNoErrors();
    });
    it('После драга пробрасывается правильный центр', function () {
        return this.browser
            .pause(1000)
            .csDrag([100,100],[200,200])
            .pause(1000)
            .waitAndClick('ymaps=getLink')
            .waitForVisible('body #logger')
            .getText('body #logger').then(function (text) {
                checkQuery(text, {
                    z: '10',
                    ll: '37.50310050663398,55.8047077121364',
                    from: 'api-maps',
                    length: 5,
                    origin: true
                });
            })
            .verifyNoErrors();
    });
    it('После зума пробрасывается правильный зум', function () {
        return this.browser
            .click(PO.map.controls.zoom.plus())
            .pause(200)
            .waitAndClick('ymaps=getLink')
            .waitForVisible('body #logger')
            .getText('body #logger').then(function (text) {
                checkQuery(text, {
                    z: '11',
                    ll: '37.64039050663398,55.7272577121364',
                    from: 'api-maps',
                    length: 5,
                    origin: true
                });
            })
            .verifyNoErrors();
    });
    it('В урле есть старый маршрут', function () {
        return this.browser
            .waitForVisible(PO.map.controls.routeEditor())
            .pointerClick(24, 463)
            .pause(1500)
            .pointerClick(100,100)
            .pointerClick(200,100)
            .pause(1000)
            .waitAndClick('ymaps=getLink')
            .waitForVisible('body #logger')
            .getText('body #logger').then(function (text) {
                checkQuery(text, {
                    z: '10',
                    ll: '37.64039050663398,55.7272577121364',
                    from: 'api-maps',
                    rtm: 'atm',
                    rtext: '55.84796957146438,37.0279~55.84796957146438,37.1652',
                    length: 8,
                    origin: true
                });
            })
            .verifyNoErrors();
    });
    it('В урле обновляются координаты старого маршрут', function () {
        return this.browser
            .waitForVisible(PO.map.controls.routeEditor())
            .pointerClick(24, 463)
            .pause(500)
            .pointerClick(100, 100)
            .pause(500)
            .pointerClick(200, 100)
            .pause(1500)
            .csDrag([100,90], [100,190])
            .pause(3500)
            .waitAndClick('ymaps=getLink')
            .waitForVisible('body #logger')
            .getText('body #logger').then(function (text) {
                checkQuery(text, {
                    z: '10',
                    ll: '37.64039050663398,55.7272577121364',
                    from: 'api-maps',
                    rtm: 'atm',
                    rtext: '55.7706057146438,37.0279~55.84796957146438,37.1652',
                    length: 8,
                    origin: true
                });
            })
            .verifyNoErrors();
    });
    it('В урле нет одной точки старого маршрута', function () {
        return this.browser
            .waitForVisible(PO.map.controls.routeEditor())
            .pointerClick(24, 463)
            .pause(500)
            .pointerClick(100,100)
            .pause(500)
            .waitAndClick('ymaps=getLink')
            .waitForVisible('body #logger')
            .getText('body #logger').then(function (text) {
                checkQuery(text, {
                    z: '10',
                    ll: '37.64039050663398,55.7272577121364',
                    from: 'api-maps',
                    length: 5,
                    origin: true
                });
            })
            .verifyNoErrors();
    });
    it('В урле есть линейка', function () {
        return this.browser
            .waitAndClick(PO.map.controls.ruler())
            .pointerClick(100,100)
            .pointerClick(100,200)
            .pointerClick(100,300)
            .waitAndClick('ymaps=getLink')
            .waitForVisible('body #logger')
            .getText('body #logger').then(function (text) {
                checkQuery(text, {
                    z: '10',
                    ll: '37.64039050663398,55.7272577121364',
                    from: 'api-maps',
                    rl: '37.0279,55.84796957~0.00000000,-0.07733617~0.00000000,-0.07749057',
                    length: 6,
                    origin: true
                });
            })
            .verifyNoErrors();
    });
    it('В урле есть линейка и координаты обновляются', function () {
        return this.browser
            .waitAndClick(PO.map.controls.ruler())
            .pointerClick(100, 100)
            .pointerClick(100, 200)
            .pointerClick(100, 300)
            .pause(1500)
            .csDrag([100,200], [200,200])
            .pause(1500)
            .waitAndClick('ymaps=getLink')
            .waitForVisible('body #logger')
            .getText('body #logger').then(function (text) {
                checkQuery(text, {
                    z: '10',
                    from: 'api-maps',
                    rl: '37.0279,55.84796957~0.13733617,-0.07733617~-0.13733617,-0.07749057',
                    length: 6,
                    origin: true
                });
            })
            .verifyNoErrors();
    });
    it('В урле есть результат поиска', function () {
        return this.browser
            .waitAndClick(PO.map.controls.search.large.input())
            .setValue(PO.map.controls.search.large.input(), 'Москва')
            .pointerClick(PO.map.controls.search.large.button())
            .waitForVisible(PO.map.balloon.closeButton())
            .waitAndClick('ymaps=getLink')
            .waitForVisible('body #logger')
            .getText('body #logger').then(function (text) {
                checkQuery(text, {
                    z: '8',
                    ll: '37.38551050663398,55.5844',
                    from: 'api-maps',
                    ol: 'geo',
                    oll: '37.622504,55.753215',
                    text: 'Москва',
                    length: 8,
                    origin: true
                });
            })
            .verifyNoErrors();
    });
    it('В урле обновляется результат поиска', function () {
        return this.browser
            .waitAndClick(PO.map.controls.search.large.input())
            .setValue(PO.map.controls.search.large.input(), 'Москва')
            .pointerClick(PO.map.controls.search.large.button())
            .waitForVisible(PO.map.balloon.closeButton())
            .waitAndClick(PO.map.controls.search.large.clear())
            .waitForInvisible(PO.map.balloon.closeButton())
            .pause(500)
            .waitAndClick('ymaps=getLink')
            .waitForVisible('body #logger')
            .getText('body #logger').then(function (text) {
                checkQuery(text, {
                    z: '8',
                    ll: '37.38551050663398,55.5844',
                    from: 'api-maps',
                    length: 5,
                    origin: true
                });
            })
            .verifyNoErrors();
    });
    it('В урле есть результат поиска ППО', function () {
        return this.browser
            .waitAndClick(PO.map.controls.search.large.input())
            .setValue(PO.map.controls.search.large.input(), 'Москва')
            .pointerClick(PO.map.controls.search.large.button())
            .waitForVisible(PO.map.balloon.closeButton())
            .waitAndClick(PO.map.controls.search.large.clear())
            .waitForInvisible(PO.map.balloon.closeButton())
            .pause(500)
            .waitAndClick(PO.map.controls.search.large.input())
            .setValue(PO.map.controls.search.large.input(), 'Кафе')
            .pointerClick(PO.map.controls.search.large.button())
            .waitAndClick('ymaps=getLink')
            .waitForVisible('body #logger')
            .getText('body #logger').then(function (text) {
                checkQuery(text, {
                    z: '8',
                    from: 'api-maps',
                    text: 'Кафе',
                    length: 6,
                    origin: true
                });
            })
            .verifyNoErrors();
    });
    it('В урле есть результат поиска, он обновляется и удаляется', function () {
        return this.browser
            .waitAndClick(PO.map.controls.search.large.input())
            .setValue(PO.map.controls.search.large.input(), 'Москва')
            .pointerClick(PO.map.controls.search.large.button())
            .waitForVisible(PO.map.balloon.closeButton())
            .waitAndClick(PO.map.controls.search.large.clear())
            .waitForInvisible(PO.map.balloon.closeButton())
            .pause(500)
            .waitAndClick(PO.map.controls.search.large.input())
            .setValue(PO.map.controls.search.large.input(), 'Кафе')
            .pointerClick(PO.map.controls.search.large.button())
            .waitForVisible(PO.map.controls.search.large.serp.item())
            .pointerClick(445, 129)
            .pause(1000)
            .waitAndClick('ymaps=getLink')
            .waitForVisible('body #logger')
            .getText('body #logger').then(function (text) {
                checkQuery(text, {
                    from: 'api-maps',
                    text: 'Кафе',
                    ol: 'biz',
                    oid: '1101004152',
                    length: 8,
                    origin: true
                });
            })
            .verifyNoErrors();
    });
    it('В урле есть пробки', function () {
        return this.browser
            .waitAndClick(PO.map.controls.traffic())
            .pause(2000)
            .waitAndClick('ymaps=getLink')
            .waitForVisible('body #logger')
            .getText('body #logger').then(function (text) {
                checkQuery(text, {
                    z: '10',
                    ll: '37.64039050663398,55.7272577121364',
                    from: 'api-maps',
                    l: 'map,trf',
                    trfm: 'cur',
                    length: 6,
                    origin: true
                });
            })
            .verifyNoErrors();
    });
    it('В урле есть гибрид', function () {
        return this.browser
            .waitAndClick(PO.map.controls.listbox.typeSelectorIcon())
            .waitAndClick(PO.mapControlsListboxItem() + '=Гибрид')
            .pause(2000)
            .waitAndClick('ymaps=getLink')
            .waitForVisible('body #logger')
            .getText('body #logger').then(function (text) {
                checkQuery(text, {
                    z: '10',
                    ll: '37.64039050663398,55.7272577121364',
                    from: 'api-maps',
                    l: 'sat,skl',
                    length: 5,
                    origin: true
                });
            })
            .verifyNoErrors();
    });
    it('В урле есть одна метка и это построение маршрута', function () {
        return this.browser
            .waitAndClick('ymaps=addPlacemark')
            .pause(500)
            .waitAndClick('ymaps=getLink')
            .waitForVisible('body #logger')
            .getText('body #logger').then(function (text) {
                checkQuery(text, {
                    z: '10',
                    ll: '37.64039050663398,55.7272577121364',
                    from: 'api-maps',
                    rtext: '~55.750728,37.616329',
                    length: 6,
                    origin: true
                });
            })
            .verifyNoErrors();
    });
    it('В урле есть несколько меток', function () {
        return this.browser
            .waitAndClick('ymaps=addPlacemark')
            .waitAndClick('ymaps=addPlacemark')
            .pause(500)
            .waitAndClick('ymaps=getLink')
            .waitForVisible('body #logger')
            .getText('body #logger').then(function (text) {
                checkQuery(text, {
                    z: '10',
                    ll: '37.64039050663398,55.7272577121364',
                    from: 'api-maps',
                    pt: '37.616329,55.750728~37.616329,55.750728~37.616329,55.751728~37.616329,55.752728~37.616329,55.753728'
                    + '~37.616329,55.754728~37.616329,55.755728~37.616329,55.756728~37.616329,55.757728~37.616329,55.758728',
                    length: 6,
                    origin: true
                });
            })
            .verifyNoErrors();
    });
    it('В урле есть ссылка на мои карты', function () {
        return this.browser
            .waitAndClick('ymaps=myMaps')
            .pause(500)
            .waitAndClick('ymaps=getLink')
            .waitForVisible('body #logger')
            .getText('body #logger').then(function (text) {
                checkQuery(text, {
                    z: '10',
                    ll: '37.64039050663398,55.7272577121364',
                    from: 'api-maps',
                    um: 'mymaps:93jfWjoXws37exPmKH-OFIuj3IQduHal',
                    length: 6,
                    origin: true
                });
            })
            .verifyNoErrors();
    });
    it('Через контрол маршрутизации создаются авто', function () {
        return this.browser
            .waitAndClick('ymaps=Маршруты')
            .waitAndClick(PO.map.controls.routeButton.panel.svgB())
            .keys('Москва')
            .waitForVisible(PO.suggest.item0())
            .pointerClick(PO.suggest.item0())
            .waitForVisible(PO.routePoints.placemark())
            .pointerClick(200,200)
            .waitForVisible(PO.routePoints.pinA())
            .pause(1500)
            .waitAndClick('ymaps=getLink')
            .waitForVisible('body #logger')
            .getText('body #logger').then(function (text) {
                checkQuery(text, {
                    z: '11',
                    ll: '37.3965,55.7774',
                    from: 'api-maps',
                    length: 10,
                    rtt: 'auto',
                    rtm: 'dtr',
                    rtn: '0',
                    rtext: '55.77063339692481,37.16523181522771~55.753215,37.622504',
                    origin: true
                });
            })
            .verifyNoErrors();
    });
    it('Через контрол маршрутизации создаются от', function () {
        return this.browser
            .waitAndClick('ymaps=Маршруты')
            .waitAndClick(PO.map.controls.routeButton.panel.svgB())
            .keys('Москва')
            .waitForVisible(PO.suggest.item0())
            .pointerClick(PO.suggest.item0())
            .waitForVisible(PO.routePoints.placemark())
            .pointerClick(200, 200)
            .waitForVisible(PO.routePoints.pinA())
            .pause(1500)
            .waitAndClick(PO.map.controls.routeButton.panel.routeType.mass())
            .waitForVisible(PO.routePoints.transportPin())
            .pause(1500)
            .waitAndClick('ymaps=getLink')
            .waitForVisible('body #logger')
            .getText('body #logger').then(function (text) {
                checkQuery(text, {
                    z: '11',
                    ll: '37.3965,55.7774',
                    from: 'api-maps',
                    length: 10,
                    rtt: 'mt',
                    rtm: 'dtr',
                    rtn: '0',
                    rtext: '55.77063339692481,37.16523181522771~55.753215,37.622504',
                    origin: true
                });
            })
            .verifyNoErrors();
    });
    it('Через контрол маршрутизации создаются пешка', function () {
        return this.browser
            .waitAndClick('ymaps=Маршруты')
            .waitAndClick(PO.map.controls.routeButton.panel.svgB())
            .keys('Москва')
            .waitForVisible(PO.suggest.item0())
            .pointerClick(PO.suggest.item0())
            .waitForVisible(PO.routePoints.placemark())
            .pointerClick(200, 200)
            .waitForVisible(PO.routePoints.pinA())
            .pause(1500)
            .waitForVisible(PO.map.controls.routeButton.panel.routeType.pedestrian())
            .pause(500)
            .waitAndClick(PO.map.controls.routeButton.panel.routeType.pedestrian())
            .pause(1500)
            .waitAndClick('ymaps=getLink')
            .waitForVisible('body #logger')
            .getText('body #logger').then(function (text) {
                checkQuery(text, {
                    z: '11',
                    ll: '37.3965,55.7774',
                    from: 'api-maps',
                    length: 10,
                    rtt: 'pd',
                    rtm: 'dtr',
                    rtn: '0',
                    rtext: '55.77063339692481,37.16523181522771~55.753215,37.622504',
                    origin: true
                });
            })
            .verifyNoErrors();
    });

    it('После удаления маршрута с карты в инсепшн не передаётся ссылка на пустой маршрут', function () {
        return this.browser
            .waitAndClick('ymaps=Маршруты')
            .waitAndClick(PO.map.controls.routeButton.panel.svgB())
            .keys('Москва')
            .waitForVisible(PO.suggest.item0())
            .pointerClick(PO.suggest.item0())
            .waitForVisible(PO.routePoints.placemark())
            .pointerClick(200,200)
            .waitForVisible(PO.routePoints.pinA())
            .pause(500)
            .pointerClick(PO.map.controls.routeButton.panel.clear())
            .waitAndClick('ymaps=getLink')
            .waitForVisible('body #logger')
            .getText('body #logger').then(function (text) {
                checkQuery(text, {
                    z: '11',
                    ll: '37.3965,55.7774',
                    from: 'api-maps',
                    length: 5,
                    origin: true
                });
            })
            .verifyNoErrors();
    });
    it('Через контрол маршрутизации пробрасывается одна точка Б', function () {
        return this.browser
            .waitAndClick('ymaps=Маршруты')
            .waitAndClick(PO.map.controls.routeButton.panel.svgB())
            .keys('Москва')
            .waitForVisible(PO.suggest.item0())
            .pointerClick(PO.suggest.item0())
            .waitForVisible(PO.routePoints.placemark())
            .pause(500)
            .waitAndClick('ymaps=getLink')
            .waitForVisible('body #logger')
            .getText('body #logger').then(function (text) {
                checkQuery(text, {
                    z: '10',
                    ll: '37.6404050663398,55.7273077121364',
                    from: 'api-maps',
                    length: 9,
                    rtt: 'auto',
                    rtm: 'dtr',
                    rtext: '~55.753215,37.622504',
                    origin: true
                });
            })
            .verifyNoErrors();
    });
    it('Через контрол маршрутизации пробрасывается одна точка А', function () {
        return this.browser
            .waitAndClick('ymaps=Маршруты')
            .waitForVisible(PO.map.controls.routeButton.panel.svgB())
            .keys('Москва')
            .waitForVisible(PO.suggest.item0())
            .pointerClick(PO.suggest.item0())
            .waitForVisible(PO.routePoints.placemark())
            .pause(500)
            .waitAndClick('ymaps=getLink')
            .waitForVisible('body #logger')
            .getText('body #logger').then(function (text) {
                checkQuery(text, {
                    z: '10',
                    ll: '37.6404050663398,55.7273077121364',
                    from: 'api-maps',
                    length: 9,
                    rtt: 'auto',
                    rtm: 'dtr',
                    rtext: '55.753215,37.622504~',
                    origin: true
                });
            })
            .verifyNoErrors();
    });
    it('Пробрасываются результаты ближайшие к центру', function () {
        return this.browser
            .waitAndClick('ymaps=addPlacemark')
            .waitAndClick('ymaps=addPlacemark')
            .waitAndClick('ymaps=poi')
            .waitAndClick('ymaps=poi')
            .click(PO.map.controls.zoom.minus())
            .pause(500)
            .waitAndClick('ymaps=getLink')
            .waitForVisible('body #logger')
            .getText('body #logger').then(function (text) {
                checkQuery(text, {
                    z: '17',
                    ll: '47.61632899999995,55.75672799999371',
                    from: 'api-maps',
                    pt: '47.616329,55.756728~47.616329,55.755728~47.616329,55.757728~47.616329,55.754728~47.616329,'
                    + '55.758728~47.616329,55.753728~47.616329,55.759728~47.616329,55.752728~47.616329,55.760728~47.616329,55.751728',
                    length: 6,
                    origin: true
                });
            })
            .verifyNoErrors();
    });

    function checkQuery(text, result){
        var url = new URL.parse(text);
        var query = querystring.parse(url.search.substring(1));
        var version = 'jsapi_2_1_';

        for (var elem in result){
            // Проверим, что количество полей совпадает.
            switch(elem){
                case 'length':
                    Object.keys(query).length.should.equal(result.length);
                    break;
                case 'll':
                    checkCoords(query[elem].split(','), result[elem].split(','));
                    break;
                case 'rtext':
                case 'rl':
                case 'oll':
                case 'pt':
                case 'poi[point]':
                    var coordsArray = query[elem].split('~');
                    var resultArray = result[elem].split('~');
                    for (var name in coordsArray){
                        checkCoords(coordsArray[name].split(','), resultArray[name].split(','));
                    }
                    break;
                case 'oid':
                    isNaN(query[elem]).should.equal(false);
                    break;
                case 'origin':
                    query[elem].substring(0, query[elem].length - 2).should.equal(version);
                    break;
                default:
                    query[elem].should.equal(result[elem]);
                    break;
            }
        }
    }
    function checkCoords(coords, result){
        Number(coords[0]).toFixed(2).should.equal(Number(result[0]).toFixed(2));
        Number(coords[1]).toFixed(2).should.equal(Number(result[1]).toFixed(2));
    }
});
