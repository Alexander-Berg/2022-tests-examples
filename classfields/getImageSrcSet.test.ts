import offerMock from 'auto-core/react/dataDomain/card/mocks/card.cars.mock';

import getImageSrcSet from './getImageSrcSet';

const imageSizes = offerMock.state!.image_urls[0];

describe('должен правильно сформировать srcSet для набора размеров изображения', () => {
    it('если не передан набор изображений, отдаст все', () => {
        expect(getImageSrcSet(imageSizes))
            .toEqual('picture-of-cat 120w,picture-of-cat 320w,picture-of-cat 456w,picture-of-cat 1200w');
    });

    it('если передан набор изображений, отдаст только их', () => {
        expect(getImageSrcSet(imageSizes, [ '456x342n', '1200x900' ]))
            .toEqual('picture-of-cat 456w,picture-of-cat 1200w');
    });

    it('если передан набор изображений, но урлы есть не для всех, отдаст только те что есть', () => {
        expect(getImageSrcSet(imageSizes, [ '456x342n', '832x624', '1200x900' ]))
            .toEqual('picture-of-cat 456w,picture-of-cat 1200w');
    });

    it('если передан коллбек, составит srcSet со всеми имеющимися изображениями, удовлетворяющими условию', () => {
        expect(getImageSrcSet(imageSizes, ({ width }) => width > 300))
            .toEqual('picture-of-cat 320w,picture-of-cat 456w,picture-of-cat 1200w');
    });

    it('если передан набор изображений, размеры которых дублируются, отфильтрует дубли', () => {
        expect(getImageSrcSet(imageSizes, [ '456x342', '456x342n' ]))
            .toEqual('picture-of-cat 456w');
    });

    it('если передан набор изображений, не упорядоченных по размеру, отсортирует итоговый srcSet', () => {
        expect(getImageSrcSet(imageSizes, [ '1200x900', '320x240', '456x342n' ]))
            .toEqual('picture-of-cat 320w,picture-of-cat 456w,picture-of-cat 1200w');
    });
});
