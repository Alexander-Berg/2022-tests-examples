import {Tld} from 'types/locale';
import {getLocalLayerName} from 'utils/urls';
import {landingWithTlds, withTlds} from 'server/seourl/tests/test-utils';

// TODO: Проверить список
const SATELLITE_ALIASES: Record<Tld, string> = {
    ru: 'sputnik',
    ua: 'sputnik',
    ge: 'sputnik',
    uz: 'sputnik',
    by: 'sputnik',
    kz: 'sputnik',
    kg: 'sputnik',
    lv: 'sputnik',
    lt: 'sputnik',
    tj: 'sputnik',
    tm: 'sputnik',
    md: 'sputnik',
    ee: 'sputnik',
    com: 'satellite',
    eu: 'satellite',
    fr: 'satellite',
    tr: 'satellite',
    il: 'satellite',
    az: 'satellite',
    fi: 'satellite',
    pl: 'satellite'
};

describe('Satellite landing', () => {
    describe('should return satellite landing', () => {
        landingWithTlds(
            (tld) => `/213/moscow/${SATELLITE_ALIASES[tld]}`,
            () => ({
                id: 'layer',
                name: 'satellite',
                region: {
                    regionId: 213,
                    seoname: 'moscow'
                }
            })
        );
    });

    describe('should return satellite landing with region shortcut', () => {
        landingWithTlds(
            () => ['/moscow_sputnik', '/moscow_satellite'],
            () => ({
                id: 'layer',
                name: 'satellite',
                region: {
                    regionId: 213,
                    seoname: 'moscow'
                }
            })
        );
    });

    describe('should return correct local alias name', () => {
        withTlds(
            (tld) => [getLocalLayerName('satellite', tld)],
            (tld) => SATELLITE_ALIASES[tld]
        );
    });

    describe('should return undefined for unknown region name shortcut', () => {
        landingWithTlds(
            () => '/unknown_satellite',
            () => undefined
        );
    });

    describe('should return undefined for common layer name with sublanding', () => {
        landingWithTlds(
            (tld) => `/213/moscow/${SATELLITE_ALIASES[tld]}/unknown`,
            () => undefined
        );
    });
});
