import {landingWithTlds} from 'server/seourl/tests/test-utils';

describe('Toponym landing', () => {
    describe('should return toponym landing', () => {
        landingWithTlds(
            () => ['/213/moscow/geo/rayon_khamovniki/53211698/'],
            () => ({
                id: 'toponym',
                region: {
                    regionId: 213,
                    seoname: 'moscow'
                },
                geo: {
                    id: 53211698,
                    seoname: 'rayon_khamovniki'
                }
            })
        );
    });

    describe('should return toponym landing without region', () => {
        landingWithTlds(
            () => '/geo/uryupinsk/53164456/',
            () => ({
                id: 'toponym',
                geo: {
                    id: 53164456,
                    seoname: 'uryupinsk'
                }
            })
        );
    });

    describe('should return undefined for unknown sublanding', () => {
        landingWithTlds(
            () => ['/213/moscow/geo/unknown', '/213/moscow/geo/rayon_khamovniki/53211698/unknown'],
            () => undefined
        );
    });
});
