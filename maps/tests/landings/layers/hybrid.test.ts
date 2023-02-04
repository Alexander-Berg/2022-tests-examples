import {landingWithTlds} from 'server/seourl/tests/test-utils';

describe('Hybrid landing', () => {
    describe('should return hybrid landing with region', () => {
        landingWithTlds(
            () => '/213/moscow/hybrid',
            () => ({
                id: 'layer',
                name: 'hybrid',
                region: {
                    regionId: 213,
                    seoname: 'moscow'
                }
            })
        );
    });

    describe('should return hybrid landing with region shortcut', () => {
        landingWithTlds(
            () => '/moscow_hybrid',
            () => ({
                id: 'layer',
                name: 'hybrid',
                region: {
                    regionId: 213,
                    seoname: 'moscow'
                }
            })
        );
    });

    describe('should return undefined without region', () => {
        landingWithTlds(
            () => '/hybrid',
            () => undefined
        );
    });

    describe('should return undefined with extra sublanding', () => {
        landingWithTlds(
            () => '/213/moscow/hybrid/unknown',
            () => undefined
        );
    });

    describe('should return undefined with unknown region shortcut', () => {
        landingWithTlds(
            () => '/unknown_hybrid',
            () => undefined
        );
    });
});
