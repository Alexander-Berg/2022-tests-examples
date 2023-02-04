import {landingWithTlds} from 'server/seourl/tests/test-utils';

describe('Coronavirus landing', () => {
    describe('should return coronavirus landing', () => {
        landingWithTlds(
            () => ['/covid19', '/print/covid19'],
            () => ({
                id: 'coronavirus'
            })
        );
    });

    describe('should return coronavirus landing with isolation', () => {
        landingWithTlds(
            () => ['/covid19/isolation', '/print/covid19/isolation'],
            () => ({
                id: 'coronavirus'
            })
        );
    });

    describe('should return undefined with region', () => {
        landingWithTlds(
            () => '/213/moscow/covid19',
            () => undefined
        );
    });

    describe('should return undefined for wrong sublanding', () => {
        landingWithTlds(
            () => '/covid19/foo',
            () => undefined
        );
    });

    describe('should return undefined for extra sublanding', () => {
        landingWithTlds(
            () => '/covid19/isolation/foo',
            () => undefined
        );
    });
});
