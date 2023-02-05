import {MapMargin, MapSize} from 'types/map';
import {MapViewport} from 'app/components/body-component';
import * as ugcPlacemarkUtils from './ugc-placemark-utils';

const zoom = 16.88;
const mapViewport: MapViewport = {top: 0, bottom: 177};
const mapSize: MapSize = {width: 320, height: 568};
const mapMargin: MapMargin = [56, 50, 100, 10];

describe('Утилиты для UGC плейсмарки', () => {
    test('Получение точки, куда указывает плейсмарка', () => {
        expect(
            ugcPlacemarkUtils.getPlacemarkPoint({
                zoom,
                mapSize,
                mapViewport,
                mapMargin,
                mapLocation: {
                    center: [37.67278281199642, 55.76168783163346],
                    zoom: 16.88,
                    span: [0.003031444046882825, 0.0027084868539404283],
                    bounds: [
                        [37.671267089972986, 55.760333564584066],
                        [37.67429853401987, 55.763042051438006]
                    ]
                }
            })
        ).toEqual([37.67301600000005, 55.76212499997481]);
    });

    test('Получение центра карты для точки, куда указывает плейсмарка', () => {
        expect(
            ugcPlacemarkUtils.getCenterForPlacemark({
                zoom,
                mapSize,
                mapViewport,
                mapMargin,
                point: [37.673016, 55.762125],
                mapLocation: {
                    center: [37.67418428345935, 55.76092927768211],
                    zoom: 16.88,
                    span: [0.0030314440468544035, 0.002708539766274498],
                    bounds: [
                        [37.6726685614359, 55.75957498417631],
                        [37.67570000548275, 55.762283523942585]
                    ]
                }
            })
        ).toEqual([37.67278281199643, 55.76168783163977]);
    });

    test('Получение координат плейсмарка в браузере', () => {
        expect(
            ugcPlacemarkUtils.getPlacemarkPosition({
                mapSize,
                mapViewport
            })
        ).toEqual({top: 195.5, left: 160});
    });
});
