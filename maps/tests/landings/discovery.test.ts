import {landingWithTlds} from 'server/seourl/tests/test-utils';

describe('Discovery landing', () => {
    describe('should return discovery landing without region', () => {
        landingWithTlds(
            () => ['/discovery/park-zaryadie', '/print/discovery/park-zaryadie'],
            () => ({
                id: 'discovery',
                alias: 'park-zaryadie'
            })
        );
    });

    describe('should return undefined with missed name', () => {
        landingWithTlds(
            () => '/discovery',
            () => undefined
        );
    });

    describe('should return undefined with extra sublanding', () => {
        landingWithTlds(
            () => '/discovery/park-zaryadie/unknown',
            () => undefined
        );
    });

    describe('should return undefined with region', () => {
        landingWithTlds(
            () => '/213/moscow/discovery/park-zaryadie',
            () => undefined
        );
    });

    describe('should return undefined with malformed prefix', () => {
        landingWithTlds(
            () => '/foo/bar/discovery/park-zaryadie',
            () => undefined
        );
    });
});
