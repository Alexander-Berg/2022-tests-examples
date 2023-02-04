import qs from 'qs';
import {openPage, click} from '../../utils/commands';
import {MIN_ZOOM} from '../../../../src/client/common/utils/map-utils';

const SELECTORS = {
    zoomPlusButton: '.icon_type_zoom-in'
};

describe('Map action correction', () => {
    test('should not let open map with too small zoom', async () => {
        await openPage({useAuth: true, query: {z: MIN_ZOOM - 1}});
        const actualZoom = Number(qs.parse(new URL(page.url()).search, {ignoreQueryPrefix: true}).z);
        expect(actualZoom).toBeGreaterThan(MIN_ZOOM - 1);
    });

    test('should correct map view', async () => {
        await openPage({useAuth: true, query: {z: MIN_ZOOM, ll: '0%2C90'}});
        await click(SELECTORS.zoomPlusButton);
        const actualCenter = qs.parse(new URL(page.url()).search, {ignoreQueryPrefix: true}).ll;
        expect(actualCenter).not.toEqual('0,90');
    });
});
