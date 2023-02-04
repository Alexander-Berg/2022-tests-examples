var myMap;

var dataPoints = Array();
var markerPoints = Array();
var dataPointsLength = 0;

var placemark = null;
var BalloonContentLayout;
var MyBalloonLayout;

function initYandexMap(ymaps) {
    window.ymaps = ymaps;
    myMap = new ymaps.Map('map_section', { center: [56.83348413075511, 60.59290945529938], zoom: 11, controls: ['zoomControl'], type: 'yandex#satellite' });

    myMap.behaviors.disable('scrollZoom');
    myMap.behaviors.disable('dblClickZoom');

    var wB = 520;
    var mHB = 145;

    if (mobileVersion) {
        wB = 280;
        mHB = 345;
    }

    //console.log(mHB);

    MyBalloonLayout = ymaps.templateLayoutFactory.createClass(
            '<div class="popover top">' +
            '<a class="close" href="#">&times;</a>' +
            '<div class="arrow"></div>' +
            '<div class="popover-inner">' +
            '$[[options.contentLayout observeSize minWidth=' + wB + ' maxWidth=' + wB + ' maxHeight=' + mHB + ']]' +
            '</div>' +
            '</div>', {

            build: function () {

                this.constructor.superclass.build.call(this);
                this._$element = $('.popover', this.getParentElement());

                this.applyElementOffset();
                this._$element.find('.close').on('click', $.proxy(this.onCloseClick, this));
            },

            clear: function () {
                this._$element.find('.close').off('click');
                this.constructor.superclass.clear.call(this);
            },

            onSublayoutSizeChange: function () {
                MyBalloonLayout.superclass.onSublayoutSizeChange.apply(this, arguments);
                if (!this._isElement(this._$element)) {
                    return;
                }
                this.applyElementOffset();
                this.events.fire('shapechange');
            },

            onCloseClick: function (e) {
                e.preventDefault();
                this.events.fire('userclose');
            },


            applyElementOffset: function () {
                this._$element.css({
                    left: -(this._$element[0].offsetWidth / 2),
                    top: -(this._$element[0].offsetHeight + this._$element.find('.arrow')[0].offsetHeight)
                });
            },

            getShape: function () {
                if (!this._isElement(this._$element)) {
                    return MyBalloonLayout.superclass.getShape.call(this);
                }
                var position = this._$element.position();
                return new ymaps.shape.Rectangle(new ymaps.geometry.pixel.Rectangle([
                    [position.left, position.top],
                    [
                            position.left + this._$element[0].offsetWidth,
                            position.top + this._$element[0].offsetHeight + this._$element.find('.arrow')[0].offsetHeight
                    ]
                ]));
            },

            _isElement: function (element) {
                return element && element[0] && element.find('.arrow')[0];
            }
        });

    BalloonContentLayout = ymaps.templateLayoutFactory.createClass(
            '<div class="ds_mapCont">' +
            '<span>$[properties.companyName]</span>' +
            '<div class="ds_mapCol"><div class="ds_padR15">' +
            'Адрес: <br/>' +
            '$[properties.companyAdress]' +
            '</div></div>' +
            '<div class="ds_mapCol"><div class="">' +
            '[if properties.phone]Тел: $[properties.phone]<br />[endif]' +
            '[if properties.fax]Факс: $[properties.fax]<br />[endif]' +
            '[if properties.email]E-mail: $[properties.email]<br />[endif]' +
            '[if properties.site]Сайт: $[properties.site]<br />[endif]' +
            '</div></div><div style="clear:left"></div>' +
            '</div>'
    );


    // не зависимо от того, сколько маркетов я добавлю через функцию InitMarkers: 2 или все,
    // через 10 секунд при попытке myMap.setCenter на мобильном карта подвиснет (возм начнет отвечать на жестны через несколько минут) (nexus 5, chrome last)
    // на компе все норм. если поставить масштаб не 17 а 12, то не зависнет
    InitMarkers();

    var btn = new ymaps.control.Button('setCenter');
    btn.events.add('click', function () {
        // console.profile('1');
        console.log('вешаем карту');
        myMap.setCenter([44.89979657743778, 37.347005903720856], 17, { checkZoomRange: true });
        // console.profileEnd('1');
    });
    myMap.controls.add(btn);

    /*
     setTimeout(function() {
     console.log('вешаем карту');
     myMap.setCenter([44.89979657743778, 37.347005903720856], 17, { checkZoomRange: true }); }
     , 10000);
     */
    //SortAndCreateSubMenu();


}

var tmpPlaceData;
var mainCity = Array();

function ArrSort(a, b) {
    if (a.name > b.name) return 1;
    if (a.name < b.name) return -1;
}

function MainLinkClick(_this) {

    var nom = $(_this).attr('sp_id');
    console.log('MainLinkClick(), MoveToPlace: ' + nom);
    MoveToPlace(nom);
    $('#id-map-tab li').removeClass('selected');
    $('#id-sub-map li').removeClass('selected');
    $(_this).parent().addClass('selected');

    if ($('#id-sub-map').is(':visible'))
        OpenSubMenu();
}

function SubLinkClick(_this) {

    var nom = $(_this).attr('sp_sub_id');

    $('#id-map-tab li').removeClass('selected');
    $('#id-sub-map li').removeClass('selected');
    $(_this).parent().addClass('selected');
    $('#id-map-tab li').first().remove();
    $('#id-map-tab').prepend('<li class="selected"><a href="javascript:void(0);" sp_id="' + nom + '" onclick="MainLinkClick(this);">' + $(_this).text() + '</a></li>');

    MoveToPlace(nom);

    setTimeout(function () {
        OpenSubMenu();
    }, 200);
}

function SortAndCreateSubMenu() {
    //tmpPlaceData = dataPoints;
    tmpPlaceData = JSON.parse(JSON.stringify(dataPoints))
    tmpPlaceData.sort(ArrSort);

    for (var ax = 0; ax < tmpPlaceData.length; ax++) {
        if (tmpPlaceData[ax].name == 'Москва')
            mainCity[0] = tmpPlaceData[ax].nom;

        if (tmpPlaceData[ax].name == 'Санкт-Петербург')
            mainCity[1] = tmpPlaceData[ax].nom;

        if (tmpPlaceData[ax].name == 'Екатеринбург')
            mainCity[2] = tmpPlaceData[ax].nom;

        $('#id-sub-map').append('<li><a href="javascript:void(0)" class="ds_subLnk" sp_sub_id="' + tmpPlaceData[ax].nom + '" onclick="SubLinkClick(this);">' + tmpPlaceData[ax].name + '</a></li>')
        if (mobileVersion)
            $('#id-sub-map').addClass('ds_2colMapCity');
    }

    $('#id-map-tab').append('<li class="selected"><a href="javascript:void(0);" sp_id="' + dataPoints[mainCity[0]].nom + '" onclick="MainLinkClick(this);">' + dataPoints[mainCity[0]].name + '</a></li>');
    $('#id-map-tab').append('<li><a href="javascript:void(0);"                  sp_id="' + dataPoints[mainCity[1]].nom + '" onclick="MainLinkClick(this);">' + dataPoints[mainCity[1]].name + '</a></li>');
    $('#id-map-tab').append('<li><a href="javascript:void(0);"                  sp_id="' + dataPoints[mainCity[2]].nom + '" onclick="MainLinkClick(this);">' + dataPoints[mainCity[2]].name + '</a></li>');

    $('#id-map-tab').append('<li class="ds_arDown" id="id_more_city"><a href="javascript:void(0);">Еще &nbsp;</a></li>');

    $('#id_more_city').on('click', OpenSubMenu);
}


