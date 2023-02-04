import { REGIONS } from 'realty-core/app/lib/constants';

import { Rgid } from 'types/friendlyUrls/common';

import { addRegionToRgidIfExists } from '../getValidRgid';

describe('addRegionToRgidIfExists', () => {
    it('возвращает МСК + МО, если пришёл ргид москвы', () => {
        const result = addRegionToRgidIfExists(REGIONS.MOSCOW as Rgid);

        expect(result).toBe(REGIONS.MOSCOW_AND_MOSCOW_OBLAST);
    });

    it('возвращает Брянска, если пришёл ргид Брянска', () => {
        const result = addRegionToRgidIfExists(REGIONS.BRYANSK as Rgid);

        expect(result).toBe(REGIONS.BRYANSK);
    });

    it('возвращает МСК + МО, если пришёл ргид МСК + МО', () => {
        const result = addRegionToRgidIfExists(REGIONS.MOSCOW_AND_MOSCOW_OBLAST as Rgid);

        expect(result).toBe(REGIONS.MOSCOW_AND_MOSCOW_OBLAST);
    });
});
