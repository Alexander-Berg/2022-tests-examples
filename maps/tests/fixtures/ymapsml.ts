// https://sandbox.api.maps.yandex.net/examples/ru/2.1/ymapsml_polygon/data.xml
export const ymapsmlXml = `
<?xml version="1.0" encoding="utf-8"?>
<ymaps:ymaps xmlns:ymaps="http://maps.yandex.ru/ymaps/1.x"
             xmlns:repr="http://maps.yandex.ru/representation/1.x"
             xmlns:gml="http://www.opengis.net/gml"
             xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
             xsi:schemaLocation="http://maps.yandex.ru/schemas/ymaps/1.x/ymaps.xsd">
    <ymaps:GeoObjectCollection>
        <gml:featureMember>
            <ymaps:GeoObject>
                <gml:name>Многоугольник</gml:name>
                <gml:description>Внешняя граница многоугольника представляет собой замкнутую ломаную линию</gml:description>
                <gml:Polygon>
                    <gml:exterior>
                        <gml:LinearRing>
                            <gml:posList>34.320818 61.766513 34.339359 61.77562 34.353095 61.779849 34.359958 61.772369 34.320818 61.766513</gml:posList>
                        </gml:LinearRing>
                    </gml:exterior>
                </gml:Polygon>
            </ymaps:GeoObject>
        </gml:featureMember>
    </ymaps:GeoObjectCollection>
</ymaps:ymaps>
`;

export const ymapsmlResponse = {"response":{"ymaps":{"schemaLocation":"http://maps.yandex.ru/schemas/ymaps/1.x/ymaps.xsd","GeoObjectCollection":{"featureMembers":[{"GeoObject":{"name":"Многоугольник","description":"Внешняя граница многоугольника представляет собой замкнутую ломаную линию","Polygon":{"exterior":{"polylod":{"polyline":"srELAnF7rgNtSAAAkyMAAKg1AACFEAAAzxoAAMji__8cZ___IOn__w==","levels":"","maxlevel":""}}}}}]}}}};
