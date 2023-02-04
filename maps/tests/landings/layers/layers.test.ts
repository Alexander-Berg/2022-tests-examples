import geolocation from 'server/lib/geolocation';
import {getLayers} from 'server/seourl/layers';

describe('Get layers', () => {
    it('with city', () => {
        expect(getLayers(geolocation.GEOBASE_TYPES.city)).toEqual([
            'traffic',
            'streetview',
            'satellite',
            'routes',
            'transport',
            'hybrid',
            'parking',
            'photo'
        ]);
    });

    it('with country', () => {
        expect(getLayers(geolocation.GEOBASE_TYPES.country)).toEqual([
            'streetview',
            'satellite',
            'transport',
            'hybrid',
            'photo'
        ]);
    });

    it('without region', () => {
        expect(getLayers()).toEqual(['traffic', 'routes', 'parking']);
    });
});
