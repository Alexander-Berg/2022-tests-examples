import {landingWithTlds} from 'server/seourl/tests/test-utils';

describe('Transport landing', () => {
    describe('should return transport landing with region', () => {
        landingWithTlds(
            () => '/213/moscow/transport',
            () => ({
                id: 'layer',
                name: 'transport',
                region: {
                    regionId: 213,
                    seoname: 'moscow'
                }
            })
        );
    });

    describe('should return undefined for region shortcut', () => {
        landingWithTlds(
            () => '/moscow_transport',
            () => undefined
        );
    });

    describe('should return undefined without region', () => {
        landingWithTlds(
            () => '/transport',
            () => undefined
        );
    });

    describe('should return undefined with unknown region shortcut', () => {
        landingWithTlds(
            () => '/unknown_transport',
            () => undefined
        );
    });
});
