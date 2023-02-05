ymaps.modules.define('test.control.searchControl.fakeGeocodeProvider', [
    'geocode',
    'geoXml.parser.ymapsml.geoObjects',
    'util.extend',
    'vow'
], function (provide, geocode, geoObjects, extend, vow) {
    /**
     * Custom provider for geocoding.
     * Intercepts test requests and returns the prepared data.
     * If the request is not known — sends it to the geocoder.
     * @param {String}  request
     * @param {options}  options
     * @returns "{vow.Promise}
     "*/
    var geocodeProvider = {
        geocode: function (request, options) {
            if (request == 'Москва' || request == '84.70126567, 88.20093371') {
                var deferred = vow.defer(),
                    data = '{"response":{"Attribution":"","GeoObjectCollection":{"metaDataProperty":{"GeocoderResponseMetaData":{"request":"Москва","found":"25","results":"10"}},"featureMember":[{"GeoObject":{"metaDataProperty":{"GeocoderMetaData":{"kind":"locality","text":"Россия, Москва","precision":"other","AddressDetails":{"Country":{"AddressLine":"Москва","CountryNameCode":"RU","CountryName":"Россия","AdministrativeArea":{"AdministrativeAreaName":"Центральный федеральный округ","SubAdministrativeArea":{"SubAdministrativeAreaName":"Москва","Locality":{"LocalityName":"Москва"}}}}}}},"description":"Россия","name":"Москва","boundedBy":{"Envelope":{"lowerCorner":"37.298509 55.490631","upperCorner":"37.967682 55.957565"}},"Point":{"pos":"37.619899 55.753676"}}},{"GeoObject":{"metaDataProperty":{"GeocoderMetaData":{"kind":"hydro","text":"Россия, река Москва","precision":"other","AddressDetails":{"Country":{"AddressLine":"река Москва","CountryNameCode":"RU","CountryName":"Россия","AdministrativeArea":{"AdministrativeAreaName":"Центральный федеральный округ","Locality":{"Premise":{"PremiseName":"река Москва"}}}}}}},"description":"Россия","name":"река Москва","boundedBy":{"Envelope":{"lowerCorner":"35.27093 55.075175","upperCorner":"38.845183 55.83022"}},"Point":{"pos":"37.049280 55.709449"}}},{"GeoObject":{"metaDataProperty":{"GeocoderMetaData":{"kind":"hydro","text":"Польша, Великопольское воеводство, Москва","precision":"other","AddressDetails":{"Country":{"AddressLine":"Великопольское воеводство, Москва","CountryNameCode":"PL","CountryName":"Польша","AdministrativeArea":{"AdministrativeAreaName":"Великопольское воеводство","Locality":{"Premise":{"PremiseName":"Москва"}}}}}}},"description":"Великопольское воеводство, Польша","name":"Москва","boundedBy":{"Envelope":{"lowerCorner":"17.043466 52.10327","upperCorner":"17.386434 52.320926"}},"Point":{"pos":"17.286694 52.168989"}}},{"GeoObject":{"metaDataProperty":{"GeocoderMetaData":{"kind":"street","text":"Россия, Липецкая область, Липецкий район, село Подгорное, улица Москва","precision":"street","AddressDetails":{"Country":{"AddressLine":"Липецкая область, Липецкий район, село Подгорное, улица Москва","CountryNameCode":"RU","CountryName":"Россия","AdministrativeArea":{"AdministrativeAreaName":"Липецкая область","SubAdministrativeArea":{"SubAdministrativeAreaName":"Липецкий район","Locality":{"LocalityName":"село Подгорное","Thoroughfare":{"ThoroughfareName":"улица Москва"}}}}}}}},"description":"село Подгорное, Липецкий район, Липецкая область, Россия","name":"улица Москва","boundedBy":{"Envelope":{"lowerCorner":"39.499175 52.539358","upperCorner":"39.513808 52.544698"}},"Point":{"pos":"39.506271 52.541554"}}},{"GeoObject":{"metaDataProperty":{"GeocoderMetaData":{"kind":"locality","text":"Россия, Кировская область, Верхошижемский район, деревня Москва","precision":"other","AddressDetails":{"Country":{"AddressLine":"Кировская область, Верхошижемский район, деревня Москва","CountryNameCode":"RU","CountryName":"Россия","AdministrativeArea":{"AdministrativeAreaName":"Кировская область","SubAdministrativeArea":{"SubAdministrativeAreaName":"Верхошижемский район","Locality":{"LocalityName":"деревня Москва"}}}}}}},"description":"Верхошижемский район, Кировская область, Россия","name":"деревня Москва","boundedBy":{"Envelope":{"lowerCorner":"49.105426 57.963808","upperCorner":"49.109235 57.969222"}},"Point":{"pos":"49.107420 57.966448"}}},{"GeoObject":{"metaDataProperty":{"GeocoderMetaData":{"kind":"locality","text":"Россия, Тверская область, Пеновский район, деревня Москва","precision":"other","AddressDetails":{"Country":{"AddressLine":"Тверская область, Пеновский район, деревня Москва","CountryNameCode":"RU","CountryName":"Россия","AdministrativeArea":{"AdministrativeAreaName":"Тверская область","SubAdministrativeArea":{"SubAdministrativeAreaName":"Пеновский район","Locality":{"LocalityName":"деревня Москва"}}}}}}},"description":"Пеновский район, Тверская область, Россия","name":"деревня Москва","boundedBy":{"Envelope":{"lowerCorner":"32.15807 56.914822","upperCorner":"32.171554 56.918983"}},"Point":{"pos":"32.164817 56.916350"}}},{"GeoObject":{"metaDataProperty":{"GeocoderMetaData":{"kind":"hydro","text":"Россия, Омская область, Седельниковский район, река Москва","precision":"other","AddressDetails":{"Country":{"AddressLine":"Омская область, Седельниковский район, река Москва","CountryNameCode":"RU","CountryName":"Россия","AdministrativeArea":{"AdministrativeAreaName":"Омская область","SubAdministrativeArea":{"SubAdministrativeAreaName":"Седельниковский район","Locality":{"Premise":{"PremiseName":"река Москва"}}}}}}}},"description":"Седельниковский район, Омская область, Россия","name":"река Москва","boundedBy":{"Envelope":{"lowerCorner":"75.885606 56.781084","upperCorner":"75.920487 56.79934"}},"Point":{"pos":"75.897571 56.784023"}}},{"GeoObject":{"metaDataProperty":{"GeocoderMetaData":{"kind":"hydro","text":"Россия, Тюменская область, Сладковский район, болото Москва","precision":"other","AddressDetails":{"Country":{"AddressLine":"Тюменская область, Сладковский район, болото Москва","CountryNameCode":"RU","CountryName":"Россия","AdministrativeArea":{"AdministrativeAreaName":"Тюменская область","SubAdministrativeArea":{"SubAdministrativeAreaName":"Сладковский район","Locality":{"Premise":{"PremiseName":"болото Москва"}}}}}}}},"description":"Сладковский район, Тюменская область, Россия","name":"болото Москва","boundedBy":{"Envelope":{"lowerCorner":"70.067712 55.611134","upperCorner":"70.07921 55.618151"}},"Point":{"pos":"70.073766 55.614551"}}},{"GeoObject":{"metaDataProperty":{"GeocoderMetaData":{"kind":"hydro","text":"Россия, Пермский край, Добрянский район, река Москва","precision":"other","AddressDetails":{"Country":{"AddressLine":"Пермский край, Добрянский район, река Москва","CountryNameCode":"RU","CountryName":"Россия","AdministrativeArea":{"AdministrativeAreaName":"Пермский край","SubAdministrativeArea":{"SubAdministrativeAreaName":"Добрянский район","Locality":{"Premise":{"PremiseName":"река Москва"}}}}}}}},"description":"Добрянский район, Пермский край, Россия","name":"река Москва","boundedBy":{"Envelope":{"lowerCorner":"56.726248 58.540183","upperCorner":"56.765351 58.566002"}},"Point":{"pos":"56.759126 58.563224"}}},{"GeoObject":{"metaDataProperty":{"GeocoderMetaData":{"kind":"locality","text":"Россия, Псковская область, Порховский район, деревня Москва","precision":"other","AddressDetails":{"Country":{"AddressLine":"Псковская область, Порховский район, деревня Москва","CountryNameCode":"RU","CountryName":"Россия","AdministrativeArea":{"AdministrativeAreaName":"Псковская область","SubAdministrativeArea":{"SubAdministrativeAreaName":"Порховский район","Locality":{"LocalityName":"деревня Москва"}}}}}}},"description":"Порховский район, Псковская область, Россия","name":"деревня Москва","boundedBy":{"Envelope":{"lowerCorner":"29.184009 57.446894","upperCorner":"29.18709 57.449945"}},"Point":{"pos":"29.185626 57.448671"}}}]}}}',
                    dataOneResult = '{"response":{"Attribution":"","GeoObjectCollection":{"metaDataProperty":{"GeocoderResponseMetaData":{"request":"84.70126567, 88.20093371","found":"1","results":"10","Point":{"pos":"84.701266 88.200934"}}},"featureMember":[{"GeoObject":{"metaDataProperty":{"GeocoderMetaData":{"kind":"hydro","text":"Северный Ледовитый Океан","precision":"other","AddressDetails":{"Country":{"Locality":{"Premise":{"PremiseName":"Северный Ледовитый Океан"}}}}}},"name":"Северный Ледовитый Океан","boundedBy":{"Envelope":{"lowerCorner":"-179.999988 67.919","upperCorner":"179.999988 89.99999"}},"Point":{"pos":"84.701266 88.200934"}}}]}}}',
                    // eslint-disable-next-line
                    geoObject = {"GeoObject":{"metaDataProperty":{"GeocoderMetaData":{"kind":"locality","text":"Россия, Москва","precision":"other","AddressDetails":{"Country":{"AddressLine":"Москва","CountryNameCode":"RU","CountryName":"Россия","AdministrativeArea":{"AdministrativeAreaName":"Центральный федеральный округ","SubAdministrativeArea":{"SubAdministrativeAreaName":"Москва","Locality":{"LocalityName":"Москва"}}}}}}},"description":"Россия","name":"Москва","boundedBy":{"Envelope":{"lowerCorner":"37.298509 55.490631","upperCorner":"37.967682 55.957565"}},"Point":{"pos":"37.619899 55.753676"}}},
                    results = options.results;

                var parsedData;
                if (results == 1 || request == '84.70126567, 88.20093371') {
                    parsedData = JSON.parse(dataOneResult).response;
                } else {
                    parsedData = JSON.parse(data).response;
                    if (options.results > 10) {
                        var i = options.results - 10;
                        while (i--) {
                            parsedData.GeoObjectCollection.featureMember.push(geoObject);
                        }
                    }
                }

                var collection = geoObjects(parsedData);
                if (!collection) {
                    deferred.reject({
                        message: 'Bad response'
                    });
                    return;
                }
                var meta = collection.properties.get('metaDataProperty').GeocoderResponseMetaData;
                if (options.skip) {
                    meta.skip = options.skip;
                }

                collection.each(function (geoObject) {
                    var meta = geoObject.properties.get('metaDataProperty').GeocoderMetaData,
                        arr = meta.text.split(',');
                    arr.length -= meta.kind == 'house' ? 2 : 1;
                    var desc = arr.join(',');
                    geoObject.properties.set({
                        description: desc,
                        text: meta.text,
                        balloonContentBody: '<h3>' + geoObject.properties.get('name') + '</h3>' +
                            '<p>' + desc + '</p>'
                    });
                });

                deferred.resolve({
                    geoObjects: collection,
                    metaData: {
                        geocoder: {
                            request: meta.request,
                            found: parseInt(meta.found, 10),
                            results: parseInt(meta.results, 10),
                            skip: meta.skip ? parseInt(meta.skip, 10) : 0,
                            suggest: meta.suggest
                        }
                    }
                });

                return deferred.promise();
            } else {
                return geocode(request, extend({}, options, { provider: 'yandex#map' }));
            }
        }
    };

    provide(geocodeProvider);
});
