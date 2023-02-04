jest.mock('auto-core/server/helpers/makeBlurPreviewData', () => {
    return () => 'cat-preview';
});

import type { ExteriorPoi } from '@vertis/schema-registry/ts-types-snake/auto/panoramas/poi_model';

import type { PanoramaHotSpot } from 'auto-core/react/dataDomain/panoramaHotSpots/types';
import { panoramaHotSpotMock } from 'auto-core/react/dataDomain/panoramaHotSpots/mocks';
import getHotSpotPhotoPreview from 'auto-core/react/dataDomain/panoramaHotSpots/helpers/getHotSpotPhotoPreview';

import preparer from './exteriorPanoramaPoi';

it('заменить превью фоток, если они есть', () => {
    const spot = panoramaHotSpotMock.withImage('my-cat').value();
    const result = preparer(spot as unknown as ExteriorPoi) as unknown as PanoramaHotSpot;

    expect(result).not.toEqual(spot);
    expect(getHotSpotPhotoPreview(result)).toBe('cat-preview');
});

it('ничего не будет менять, если превью фоток нет', () => {
    const spot = panoramaHotSpotMock.value();
    const result = preparer(spot as unknown as ExteriorPoi) as unknown as PanoramaHotSpot;

    expect(result).toEqual(spot);
});
