import {landingWithTlds} from 'server/seourl/tests/test-utils';

describe('Catalog landing', () => {
    describe('should return catalog landing for link', () => {
        landingWithTlds(
            () => ['/213/moscow/catalog', '/print/213/moscow/catalog'],
            () => ({
                id: 'catalog',
                region: {
                    regionId: 213,
                    seoname: 'moscow'
                }
            })
        );
    });

    describe('should return catalog landing with group id for link', () => {
        landingWithTlds(
            () => ['/213/moscow/catalog/123', '/print/213/moscow/catalog/123'],
            () => ({
                id: 'catalog',
                rubricId: '123',
                region: {
                    regionId: 213,
                    seoname: 'moscow'
                }
            })
        );
    });

    describe('should return undefined for extra sublanding', () => {
        landingWithTlds(
            () => ['/213/moscow/catalog/123/foo', '/print/213/moscow/catalog/123/foo'],
            () => undefined
        );
    });

    describe('should return undefined for no region', () => {
        landingWithTlds(
            () => ['/catalog', '/print/catalog', '/catalog/123', '/print/catalog/123'],
            () => undefined
        );
    });
});