function OpenSubMenu() {
    if ($('#id-sub-map').is(':visible')) {
        $('#id-sub-map').stop().slideUp();
        $(this).removeClass('ds_arDown').addClass('ds_arUp');
    }
    else {
        $('#id-sub-map').stop().slideDown();
        $(this).removeClass('ds_arUp').addClass('ds_arDown');
    }
}


var dataPlacemarks = {};
function SetNewPoint(nom) {
    //var placemark;

    if (typeof(markerPoints[nom]) != 'undefined') {
        dataPlacemarks['o_' + nom] = new ymaps.Placemark(
            [ markerPoints[nom].coord0, markerPoints[nom].coord1 ],
            {
                id: nom,
                companyName: markerPoints[nom].compName,
                companyAdress: markerPoints[nom].companyAdress,
                phone: markerPoints[nom].phone,
                fax: markerPoints[nom].fax,
                email: markerPoints[nom].email,
                site: markerPoints[nom].site
            },
            {

                /*balloonPanelMaxMapArea: 0,*/
                balloonLayout: MyBalloonLayout,
                balloonContentLayout: BalloonContentLayout,
                balloonCloseButton: true,
                hideIconOnBalloonOpen: false,
                balloonOffset: [3, -40],

                iconLayout: 'default#image',
                iconImageHref: 'http://barausse.archidev.ru/bitrix/templates/barausse/images/dump.png',
                iconImageSize: [43, 43],
                iconImageOffset: [-19, -43],
                //syncOverlayInit: true
            }
        );

        myMap.geoObjects.add(dataPlacemarks['o_' + nom]);
    }
}

// перемещение к городу (координаты первого магазина, если в городе nom магазин только 1, то открываем его (MoveToShop)
function MoveToPlace(nom) {
    if (typeof(dataPoints[nom]) != 'undefined') {
        if (dataPoints[nom].cityCount == 1) {
            MoveToShop(dataPoints[nom].markerID);
            return;
        }
        myMap.setCenter([dataPoints[nom].coord0, dataPoints[nom].coord1], 12);
    }
}

// перемещение к магазину (метка на карте)
function MoveToShop(nom) {
    if (typeof(markerPoints[nom]) != 'undefined') {
        if (typeof(dataPlacemarks['o_' + nom]) != 'undefined') {
            dataPlacemarks['o_' + nom].balloon.open();
            myMap.setCenter([markerPoints[nom].coord0, markerPoints[nom].coord1], 17, { checkZoomRange: true });
        }
    }
}


function AddDataPoint(a, b, c, d, e, f, g, h, i, j) {
    if (h == '<a href=\'mailto:\'></a>')
        h = '';
    if (i == '<a href=\'http://barausse.com/portal/ru/russia/where-to-buy/store-detail/\' target=\'_blank\'>barausse.com</a>')
        i = '<a href=\'http://barausse.com/portal/ru/russia/where-to-buy/\' target=\'_blank\'>barausse.com</a>';

    markerPoints[a] = {name: a, coord0: b, coord1: c, compName: d, companyAdress: e, phone: f, fax: g, email: h, site: i, city: j};
    var needAdd = true;

    for (var ax = 0; ax < dataPoints.length; ax++) {
        if (dataPoints[ax].name == j) {
            needAdd = false;
            dataPoints[ax].cityCount++;
            break;
        }
    }
    if (needAdd) {
        AddPlacePoint(j, b, c, a);
    }
    SetNewPoint(a);

    console.log(a);
}

function AddPlacePoint(a, b, c, d) {
    dataPoints.push({name: a, coord0: b, coord1: c, markerID: d, nom: dataPoints.length, cityCount: 1});
}

