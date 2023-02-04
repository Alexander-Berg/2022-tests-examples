import {landingWithTlds} from 'server/seourl/tests/test-utils';

describe('Photo landing', () => {
    describe('should return photo landing with region', () => {
        landingWithTlds(
            () => '/213/moscow/photo',
            () => ({
                id: 'layer',
                name: 'photo',
                region: {
                    regionId: 213,
                    seoname: 'moscow'
                }
            })
        );
    });

    describe('should return undefined for region shortcut', () => {
        landingWithTlds(
            () => '/moscow_photo',
            () => undefined
        );
    });

    describe('should return undefined without region', () => {
        landingWithTlds(
            () => '/photo',
            () => undefined
        );
    });

    describe('should return undefined with extra sublanding', () => {
        landingWithTlds(
            () => '/213/moscow/photo/unknown',
            () => undefined
        );
    });

    describe('should return undefined with unknown region shortcut', () => {
        landingWithTlds(
            () => '/unknown_photo',
            () => undefined
        );
    });
});
