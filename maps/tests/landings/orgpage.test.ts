import {landingWithTlds} from 'server/seourl/tests/test-utils';
import {SUBLANDINGS} from 'types/sublanding';

describe('Orgpage landing', () => {
    describe('should return orgpage landing for link', () => {
        landingWithTlds(
            () => ['/org/23423434', '/print/org/23423434'],
            () => ({
                id: 'orgpage',
                orgId: 23423434
            })
        );
    });

    describe('should return orgpage landing for link with seoname', () => {
        landingWithTlds(
            () => ['/org/seoname_org/23423434', '/print/org/seoname_org/23423434'],
            () => ({
                id: 'orgpage',
                orgId: 23423434,
                seoname: 'seoname_org'
            })
        );
    });

    const AVAILABLE_SUBLANDINGS = SUBLANDINGS.orgpage;
    AVAILABLE_SUBLANDINGS.forEach((sublanding) => {
        describe(`should return orgpage landing with ${sublanding} sublanding`, () => {
            landingWithTlds(
                () => [`/org/seoname_org/23423434/${sublanding}`, `/print/org/seoname_org/23423434/${sublanding}`],
                () => ({
                    id: 'orgpage',
                    orgId: 23423434,
                    seoname: 'seoname_org',
                    sublanding
                })
            );
            landingWithTlds(
                () => [`/org/23423434/${sublanding}`, `/print/org/23423434/${sublanding}`],
                () => ({
                    id: 'orgpage',
                    orgId: 23423434,
                    sublanding
                })
            );
        });
    });

    describe('should return undefined for link with missed orgpage id', () => {
        landingWithTlds(
            () => '/org',
            () => undefined
        );
    });

    describe('should return undefined for link with unknown prefixes', () => {
        landingWithTlds(
            () => '/foo/bar/org/seoname/123456',
            () => undefined
        );
    });

    describe('should return undefined for link with region', () => {
        landingWithTlds(
            () => ['/213/moscow/org/seoname/123456', '/213/moscow/org/123456'],
            () => undefined
        );
    });

    describe('should return undefined for link with unknown sub-sublandings', () => {
        landingWithTlds(
            () => [
                '/org/seoname_org/23423434/menu/unknown',
                '/org/seoname_org/23423434/reviews/unknown',
                '/org/seoname_org/23423434/inside/unknown'
            ],
            () => undefined
        );
    });

    describe('should return undefined for link with unknown sublanding', () => {
        landingWithTlds(
            () => [
                '/org/seoname_org/23423434/unknown/',
                '/print/org/seoname_org/23423434/unknown/',
                '/org/23423434/unknown/',
                '/print/org/23423434/unknown/'
            ],
            () => undefined
        );
    });
});
