import { REGIONS } from 'realty-core/app/lib/constants';

import { Rgid } from 'types/friendlyUrls/common';

import { addRegionToRgid } from './addRegionToRgid';

const TEST_CASES: Array<{ value: Rgid; result: Rgid }> = [
    { value: REGIONS.MOSCOW as Rgid, result: REGIONS.MOSCOW_AND_MOSCOW_OBLAST as Rgid },
    { value: REGIONS.SPB as Rgid, result: REGIONS.SPB_AND_LENINGRAD_OBLAST as Rgid },
];

it('возвращает ргид региона с его областью', () => {
    for (const testCase of TEST_CASES) {
        const result = addRegionToRgid(testCase.value);

        expect(result).toBe(testCase.result);
    }
});
