import {landingWithTlds} from 'server/seourl/tests/test-utils';

describe('Home landing', () => {
    describe('should return home landing', () => {
        landingWithTlds(
            () => ['', '/', '/print', '/print/'],
            () => ({
                id: 'home'
            })
        );
    });

    describe('should return home landing with region', () => {
        landingWithTlds(
            () => ['/213/moscow', '/print/213/moscow'],
            () => ({
                id: 'home',
                region: {
                    regionId: 213,
                    seoname: 'moscow'
                }
            })
        );
    });

    describe('should return home landing with region for shortcut', () => {
        landingWithTlds(
            () => '/moscow',
            () => ({
                id: 'home',
                region: {
                    regionId: 213,
                    seoname: 'moscow'
                }
            })
        );
    });

    describe('should return undefined for malformed url with underscore', () => {
        landingWithTlds(
            () => ['/moscow_', '/_moscow', '/moscow_unknown', '/unknown_moscow'],
            () => undefined
        );
    });

    describe('should return undefined for unknown sublanding', () => {
        landingWithTlds(
            () => '/213/moscow/unknown',
            () => undefined
        );
    });

    describe('should return undefined for region id without name', () => {
        landingWithTlds(
            () => '/213/',
            () => undefined
        );
    });

    describe('should return undefined for constructor url', () => {
        landingWithTlds(
            () => ['/constructor/', '/services/constructor/'],
            () => undefined
        );
    });
});
