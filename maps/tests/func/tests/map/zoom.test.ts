import qs from 'qs';
import {openPage} from '../../utils/commands';
import {MAX_ZOOM} from '../../../../src/client/common/utils/map-utils';

describe('Zoom', () => {
    test('should be restricted by a custom maximum value', async () => {
        await openPage({useAuth: true, query: {z: MAX_ZOOM}});
        const expected = String(MAX_ZOOM);
        const actual = qs.parse(new URL(page.url()).search, {ignoreQueryPrefix: true}).z;
        expect(actual).toEqual(expected);
    });
});
