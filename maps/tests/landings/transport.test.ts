import {landingWithTlds} from 'server/seourl/tests/test-utils';

describe('Transport landing', () => {
    describe('should return transport landing', () => {
        landingWithTlds(
            () => '/213/moscow/transport/buses/',
            () => ({
                id: 'transport',
                region: {
                    regionId: 213,
                    seoname: 'moscow'
                },
                transport: 'buses'
            })
        );
    });

    describe('should return undefined without region', () => {
        landingWithTlds(
            () => '/transport/buses',
            () => undefined
        );
    });
});
