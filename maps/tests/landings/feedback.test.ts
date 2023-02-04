import {landingWithTlds} from 'server/seourl/tests/test-utils';

describe('Feedback landing', () => {
    landingWithTlds(
        () => '/feedback',
        () => ({
            id: 'feedback'
        })
    );

    describe('should return undefined with extra sublanding', () => {
        landingWithTlds(
            () => '/feedback/unknown',
            () => undefined
        );
    });

    describe('should return undefined with region', () => {
        landingWithTlds(
            () => '/213/moscow/feedback',
            () => undefined
        );
    });

    describe('should return undefined with malformed prefix', () => {
        landingWithTlds(
            () => '/foo/bar/feedback',
            () => undefined
        );
    });
});
