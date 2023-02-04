import {landingWithTlds} from 'server/seourl/tests/test-utils';

describe('Parking landing', () => {
    describe('should return parking landing without region', () => {
        landingWithTlds(
            () => '/parking',
            () => ({
                id: 'layer',
                name: 'parking'
            })
        );
    });

    describe('should return parking landing with region', () => {
        landingWithTlds(
            () => '/213/moscow/parking',
            () => ({
                id: 'layer',
                name: 'parking',
                region: {
                    regionId: 213,
                    seoname: 'moscow'
                }
            })
        );
    });

    describe('should return undefined for region shortcut', () => {
        landingWithTlds(
            () => '/moscow_parking',
            () => undefined
        );
    });

    describe('should return undefined with extra sublanding', () => {
        landingWithTlds(
            () => '/213/moscow/parking/unknown',
            () => undefined
        );
    });

    describe('should return undefined with unknown region shortcut', () => {
        landingWithTlds(
            () => '/unknown_parking',
            () => undefined
        );
    });
});