function InitMarkers() {

    AddDataPoint(0, 56.83348413075511, 60.59290945529938, "BARAUSSE ЕКАТЕРИНБУРГ НА МАЛЫШЕВА", "г. Екатеринбург ул. Малышева, 23 стильный дом &quot;ДВЕРСАЧЕ&quot;", "8 (343) 376-40-02", "8 (343) 376-40-03", "<a href='mailto:makorova_marina@inbox.ru'>makorova_marina@inbox.ru</a>", "<a href='http://barausse.com/portal/ru/russia/where-to-buy/' target='_blank'>barausse.com</a>", "Екатеринбург");
    AddDataPoint(1, 43.122036, 131.925552, "BARAUSSE - Владивосток", "Приморский край, г. Владивосток, Океанский проспект, д. 43", "+7-4232-43-41-45", "+7-4232-43-41-48", "<a href='mailto:imprus@mail.primorye.ru'>imprus@mail.primorye.ru</a>", "<a href='http://barausse.com/portal/ru/russia/where-to-buy/store-detail/27_rivenditore' target='_blank'>barausse.com</a>", "Владивосток");
    AddDataPoint(2, 55.34633954818297, 86.10865116119385, "BARAUSSE - Кемерово", "Кемеровская область, г. Кемерово, Пионерский б-р, д. 14", "+7 (3842) 351-046", "", "<a href='mailto:'></a>", "<a href='http://barausse.com/portal/ru/russia/where-to-buy/store-detail/46_rivenditore' target='_blank'>barausse.com</a>", "Кемерово");
    AddDataPoint(3, 54.515207343316526, 36.24147713184357, "BARAUSSE - Калуга", "Калужская область, г. Калуга, ул. Гагарина, д. 1", "+7-4842-79-04-21", "+7-4842-79-04-20", "<a href='mailto:olaysh@mail.ru'>olaysh@mail.ru</a>", "<a href='http://barausse.com/portal/ru/russia/where-to-buy/store-detail/52_rivenditore' target='_blank'>barausse.com</a>", "Калуга");
    AddDataPoint(4, 54.96312282244225, 73.38942289352417, "BARAUSSE - Омск &quot;На ул. Пушкина&quot;", "Омская область, г. Омск, ул. Пушкина, д. 137", "+7-3812-32-50-51", "", "<a href='mailto:trambach@list.ru'>trambach@list.ru</a>", "<a href='http://barausse.com/portal/ru/russia/where-to-buy/store-detail/64_rivenditore' target='_blank'>barausse.com</a>", "Омск");
    AddDataPoint(5, 59.980881, 30.336191, "BARAUSSE - Санкт-Петербург &quot;На Студенческой&quot;", "г. Санкт-Петербург, ул. Студенческая, д.10, ТЦ&quot;Ланской&quot;,модуль B-4", "+7-812-332-30-81; +7-812-295-40-46; +7-921-933-99-26; +7-812-295-40-46", "", "<a href='mailto:opt@l-porte.com'>opt@l-porte.com</a>", "<a href='http://barausse.com/portal/ru/russia/where-to-buy/store-detail/65_rivenditore' target='_blank'>barausse.com</a>", "Санкт-Петербург");
    AddDataPoint(6, 55.81899490086866, 49.11241538392642, "BARAUSSE - КАЗАНЬ", "Республика Татарстан, г. Казань, ул. Чистопольская, д. 20/12", "+7-843-518-68-58", "+7-843-518-37-35", "<a href='mailto:info@baraussekazan.ru'>info@baraussekazan.ru</a>", "<a href='http://barausse.com/portal/ru/russia/where-to-buy/store-detail/71_rivenditore' target='_blank'>barausse.com</a>", "Казань");
    AddDataPoint(7, 57.627022, 39.882088, "BARAUSSE - Ярославль &quot;На Советской&quot;", "Ярославская область, г. Ярославль, ул. Советская, д. 16", "+7-4852-74-52-15", "", "<a href='mailto:otdelka@superfloor.ru'>otdelka@superfloor.ru</a>", "<a href='http://barausse.com/portal/ru/russia/where-to-buy/store-detail/72_rivenditore' target='_blank'>barausse.com</a>", "Ярославль");
    AddDataPoint(8, 54.729256, 20.477133, "BARAUSSE - Калининград", "Yanalova 17D", "", "", "<a href='mailto:sbr@westrussia.ru'>sbr@westrussia.ru</a>", "<a href='http://barausse.com/portal/ru/russia/where-to-buy/store-detail/73_rivenditore' target='_blank'>barausse.com</a>", "Калининград");
    AddDataPoint(9, 47.238220, 39.710083, "BARAUSSE - Ростов-на-Дону НА ЛАРИНА", "Ростовская область, г. Ростов-на -Дону, ул. Ларина, д.15/2", "+7-863-245-25-22", "+7-863-255-36-96", "<a href='mailto:vgorod@s-doors.com'>vgorod@s-doors.com</a>", "<a href='http://barausse.com/portal/ru/russia/where-to-buy/store-detail/76_rivenditore' target='_blank'>barausse.com</a>", "Ростов-на-Дону");
    AddDataPoint(10, 48.47919357220797, 135.11304706335068, "BARAUSSE - Хабаровск", "Хабаровский край, г. Хабаровск, пр-т 60 лет Октября, д. 152", "+7-4212-27-59-23", "", "<a href='mailto:alinia7@mail.ru'>alinia7@mail.ru</a>", "<a href='http://barausse.com/portal/ru/russia/where-to-buy/store-detail/78_rivenditore' target='_blank'>barausse.com</a>", "Хабаровск");
    AddDataPoint(11, 52.731457, 41.453819, "BARAUSSE - Тамбов", "Тамбовская область, г. Тамбов, ул. Советская, д.94", "+7-4752-71-86-19", "", "<a href='mailto:vivadesign@mail.ru'>vivadesign@mail.ru</a>", "<a href='http://barausse.com/portal/ru/russia/where-to-buy/store-detail/79_rivenditore' target='_blank'>barausse.com</a>", "Тамбов");
    AddDataPoint(12, 54.71005393530245, 55.994622856378555, "BARAUSSE - Уфа", "Республика Башкортостан, Уфимский район, г. Уфа, Дуванский б-р, дом №21,Салон &quot;ВАШ ДОМ&quot;", "+7-347-255-64-64", "+7-347-255-64-64", "<a href='mailto:vashdom@list.ru'>vashdom@list.ru</a>", "<a href='http://barausse.com/portal/ru/russia/where-to-buy/store-detail/80_rivenditore' target='_blank'>barausse.com</a>", "Уфа");
    AddDataPoint(13, 57.996532, 56.262638, "BARAUSSE - Пермь", "Пермский край, г. Пермь, ул. Чернышевского, д.23", "+7-342-216-32-71", "+7-342-216-32-71", "<a href='mailto:nata1530@mail.ru'>nata1530@mail.ru</a>", "<a href='http://barausse.com/portal/ru/russia/where-to-buy/store-detail/86_rivenditore' target='_blank'>barausse.com</a>", "Пермь");
    AddDataPoint(14, 55.16938295858763, 61.37392044067383, "BARAUSSE - Челябинск", "Челябинская область, г. Челябинск, ул. Труда, 185,     Центр интерьера«Магнит», секция 008", "+7 (351) 247-45-69", "", "<a href='mailto:dveri@barausse74.ru'>dveri@barausse74.ru</a>", "<a href='http://barausse.com/portal/ru/russia/where-to-buy/store-detail/87_rivenditore' target='_blank'>barausse.com</a>", "Челябинск");
    AddDataPoint(15, 53.330635916469994, 83.78888636827469, "BARAUSSE - Барнаул  - Площадь Свободы", "Алтайский край, г. Барнаул, пл. Свободы 6, ТЦ&quot;Поместье&quot;,1этаж, салон дверей &quot;Маленькая Италия&quot;", "+7-3852-65-88-04", "", "<a href='mailto:av-doors@mail.ru'>av-doors@mail.ru</a>", "<a href='http://barausse.com/portal/ru/russia/where-to-buy/store-detail/90_rivenditore' target='_blank'>barausse.com</a>", "Барнаул");
    AddDataPoint(16, 44.038883442395026, 43.03640574216843, "BARAUSSE - Пятигорск", "Ставропольский край, г. Пятигорск ул. Ермолова 28", "+7 (928) 555-55-83", "", "<a href='mailto:'></a>", "<a href='http://barausse.com/portal/ru/russia/where-to-buy/store-detail/91_rivenditore' target='_blank'>barausse.com</a>", "Пятигорск");
    AddDataPoint(17, 43.300181183978935, 45.70219427347183, "BARAUSSE - Грозный", "Чеченская республика, г. Грозный, ул. Мирзоева, д. 16", "8-928-890-30-80", "", "<a href='mailto:barausse_grozny@mail.ru'>barausse_grozny@mail.ru</a>", "<a href='http://barausse.com/portal/ru/russia/where-to-buy/store-detail/92_rivenditore' target='_blank'>barausse.com</a>", "Грозный");
    AddDataPoint(18, 57.15055244583224, 65.57110548019409, "BARAUSSE - Тюмень", "Тюменская область, г. Тюмень ул. 50 лет Октября д. 26/5", "+7 (3452) 566 801", "+7 (3452) 398 700", "<a href='mailto:barausse72@gmail.com'>barausse72@gmail.com</a>", "<a href='http://barausse.com/portal/ru/russia/where-to-buy/store-detail/93_rivenditore' target='_blank'>barausse.com</a>", "Тюмень");
    AddDataPoint(19, 52.619827, 39.580307, "BARAUSSE - Липецк", "Липецкая область, Липецкий район, г. Липецк, ул. Гагарина, д.21", "+7-4742-27-19-42", "+7-4742-27-19-42", "<a href='mailto:olate@mail.ru'>olate@mail.ru</a>", "<a href='http://barausse.com/portal/ru/russia/where-to-buy/store-detail/94_rivenditore' target='_blank'>barausse.com</a>", "Липецк");
    AddDataPoint(20, 56.473994013279444, 84.94750628247857, "BARAUSSE - Томск", "Томская область,  Типографский пер. 1а,  Салон &quot;Верона&quot;", "+7 (3822) 53-58-00", "", "<a href='mailto:ksnakul@mail.ru'>ksnakul@mail.ru</a>", "<a href='http://barausse.com/portal/ru/russia/where-to-buy/store-detail/95_rivenditore' target='_blank'>barausse.com</a>", "Томск");
    AddDataPoint(21, 53.465977, 59.078979, "BARAUSSE - Магнитогорск", "Челябинская область, г. Магнитогорск, проспект Карла Маркса, д. 71", "+7-3519-28-85-80", "+7-3519-28-85-80", "<a href='mailto:skse@mail.ru'>skse@mail.ru</a>", "<a href='http://barausse.com/portal/ru/russia/where-to-buy/store-detail/96_rivenditore' target='_blank'>barausse.com</a>", "Магнитогорск");
    AddDataPoint(22, 54.631886, 39.733685, "BARAUSSE - Рязань", "Рязанская область, г. Рязань, ул. Сенная, д. 12", "+7-4912-25-32-38", "+7-4912-25-34-62", "<a href='mailto:sm-venezia@yandex.ru'>sm-venezia@yandex.ru</a>", "<a href='http://barausse.com/portal/ru/russia/where-to-buy/store-detail/97_rivenditore' target='_blank'>barausse.com</a>", "Рязань");
    AddDataPoint(23, 54.96166916291419, 73.38586091995239, "BARAUSSE - Омск &quot;На К.Маркса&quot;", "Омская область, г. Омск, пр. К. Маркса, д. 36/1", "+7-3812-31-71-27", "+7-3812-37-14-38", "<a href='mailto:'></a>", "<a href='http://barausse.com/portal/ru/russia/where-to-buy/store-detail/98_rivenditore' target='_blank'>barausse.com</a>", "Омск");
    AddDataPoint(24, 58.60857955368696, 49.66226130723953, "BARAUSSE - Киров", "Кировская область, г. Киров, ул. Труда, дом 71", "+7 (8332) 41-55-77 многоканальный", "+7 (912) 330-58-04 мобильный", "<a href='mailto:nm-floor@yandex.ru'>nm-floor@yandex.ru</a>", "<a href='http://barausse.com/portal/ru/russia/where-to-buy/store-detail/99_rivenditore' target='_blank'>barausse.com</a>", "Киров");
    AddDataPoint(25, 51.667107, 39.197678, "BARAUSSE - Воронеж", "Воронежская область, г.Воронеж, ул.Средне-Московская, 15/17", "+7 (473) 252-37-37", "", "<a href='mailto:barausse-v@mail.ru'>barausse-v@mail.ru</a>", "<a href='http://barausse.com/portal/ru/russia/where-to-buy/store-detail/100_rivenditore' target='_blank'>barausse.com</a>", "Воронеж");
    AddDataPoint(26, 57.624451, 39.861145, "BARAUSSE - Ярославль &quot;На Волжской набережной&quot;", "Ярославская область, г. Ярославль, ул. Волжская набережная , д. 4", "+7-4852-94-61-33", "+7-4852-30-15-37", "<a href='mailto:interium.kav@gmail.com'>interium.kav@gmail.com</a>", "<a href='http://barausse.com/portal/ru/russia/where-to-buy/store-detail/101_rivenditore' target='_blank'>barausse.com</a>", "Ярославль");
    AddDataPoint(27, 45.040977, 38.967606, "BARAUSSE - Краснодар", "г. Краснодар, ул. Северная, д. 320/1, ТД &quot;Интерьер&quot;, 2 этаж", "+7-861-200-15-51", "+7-861-200-15-50", "<a href='mailto:info@parchetti.ru'>info@parchetti.ru</a>", "<a href='http://barausse.com/portal/ru/russia/where-to-buy/store-detail/102_rivenditore' target='_blank'>barausse.com</a>", "Краснодар");
    AddDataPoint(28, 52.29122950544327, 104.3017315864563, "BARAUSSE - Иркутск", "Иркутская область, г. Иркутск, ул. Октябрьской Революции, д.1/4Бизнесцентр &quot;Терра&quot; Салон художественного паркета и дверей &quot;Кавалер&quot;", "+7-3952-50-44-33", "", "<a href='mailto:288619@mail.ru'>288619@mail.ru</a>", "<a href='http://barausse.com/portal/ru/russia/where-to-buy/store-detail/103_rivenditore' target='_blank'>barausse.com</a>", "Иркутск");
    AddDataPoint(29, 56.838455, 53.219147, "BARAUSSE - Ижевск", "Удмуртская руспублика, г. Ижевск, ул. Холмогорова, д. 15, ТЦ СИТИ, 3 эт.", "+7-3412-91-20-74 / +7-909-055-44-62", "", "<a href='mailto:'></a>", "<a href='http://barausse.com/portal/ru/russia/where-to-buy/store-detail/104_rivenditore' target='_blank'>barausse.com</a>", "Ижевск");
    AddDataPoint(30, 53.201008, 45.010448, "BARAUSSE - Пенза", "Пензенская область, г. Пенза, ул. Пушкина, д.15", "+7-8412-20-20-66", "", "<a href='mailto:nd588@yandex.ru'>nd588@yandex.ru</a>", "<a href='http://barausse.com/portal/ru/russia/where-to-buy/store-detail/105_rivenditore' target='_blank'>barausse.com</a>", "Пенза");
    AddDataPoint(31, 59.204063, 39.861145, "BARAUSSE - Вологда", "Вологодская область, г. Вологда, ул. Ленинградская, д. 71, БЦ &quot;Сфера&quot;, 3 эт., бутик интерьеров &quot;ЭГО&quot;", "+7-8172-52-93-53", "+7-8172-52-93-57", "<a href='mailto:'></a>", "<a href='http://barausse.com/portal/ru/russia/where-to-buy/store-detail/107_rivenditore' target='_blank'>barausse.com</a>", "Вологда");
    AddDataPoint(32, 56.642635, 47.886658, "BARAUSSE - Йошкар-Ола", "Республика Марий Эл, г. Йошкар-Ола, ул. Кремлевская, д. 26а", "+7-8362-46-96-22", "+7-8362-46-96-22", "<a href='mailto:'></a>", "<a href='http://barausse.com/portal/ru/russia/where-to-buy/store-detail/116_rivenditore' target='_blank'>barausse.com</a>", "Йошкар-Ола");
    AddDataPoint(33, 53.26690699725491, 34.35321807861328, "BARAUSSE - Брянск", "Брянская область,Брянск ул. Бежицкая 1Б ТК &quot;МАНДАРИН&quot;", "+7-4832-36-22-00", "", "<a href='mailto:italiadoors@mail.ru'>italiadoors@mail.ru</a>", "<a href='http://barausse.com/portal/ru/russia/where-to-buy/store-detail/118_rivenditore' target='_blank'>barausse.com</a>", "Брянск");
    AddDataPoint(34, 56.317894, 44.060692, "BARAUSSE - Нижний Новгород", "Нижний Новгород, ул. Родионова, д. 23А", "+7-831-278-91-17", "+7-831-278-91-18", "<a href='mailto:'></a>", "<a href='http://barausse.com/portal/ru/russia/where-to-buy/store-detail/122_rivenditore' target='_blank'>barausse.com</a>", "Нижний Новгород");
    AddDataPoint(35, 43.605255, 39.722443, "BARAUSSE - Сочи &quot;На Донской&quot;", "Краснодарский край, г. Сочи, ул. Донская, 28, ТК &quot;Строй Сити&quot;,2 этаж", "+7-8622-55-13-10", "+7-8622-55-13-10", "<a href='mailto:premiumva@mail.ru'>premiumva@mail.ru</a>", "<a href='http://barausse.com/portal/ru/russia/where-to-buy/store-detail/123_rivenditore' target='_blank'>barausse.com</a>", "Сочи");
    AddDataPoint(36, 51.765642148151485, 55.122313499450684, "BARAUSSE - Оренбург", "Оренбургская область, г. Оренбург, ул. Чкалова, д.3/1", "+7-3532-66-55-00", "+7-3532-66-55-00", "<a href='mailto:ampir-orenburg@mail.ru'>ampir-orenburg@mail.ru</a>", "<a href='http://barausse.com/portal/ru/russia/where-to-buy/store-detail/125_rivenditore' target='_blank'>barausse.com</a>", "Оренбург");
    AddDataPoint(37, 59.980881, 30.336191, "BARAUSSE - Санкт-Петербург &quot;На Сампсониевском&quot;", "г. Санкт-Петербург, Большой Сампсониевский проспект, д. 70", "+7-812-331-90-99", "+7-812-596-38-32", "<a href='mailto:spb@parchetti.ru'>spb@parchetti.ru</a>", "<a href='http://barausse.com/portal/ru/russia/where-to-buy/store-detail/126_rivenditore' target='_blank'>barausse.com</a>", "Санкт-Петербург");
    AddDataPoint(38, 51.732132, 36.232910, "BARAUSSE - Курск", "Курская область, г. Курск, Малиновый 2-й пер., д.5а", "+7-4712-32-69-42", "+7-4712-32-69-42", "<a href='mailto:joludev-stroi@mail.ru'>joludev-stroi@mail.ru</a>", "<a href='http://barausse.com/portal/ru/russia/where-to-buy/store-detail/128_rivenditore' target='_blank'>barausse.com</a>", "Курск");
    AddDataPoint(39, 56.044743, 92.90318, "BARAUSSE - Красноярск &quot;На Алексеева&quot;", "Красноярский край, г. Красноярск, ул. Алексеева, д.93", "+7-391-226-88-55", "+7-391-226-88-55", "<a href='mailto:'></a>", "<a href='http://barausse.com/portal/ru/russia/where-to-buy/store-detail/129_rivenditore' target='_blank'>barausse.com</a>", "Красноярск");
    AddDataPoint(40, 45.04320860851281, 41.964809596538544, "BARAUSSE - Ставрополь", "Ставропольский край, г. Ставрополь, ул. Р. Люксембург, д. 3", "+7-8652-266-267", "+7-8652-266-302", "<a href='mailto:26doors@mail.ru'>26doors@mail.ru</a>", "<a href='http://barausse.com/portal/ru/russia/where-to-buy/store-detail/131_rivenditore' target='_blank'>barausse.com</a>", "Ставрополь");
    AddDataPoint(41, 48.712360, 44.521858, "BARAUSSE - Волгоград", "Волгоградская область, г. Волгоград, ул. Порт-Саида, д.8", "+7-8442-38-63-00", "+7-8442-38-63-00", "<a href='mailto:'></a>", "<a href='http://barausse.com/portal/ru/russia/where-to-buy/store-detail/132_rivenditore' target='_blank'>barausse.com</a>", "Волгоград");
    AddDataPoint(42, 56.831627, 60.618179, "BARAUSSE - Екатеринбург  Красноармейская", "г. Екатеринбург, ул. Красноармейская, д.66", "+7-343-228-11-11", "+7-343-228-11-11", "<a href='mailto:ekb@parchetti.ru'>ekb@parchetti.ru</a>", "<a href='http://barausse.com/portal/ru/russia/where-to-buy/store-detail/133_rivenditore' target='_blank'>barausse.com</a>", "Екатеринбург");
    AddDataPoint(43, 54.1857102392563, 37.62388363480568, "BARAUSSE - Тула", "Тульская область, г. Тула, ул. Колетвинова, д.6", "+7 (4872) 701 071", "+7 (4872) 55 04 80", "<a href='mailto:info@tuladoors.ru'>info@tuladoors.ru</a>", "<a href='http://barausse.com/portal/ru/russia/where-to-buy/store-detail/135_rivenditore' target='_blank'>barausse.com</a>", "Тула");
    AddDataPoint(44, 54.185745, 45.181274, "BARAUSSE - Саранск", "Республика Мордовия, г. Саранск, ул. Васенко, д.10", "+7-8432-32-81-29", "+7-8432-32-81-28", "<a href='mailto:mirdveri@mail.ru'>mirdveri@mail.ru</a>", "<a href='http://barausse.com/portal/ru/russia/where-to-buy/store-detail/137_rivenditore' target='_blank'>barausse.com</a>", "Саранск");
    AddDataPoint(45, 56.135220, 47.243614, "BARAUSSE - Чебоксары &quot;На проспекте Горького&quot;", "Чувашская Республика, г. Чебоксары, пр. М.Горького, 6, галерея &quot;ALBERRO&quot;", "+7-8352-43-97-77", "+7-8352-43-97-77", "<a href='mailto:'></a>", "<a href='http://barausse.com/portal/ru/russia/where-to-buy/store-detail/139_rivenditore' target='_blank'>barausse.com</a>", "Чебоксары");
    AddDataPoint(46, 59.881336, 30.310249, "BARAUSSE - Санкт-Петербург &quot;На Московском&quot;", "г. Санкт-Петербург, ул. Варшавская, д.3,ТЦ&quot;Мебельныйконтинент&quot;, секции IV-304-306", "+7-812-640-73-57, 974-14-88", "+7-812-373-76-35", "<a href='mailto:barausse-neva@mail.ru'>barausse-neva@mail.ru</a>", "<a href='http://barausse.com/portal/ru/russia/where-to-buy/store-detail/140_rivenditore' target='_blank'>barausse.com</a>", "Санкт-Петербург");
    AddDataPoint(47, 59.921658, 30.307728, "BARAUSSE - Санкт-Петербург &quot;На Вознесенском&quot;", "г. Санкт-Петербург, Вознесенский проспект, д.47", "+7-812-314-44-94", "+7-812-314-44-94", "<a href='mailto:iacademspb@yandex.ru'>iacademspb@yandex.ru</a>", "<a href='http://barausse.com/portal/ru/russia/where-to-buy/store-detail/141_rivenditore' target='_blank'>barausse.com</a>", "Санкт-Петербург");
    AddDataPoint(48, 50.596313, 36.606445, "BARAUSSE - Белгород &quot;На Архиерейской&quot;", "Белгород, ул. Архиерейская,  4", "+7-4722-55-72-17", "+7-4722-55-72-17", "<a href='mailto:'></a>", "<a href='http://barausse.com/portal/ru/russia/where-to-buy/store-detail/143_rivenditore' target='_blank'>barausse.com</a>", "Белгород");
    AddDataPoint(49, 55.041157, 82.913559, "BARAUSSE - НОВОСИБИРСК на Советской", "Новосибирская область, г. Новосибирск, ул. Советская, д. 46/2", "+7-383-220-11-11", "+7-383-218-45-34", "<a href='mailto:info@vipdoors.ru'>info@vipdoors.ru</a>", "<a href='http://barausse.com/portal/ru/russia/where-to-buy/store-detail/145_rivenditore' target='_blank'>barausse.com</a>", "Новосибирск");
    AddDataPoint(50, 55.734379, 37.656631, "BARAUSSE - Таганская", "г. Москва, Новоспасский переулок, д.3", "+7-495-671-00-00", "", "<a href='mailto:russia.support@barausse.com'>russia.support@barausse.com</a>", "<a href='http://barausse.com/portal/ru/russia/where-to-buy/store-detail/146_rivenditore' target='_blank'>barausse.com</a>", "Москва");
    AddDataPoint(51, 56.992043, 40.972309, "BARAUSSE - Иваново", "Ивановская область, г. Иваново, ул. Парижской Коммуны, д.16", "+7-4932-59-04-24", "+7-4932-41-21-36", "<a href='mailto:dveri37@mail.ru'>dveri37@mail.ru</a>", "<a href='http://barausse.com/portal/ru/russia/where-to-buy/store-detail/148_rivenditore' target='_blank'>barausse.com</a>", "Иваново");
    AddDataPoint(52, 56.137479095908176, 47.277664840221405, "BARAUSSE - Чебоксары &quot;На Калинина&quot;", "Чувашская Республика, г. Чебоксары,ул.Калинина,105а,ТРК&quot;МегаМолл&quot;, 1 этаж", "8 (8352) 223-018", "", "<a href='mailto:bv223018@yandex.ru'>bv223018@yandex.ru</a>", "<a href='http://barausse.com/portal/ru/russia/where-to-buy/store-detail/149_rivenditore' target='_blank'>barausse.com</a>", "Чебоксары");
    AddDataPoint(53, 44.746735, 37.765503, "BARAUSSE - Новороссийск", "Краснодарский край, г. Новороссийск, ул. Свободы, д. 9/11", "+7-8617-64-49-70", "", "<a href='mailto:'></a>", "<a href='http://barausse.com/portal/ru/russia/where-to-buy/store-detail/150_rivenditore' target='_blank'>barausse.com</a>", "Новороссийск");
    AddDataPoint(54, 50.608444, 36.580524, "BARAUSSE - Белгород &quot;На проспекте Хмельницкого&quot;", "Белгород, проспект Б. Хмельницкого, 70", "+7-4722-32-88-82", "", "<a href='mailto:'></a>", "<a href='http://barausse.com/portal/ru/russia/where-to-buy/store-detail/154_rivenditore' target='_blank'>barausse.com</a>", "Белгород");
    AddDataPoint(55, 56.015274, 92.878418, "BARAUSSE - Красноярск &quot;Партизана Железняка&quot;", "Красноярский край, г. Красноярск, ул. Партизана Железняка, д. 6а", "+7-391-226-88-00", "+7-391-226-88-00", "<a href='mailto:'></a>", "<a href='http://barausse.com/portal/ru/russia/where-to-buy/store-detail/155_rivenditore' target='_blank'>barausse.com</a>", "Красноярск");
    AddDataPoint(56, 59.123817, 38.020935, "BARAUSSE - Череповец", "Вологодская область, г. Череповец, Советский пр-т, д. 16", "+7-8202-32-35-11; +7-921-251-68-25", "", "<a href='mailto:'></a>", "<a href='http://barausse.com/portal/ru/russia/where-to-buy/store-detail/156_rivenditore' target='_blank'>barausse.com</a>", "Череповец");
    AddDataPoint(57, 43.439587, 39.927727, "BARAUSSE - Сочи &quot;На Орджоникидзе&quot;", "Краснодарский край, г. Сочи, ул. Орджоникидзе, 8", "+7-622-62-28-43", "", "<a href='mailto:'></a>", "<a href='http://barausse.com/portal/ru/russia/where-to-buy/store-detail/162_rivenditore' target='_blank'>barausse.com</a>", "Сочи");
    AddDataPoint(58, 61.250000, 73.416664, "BARAUSSE - Сургут", "Ханты-Мансийский Автономный округ-Югра АО, г. Сургут, ул. Григория Кукуевицкого, 15/1", "+7-3462-36-04-80", "", "<a href='mailto:'></a>", "<a href='http://barausse.com/portal/ru/russia/where-to-buy/store-detail/163_rivenditore' target='_blank'>barausse.com</a>", "Сургут");
    AddDataPoint(62, 55.828072, 37.489075, "BARAUSSE - &quot;Войковская-Ленинградское ш&quot;", "г. Москва, Ленинградское шоссе, д. 25, 3-й этаж Центр дизайна &quot;Ленинградcкий&quot;", "7 (495) 671 0000 доб. 161", "+7 916 869 47 38", "<a href='mailto:mosca@barausse.ru'>mosca@barausse.ru</a>", "<a href='http://barausse.com/portal/ru/russia/where-to-buy/store-detail/barausse-leningradkoe-s' target='_blank'>barausse.com</a>", "Москва");
    AddDataPoint(63, 55.684166, 37.412655, "BARAUSSE - ЭлитСтройМатериалы", "51-й км МКАД внешняя сторона ТЦ &quot;ЭлитСтройМатериалы&quot; стенд С 35", "+7 (495) 642-07-17", "", "<a href='mailto:6420717@yaguar-dveri.ru'>6420717@yaguar-dveri.ru</a>", "<a href='http://barausse.com/portal/ru/russia/where-to-buy/store-detail/barausse-elitstrojmaterialy' target='_blank'>barausse.com</a>", "Москва");
    AddDataPoint(64, 55.704006, 37.596142, "BARAUSSE - Сантехника 7", "5-й Донской проезд, д. 23, 1 этаж ТЦ &quot;Сантехника 7&quot;", "+7-495-722-85-04, +7-495-642-07-52", "", "<a href='mailto:'></a>", "<a href='http://barausse.com/portal/ru/russia/where-to-buy/store-detail/barausse-santehnika-7' target='_blank'>barausse.com</a>", "Москва");
    AddDataPoint(65, 55.884853, 37.603424, "BARAUSSE - Бибирево", "Ул. Пришвина, д. 26, ТВК &quot;Миллион Мелочей&quot; 2-й этаж, пав. D 41", "+7 (495) 727-00-76", "", "<a href='mailto:info@barausse-porte.ru'>info@barausse-porte.ru</a>", "<a href='http://barausse.com/portal/ru/russia/where-to-buy/store-detail/barausse-bibirevo' target='_blank'>barausse.com</a>", "Москва");
    AddDataPoint(66, 55.724220, 37.584759, "BARAUSSE - Фрунзенская", "Фрунзенская набережная 30, ТЦ &quot;РОССТРОЙЭКСПО&quot; пав. 22", "+7 (495) 517-45-72", "+7 (495) 517-45-72", "<a href='mailto:12mebel@mail.ru'>12mebel@mail.ru</a>", "<a href='http://barausse.com/portal/ru/russia/where-to-buy/store-detail/barausse-frunzenskaa' target='_blank'>barausse.com</a>", "Москва");
    AddDataPoint(67, 55.779099, 37.556679, "BARAUSSE - Беговая", "Беговая ул, д. 11", "+7 (495) 945-23-93", "", "<a href='mailto:'></a>", "<a href='http://barausse.com/portal/ru/russia/where-to-buy/store-detail/barausse-begovaa' target='_blank'>barausse.com</a>", "Москва");
    AddDataPoint(68, 55.667160, 37.583717, "BARAUSSE-Севастопольский", "Севастопольский пр-кт, д. 51, корп. 2", "+7 (495) 649-86-42", "+7 (495) 649-86-42", "<a href='mailto:sevastopolskiy@dorian.ru, info@dorian.ru'>sevastopolskiy@dorian.ru, info@dorian.ru</a>", "<a href='http://barausse.com/portal/ru/russia/where-to-buy/store-detail/barausse-sevastopolskij' target='_blank'>barausse.com</a>", "Москва");
    AddDataPoint(69, 53.200576597004805, 50.14240622520447, "BARAUSSE - Самара", "ул. Коммунистическая д.27", "+7 (846) 266-45-45", "", "<a href='mailto:parket@samtel.ru'>parket@samtel.ru</a>", "<a href='http://barausse.com/portal/ru/russia/where-to-buy/store-detail/barausse-naiaa' target='_blank'>barausse.com</a>", "Самара");
    AddDataPoint(70, 55.807434, 37.495777, "BARAUSSE - Волоколамское", "Волоколамское ш., д.13", "+7 (495) 649-86-92", "+7 (495) 649-86-92", "<a href='mailto:volokolamka@dorian-dveri.ru'>volokolamka@dorian-dveri.ru</a>", "<a href='http://barausse.com/portal/ru/russia/where-to-buy/store-detail/barausse-volokolamskoe' target='_blank'>barausse.com</a>", "Москва");
    AddDataPoint(71, 55.72131873495109, 37.701978433959994, "BARAUSSE - Метр квадратный", "Московская обл., г.Москва, Волгоградский пр-т, д.32, корп. 25, 2-ой этаж, пав. 23-26", "+7 (495) 567-24-35", "", "<a href='mailto:metr.barausse@gmail.com'>metr.barausse@gmail.com</a>", "<a href='http://barausse.com/portal/ru/russia/where-to-buy/store-detail/barausse-metr-kvadratnyj' target='_blank'>barausse.com</a>", "Москва");
    AddDataPoint(72, 56.83210286309289, 60.58513816410539, "BARAUSSE - Екатеринбург Малышева", "Свердловская обл., г.Екатеринбург, ул.Малышева, д.8, Интерьерный центр &quot;Architector&quot;", "+7 (343) 287-12-21", "+7 (343) 287-12-21", "<a href='mailto:ekb-architector@parchetti.ru'>ekb-architector@parchetti.ru</a>", "<a href='http://barausse.com/portal/ru/russia/where-to-buy/store-detail/barausse-ekaterinburg-malysheva' target='_blank'>barausse.com</a>", "Екатеринбург");
    AddDataPoint(73, 59.95281959555045, 30.25743009629514, "BARAUSSE - Санкт Петербург  Железноводская", "Ленинградская обл.,г.Санкт-Петербург,ул.Железноводская,д.3,Строймаркет&quot;Василеостровский&quot;61 модуль", "+7 (812) 324-45-74", "+7 (812) 324-45-74", "<a href='mailto:baraussespb@mail.ru'>baraussespb@mail.ru</a>", "<a href='http://barausse.com/portal/ru/russia/where-to-buy/store-detail/barausse-sanktpeterburg-zheleznovodskaya' target='_blank'>barausse.com</a>", "Санкт-Петербург");
    AddDataPoint(74, 52.60430080524135, 39.58194522883605, "BARAUSSE -Липецк", "г. Липецк, ул. Советская, д.36", "+7(4742) 222-747", "", "<a href='mailto:info@dl48.ru, dl57orel@mail.ru'>info@dl48.ru, dl57orel@mail.ru</a>", "<a href='http://barausse.com/portal/ru/russia/where-to-buy/store-detail/barausse-lipeck' target='_blank'>barausse.com</a>", "Липецк");
    AddDataPoint(75, 59.99834760645418, 30.26854727301634, "BARAUSSE - Санкт-Петербург на Богатырском", "г. Санкт-Петербург, Богатырский проспект, д.14, ТК&quot; Интерио&quot;,главный вход, 2-й этаж, секция 219", "+7(812) 677-89-93", "+7(812) 677-89-93", "<a href='mailto:inter@dveretti.com'>inter@dveretti.com</a>", "<a href='http://barausse.com/portal/ru/russia/where-to-buy/store-detail/barausse-sankt-peterburg-na-bogatyrskom' target='_blank'>barausse.com</a>", "Санкт-Петербург");
    AddDataPoint(76, 53.537604013767854, 49.31569576263428, "BARAUSSE - Тольятти", "Самарская область, г. Тольятти, ул. Ворошилова, д.15, офис 1", "+7 (8482) 20 69 40", "", "<a href='mailto:salon-v@mail.ru'>salon-v@mail.ru</a>", "<a href='http://barausse.com/portal/ru/russia/where-to-buy/store-detail/barausse-oieuyooe' target='_blank'>barausse.com</a>", "Тольятти");
    AddDataPoint(77, 56.85567740690004, 35.904951095581055, "BARAUSSE - Тверь", "г.Тверь, улица Симеоновская, д. 1", "+7 (4822) 777-298", "+7 (4822) 331-649", "<a href='mailto:office@baraussetver.ru'>office@baraussetver.ru</a>", "<a href='http://barausse.com/portal/ru/russia/where-to-buy/store-detail/barausse-tver' target='_blank'>barausse.com</a>", "Тверь");
    AddDataPoint(78, 56.3127164651114, 43.98745536804199, "BARAUSSE - Нижний Новгород", "ул. М. Горького, 65А", "+7 (831) 433-63-77", "+7 (831) 433-10-47", "<a href='mailto:'></a>", "<a href='http://barausse.com/portal/ru/russia/where-to-buy/store-detail/barausse-niznij-novgorod' target='_blank'>barausse.com</a>", "Нижний Новогород");
    AddDataPoint(79, 55.6910163500922, 37.549161314964294, "BARAUSSE - ЛЕНИНСКИЙ", "г. Москва, Ленинский пр-т 66", "+7 (495) 788-83-42", "", "<a href='mailto:leninskiy@parchetti.ru'>leninskiy@parchetti.ru</a>", "<a href='http://barausse.com/portal/ru/russia/where-to-buy/store-detail/barausse-ia-eaieineii' target='_blank'>barausse.com</a>", "Москва");
    AddDataPoint(80, 47.23497677257527, 39.70520943403244, "BARAUSSE - Ростов-на-Дону НА ТЕКУЧЕВА", "Ростовская область, г. Ростов-на-Дону, ул. Текучева, 139А ИЦ &quot;Миллениум&quot;", "+7 (863) 294-41-18", "+7 (863) 294-41-17", "<a href='mailto:barausse@parchetti.ru'>barausse@parchetti.ru</a>", "<a href='http://barausse.com/portal/ru/russia/where-to-buy/store-detail/barausse-ðinoia-ia-aiio-ieeeaieoi' target='_blank'>barausse.com</a>", "Ростов-на-Дону");
    AddDataPoint(81, 47.23258635476597, 39.73556935787201, "BARAUSSE - Ростов-на-Дону НА КРАСНОАРМЕЙСКОЙ", "Ростовскай область, г. Ростов-на-Дону, ул. Красноармейская, д. 264", "8-988-251-96-00", "", "<a href='mailto:barausse@parchetti.ru'>barausse@parchetti.ru</a>", "<a href='http://barausse.com/portal/ru/russia/where-to-buy/store-detail/barausse-ðinoia-ia-aiio-ia-eðaniiaðiaeneie' target='_blank'>barausse.com</a>", "Ростов-на-Дону");
    AddDataPoint(82, 55.08162813895918, 82.93246507644653, "BARAUSSE - НОВОСИБИРСК на Светлановской", "Новосибирская область, г. Новосибирск , ул. Светлановская 50", "+7 (383) 363-73-89", "", "<a href='mailto:'></a>", "<a href='http://barausse.com/portal/ru/russia/where-to-buy/store-detail/barausse-iiaineaeðne-ia-naaoeaiianeie' target='_blank'>barausse.com</a>", "Новосибирск");
    AddDataPoint(83, 56.137454406196426, 47.277533411979675, "BARAUSSE - Чебоксары на Калинина", "г.Чебоксары ул. Калинина 105А ТРК &quot;МЕГАМОЛЛ&quot; 1 этаж левое крыло", "8 (8352) 223-018", "", "<a href='mailto:bv223018@yandex.ru'>bv223018@yandex.ru</a>", "<a href='http://barausse.com/portal/ru/russia/where-to-buy/store-detail/barausse-aaienaðu' target='_blank'>barausse.com</a>", "Чебоксары");
    AddDataPoint(84, 55.03814907380174, 82.91352281346917, "BARAUSSE - НОВОСИБИРСК на Сибирской", "Новосибирская область, г. Новосибирск, ул. Сибирская, д.57", "+7-383-209-16-01", "", "<a href='mailto:sib@parchetti.ru'>sib@parchetti.ru</a>", "<a href='http://barausse.com/portal/ru/russia/where-to-buy/store-detail/barausse-novosibirsk-na-sibirskoj' target='_blank'>barausse.com</a>", "Новосибирск");
    AddDataPoint(85, 56.09645102183093, 40.26332859881222, "BARAUSSE - ВЛАДИМИР", "г. Владимир, мкр. Юрьевец, ул. Станционная, дом 2", "Tel.: +7(904)659-99-79", "", "<a href='mailto:'></a>", "<a href='http://barausse.com/portal/ru/russia/where-to-buy/store-detail/barausse-vladimir' target='_blank'>barausse.com</a>", "Владимир");
    AddDataPoint(86, 43.03323318794603, 44.649932705797255, "BARAUUSE - ВЛАДИКАВКАЗ", "улица Доватора, дом 88", "8(8972)52-21-44", "", "<a href='mailto:vladparket15@mail.ru'>vladparket15@mail.ru</a>", "<a href='http://barausse.com/portal/ru/russia/where-to-buy/store-detail/barauuse-vladikavkaz' target='_blank'>barausse.com</a>", "Владикавказ");
    AddDataPoint(87, 55.671670631052564, 37.584880040958524, "BARAUSSE - &quot;ЭКСПОСТРОЙ - НАХИМОВСКИЙ&quot;", "г. Москва, Нахимовский просп., 24, стр. 1", "+7(495)671-00-00", "", "<a href='mailto:mosca@barausse.ru'>mosca@barausse.ru</a>", "<a href='http://barausse.com/portal/ru/russia/where-to-buy/store-detail/barausse-ekspostroj-nahimovskij' target='_blank'>barausse.com</a>", "Москва");
    AddDataPoint(88, 51.525586868827546, 46.04150419123471, "BARAUSSE - Саратов Чернышевского", "г. Саратов, ул. Чернышевского, д. 183  Салон интерьера&quot; ТЕЛЕПОРТ&quot;", "+7(8452) 23-23-66", "+7(8452) 23-46-61", "<a href='mailto:tportmebel@mail.ru'>tportmebel@mail.ru</a>", "<a href='http://barausse.com/portal/ru/russia/where-to-buy/store-detail/barausse-saratov-cernysevskogo' target='_blank'>barausse.com</a>", "Саратов");
    AddDataPoint(89, 44.89979657743778, 37.347005903720856, "BARAUSSE  - Анапа", "Россия, Краснодарский край, г. Анапа,  Анапское шоссе, 1, Торговый дом &quot;ПИРАМИДА&quot;", "+7 (86133) 4-55-55", "", "<a href='mailto:vengard@inbox.ru'>vengard@inbox.ru</a>", "<a href='http://barausse.com/portal/ru/russia/where-to-buy/store-detail/barausse-anapa' target='_blank'>barausse.com</a>", "Анапа");
    AddDataPoint(90, 59.96177524297693, 30.30794396996498, "BARAUSSE - Санкт-Петербург &quot;На Пушкарской&quot;", "г. Санкт-Петербург, ул. Большая Пушкарская, д. 46", "+7 (812) 331-90-88", "", "<a href='mailto:spb-pushkarskaia@vparchetti.ru'>spb-pushkarskaia@vparchetti.ru</a>", "<a href='http://barausse.com/portal/ru/russia/where-to-buy/store-detail/barausse-sankt-peterburg-na-puskarskoj' target='_blank'>barausse.com</a>", "Санкт-Петербург");
    AddDataPoint(91, 55.78700613793474, 49.18856509029865, "BARAUSSE - КАЗАНЬ НА КАМАЛЕЕВА", "Республика Татарстан, г. Казань, пр-т Альберта Камалеева, д. 26", "+7 (843) 590 3590", "", "<a href='mailto:kutuzova-ue@parchetti.ru'>kutuzova-ue@parchetti.ru</a>", "<a href='http://barausse.com/portal/ru/russia/where-to-buy/store-detail/barausse-kazan?-na-kamaleeva' target='_blank'>barausse.com</a>", "Казань");
    AddDataPoint(92, 53.340796559927725, 83.7911219894886, "BARAUSSE - Барнаул - Пролетарская", "ул. Пролетарская, д. 56, офис №Н17 Салон дверей и напольных покрытий &quot;LEGNO&quot;", "+7 (3852) 555 221", "", "<a href='mailto:legnoo@mail.ru'>legnoo@mail.ru</a>", "<a href='http://barausse.com/portal/ru/russia/where-to-buy/store-detail/barausse-barnaul-proletarskaa' target='_blank'>barausse.com</a>", "Барнаул");
}
