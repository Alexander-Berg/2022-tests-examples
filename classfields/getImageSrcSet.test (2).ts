import type { Image } from './getImageSrcData';
import getImageSrcSet from './getImageSrcSet';

it('getImageSrc вернет строку для обычного случая', () => {
    const image = {
        sizes: {
            '150x100': 'https://1',
            '300x200': 'https://2',
            '450x300': 'https://3',
        },
    } as unknown as Image;
    expect(getImageSrcSet(image, 150)).toEqual('https://2 1x');
});

it('getImageSrc вернет undefined для плохого формата', () => {
    const image = { sizes: {} } as unknown as Image;
    expect(getImageSrcSet(image, 150)).toBeUndefined();
});
