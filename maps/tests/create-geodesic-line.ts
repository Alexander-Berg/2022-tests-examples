import {expect} from 'chai';
import {createGeodesicLineString} from '../src/geo/create-geodesic-line';

describe('createGeodesicLineString', () => {
    it('should handle 90 degree latitude', () => {
        expect(() => {
            createGeodesicLineString([[37.609439, 55], [-93.09516299999999, 89.9999999]]);
            createGeodesicLineString([[37.609439, 55], [-93.09516299999999, -89.9999999]]);
        }).to.not.throw();
    });
});
