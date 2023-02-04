import {landingWithTlds} from 'server/seourl/tests/test-utils';

describe('Routes landing', () => {
    describe('should return routes landing without region', () => {
        landingWithTlds(
            () => '/routes',
            () => ({
                id: 'layer',
                name: 'routes'
            })
        );
    });

    describe('should return routes landing with region', () => {
        landingWithTlds(
            () => '/213/moscow/routes',
            () => ({
                id: 'layer',
                name: 'routes',
                region: {
                    regionId: 213,
                    seoname: 'moscow'
                }
            })
        );
    });

    describe('should return routes landing for region shortcut', () => {
        landingWithTlds(
            () => '/moscow_routes',
            () => undefined
        );
    });

    describe('should return undefined with unknown region shortcut', () => {
        landingWithTlds(
            () => '/unknown_routes',
            () => undefined
        );
    });
});
