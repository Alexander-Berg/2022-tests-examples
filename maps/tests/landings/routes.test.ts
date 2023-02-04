import {landingWithTlds} from 'server/seourl/tests/test-utils';

describe('Routes landing', () => {
    describe('should return route landing by encoded thread uri with region', () => {
        landingWithTlds(
            () => '/213/moscow/routes/some_seoname/1234567890/',
            () => ({
                id: 'routes',
                region: {
                    regionId: 213,
                    seoname: 'moscow'
                },
                invalidSeoname: 'some_seoname',
                encodedUri: '1234567890'
            })
        );
    });

    describe('should return undefined by route id without region', () => {
        landingWithTlds(
            () => '/routes/some_seoname/',
            () => undefined
        );
    });

    describe('should return undefined by route id and thread id without region', () => {
        landingWithTlds(
            () => '/routes/some_seoname/1234567890/',
            () => undefined
        );
    });

    describe('should return undefined by route id, thread id and extra subvalue with region', () => {
        landingWithTlds(
            () => '/213/moscow/routes/some_seoname/1234567890/unknown',
            () => undefined
        );
    });
});
