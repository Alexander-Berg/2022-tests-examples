import {landingWithTlds} from 'server/seourl/tests/test-utils';

describe('Actual landing', () => {
    describe('should return actual landing', () => {
        landingWithTlds(
            () => ['/actual', '/corona'],
            () => ({
                id: 'actual'
            })
        );
    });

    describe('should return undefined for extra sublanding', () => {
        landingWithTlds(
            () => ['/actual/foo', '/corona/foo'],
            () => undefined
        );
    });

    describe('should return undefined with region', () => {
        landingWithTlds(
            () => ['/213/moscow/actual', '/213/moscow/corona'],
            () => undefined
        );
    });
});
