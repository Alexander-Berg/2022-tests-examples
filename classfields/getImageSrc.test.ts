import getImageSrc from './getImageSrc';
import type { Image } from './getImageSrc';

it('getImageSrc вернет строку для обычного случая', () => {
    const image = {
        sizes: {
            '100x100': 'https://1',
            '200x200': 'https://2',
        },
    } as unknown as Image;
    expect(getImageSrc(image, 150)).toEqual('https://2');
});

it('getImageSrc вернет undefined для плохого формата', () => {
    const image = { sizes: {} } as unknown as Image;
    expect(getImageSrc(image, 150)).toBeUndefined();
});
