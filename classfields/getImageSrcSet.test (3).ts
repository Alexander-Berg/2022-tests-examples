/**
 * @jest-environment node
 */
import { IMAGE_MOCK_1 } from 'core/mocks/image.mock';

import { getImageSrcSet } from './getImageSrcSet';

it('должен правильно сформировать srcSet для всех размеров', () => {
    expect(getImageSrcSet(IMAGE_MOCK_1.sizes)).toBe(
        ' 338w, 439w, 571w, 650w, 845w, 1098w, 1252w, 1428w, 1600w, 1920w, 3840w'
    );
});
