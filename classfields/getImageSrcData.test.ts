import type { Image } from './getImageSrcData';
import getImageSrcData from './getImageSrcData';

describe('должен вернуть данные для общего случая картинки', () => {
    const IMAGE = { sizes: {
        '100x100': 'https://1',
        '200x200': 'https://2',
        '300x300': 'https://3',
        '400x400': 'https://4',
        '500x500': 'https://5',
        '600x600': 'https://6',
        '700x700': 'https://7',
        '800x800': 'https://8',
        '900x900': 'https://9',
    } } as unknown as Image;

    it('чуть меньше минимального', () => {
        const result = getImageSrcData(IMAGE, 90);
        const expected = {
            '1x': 'https://1',
            '2x': 'https://2',
            '3x': 'https://3',
        };
        expect(result).toEqual(expected);
    });

    it('чуть больше минимального', () => {
        const result = getImageSrcData(IMAGE, 120);
        const expected = {
            '1x': 'https://2',
            '2x': 'https://4',
            '3x': 'https://6',
        };
        expect(result).toEqual(expected);
    });

    it('довольно большая', () => {
        const result = getImageSrcData(IMAGE, 400);
        const expected = {
            '1x': 'https://4',
            '2x': 'https://8',
        };
        expect(result).toEqual(expected);
    });

    it('хотим слишком большую, отдаем самую большую', () => {
        const result = getImageSrcData(IMAGE, 1000);
        const expected = {
            '1x': 'https://9',
        };
        expect(result).toEqual(expected);
    });

    it('если ключи sizes вообще не в том формате, увидим только последний вариант', () => {
        const image = { sizes: { haha: 'https://lol', hehe: 'https://lmao' } } as unknown as Image;
        const result = getImageSrcData(image);
        const expected = {
            '1x': 'https://lmao',
        };
        expect(result).toEqual(expected);
    });

    it('если картинка вообще не в том формате, получим пустой объект', () => {
        const image = {} as unknown as Image;
        const result = getImageSrcData(image as unknown as Image);
        expect(result).toEqual({});
    });
});
