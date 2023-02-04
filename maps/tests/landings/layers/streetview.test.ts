import {Tld} from 'types/locale';
import {getLocalLayerName} from 'utils/urls';
import {landingWithTlds, withTlds} from 'server/seourl/tests/test-utils';

// TODO: Проверить список
const STREETVIEW_ALIASES: Record<Tld, string> = {
    ru: 'panorama',
    ua: 'panorama',
    ge: 'panorama',
    uz: 'panorama',
    by: 'panorama',
    kz: 'panorama',
    kg: 'panorama',
    lv: 'panorama',
    lt: 'panorama',
    tj: 'panorama',
    tm: 'panorama',
    md: 'panorama',
    ee: 'panorama',
    com: 'streetview',
    eu: 'streetview',
    fr: 'streetview',
    tr: 'streetview',
    il: 'streetview',
    az: 'streetview',
    fi: 'streetview',
    pl: 'streetview'
};

describe('Streetview landing', () => {
    describe('should return streetview landing for local layer name', () => {
        landingWithTlds(
            (tld) => `/213/moscow/${STREETVIEW_ALIASES[tld]}`,
            () => ({
                id: 'layer',
                name: 'streetview',
                region: {
                    regionId: 213,
                    seoname: 'moscow'
                }
            })
        );
    });

    describe('should return undefined for region shortcut', () => {
        landingWithTlds(
            () => ['/moscow_streetview', '/moscow_panorama'],
            () => undefined
        );
    });

    describe('should return correct local alias name', () => {
        withTlds(
            (tld) => [getLocalLayerName('streetview', tld)],
            (tld) => STREETVIEW_ALIASES[tld]
        );
    });

    describe('should return undefined for extra sublanding', () => {
        landingWithTlds(
            (tld) => `/213/moscow/${STREETVIEW_ALIASES[tld]}/unknown`,
            () => undefined
        );
    });

    describe('should return undefined without region', () => {
        landingWithTlds(
            (tld) => `/${STREETVIEW_ALIASES[tld]}`,
            () => undefined
        );
    });
});
