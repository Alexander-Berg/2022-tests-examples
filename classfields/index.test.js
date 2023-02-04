const cardImageGallery = require('./index');
const getValidInteriorPanoramaData = require('./getValidInteriorPanoramaData');

const cloneOfferWithHelpers = require('autoru-frontend/mockData/state/helpers/offer/cloneOfferWithHelpers');

let offer;
beforeEach(() => {
    offer = {
        state: {
            image_urls: [
                { sizes: { '1200x900n': 'img1' } },
                { sizes: { '1200x900n': 'img2' } },
                { sizes: { '1200x900n': 'img3' } },
            ],
        },
    };
});

describe('порядок galleryItems', () => {
    it('должен расположить фотки в том же порядке', () => {
        expect(cardImageGallery(offer).galleryItems).toEqual([
            { sizes: { '1200x900n': 'img1' }, type: 'IMAGE' },
            { sizes: { '1200x900n': 'img2' }, type: 'IMAGE' },
            { sizes: { '1200x900n': 'img3' }, type: 'IMAGE' },
        ]);
    });

    describe('есть панорама', () => {
        beforeEach(() => {
            offer = cloneOfferWithHelpers(offer).withPanoramaExterior();
        });

        it('должен расположить: внешняя панорама, внутренняя панорама, видео, фотки', () => {
            offer = offer.withPanoramaInterior().withYoutubeVideo().value();

            expect(cardImageGallery(offer).galleryItems).toEqual([
                { ...offer.state.external_panorama.published, type: 'PANORAMA_EXTERIOR' },
                getValidInteriorPanoramaData(offer),
                {
                    type: 'VIDEO',
                    provider: 'YOUTUBE',
                    original: '//i.ytimg.com/vi/ytvideo/hqdefault.jpg',
                    thumbnail: '//i.ytimg.com/vi/ytvideo/mqdefault.jpg',
                    video: '//www.youtube.com/embed/ytvideo?enablejsapi=1',
                },
                { sizes: { '1200x900n': 'img1' }, type: 'IMAGE' },
                { sizes: { '1200x900n': 'img2' }, type: 'IMAGE' },
                { sizes: { '1200x900n': 'img3' }, type: 'IMAGE' },
            ]);
        });

        it('должен расположить: панорама, спинкар, видео, фотки', () => {
            offer = offer.withSpincar().withYoutubeVideo().value();

            expect(cardImageGallery(offer).galleryItems).toEqual([
                { ...offer.state.external_panorama.published, type: 'PANORAMA_EXTERIOR' },
                { sizes: { '1200x900n': 'img2' }, spincar: 'spincar_exterior_url', type: 'SPINCAR' },
                {
                    type: 'VIDEO',
                    provider: 'YOUTUBE',
                    original: '//i.ytimg.com/vi/ytvideo/hqdefault.jpg',
                    thumbnail: '//i.ytimg.com/vi/ytvideo/mqdefault.jpg',
                    video: '//www.youtube.com/embed/ytvideo?enablejsapi=1',
                },
                { sizes: { '1200x900n': 'img1' }, type: 'IMAGE' },
                { sizes: { '1200x900n': 'img2' }, type: 'IMAGE' },
                { sizes: { '1200x900n': 'img3' }, type: 'IMAGE' },
            ]);
        });
    });

    describe('нет панорамы', () => {
        beforeEach(() => {
            offer = cloneOfferWithHelpers(offer);
        });

        it('должен расположить: фотка, внутренняя панорама, видео, фотки', () => {
            offer = offer.withPanoramaInterior().withYoutubeVideo().value();

            expect(cardImageGallery(offer).galleryItems).toEqual([
                { sizes: { '1200x900n': 'img1' }, type: 'IMAGE' },
                getValidInteriorPanoramaData(offer),
                {
                    type: 'VIDEO',
                    provider: 'YOUTUBE',
                    original: '//i.ytimg.com/vi/ytvideo/hqdefault.jpg',
                    thumbnail: '//i.ytimg.com/vi/ytvideo/mqdefault.jpg',
                    video: '//www.youtube.com/embed/ytvideo?enablejsapi=1',
                },
                { sizes: { '1200x900n': 'img2' }, type: 'IMAGE' },
                { sizes: { '1200x900n': 'img3' }, type: 'IMAGE' },
            ]);
        });

        it('должен расположить: фотка, видео, спинкар, фотки', () => {
            offer = offer.withSpincar().withYoutubeVideo().value();

            expect(cardImageGallery(offer).galleryItems).toEqual([
                { sizes: { '1200x900n': 'img1' }, type: 'IMAGE' },
                {
                    type: 'VIDEO',
                    provider: 'YOUTUBE',
                    original: '//i.ytimg.com/vi/ytvideo/hqdefault.jpg',
                    thumbnail: '//i.ytimg.com/vi/ytvideo/mqdefault.jpg',
                    video: '//www.youtube.com/embed/ytvideo?enablejsapi=1',
                },
                { sizes: { '1200x900n': 'img2' }, spincar: 'spincar_exterior_url', type: 'SPINCAR' },
                { sizes: { '1200x900n': 'img2' }, type: 'IMAGE' },
                { sizes: { '1200x900n': 'img3' }, type: 'IMAGE' },
            ]);
        });
    });

    describe('бейджи', () => {
        const offerWithOrigImg = {
            state: {
                image_urls: [
                    { sizes: { '1200x900n': 'img1' } },
                    { sizes: { '1200x900n': 'img2' }, is_hd: true },
                    { sizes: { '1200x900n': 'img3' } },
                ],
            },
        };

        it('должен добавить HD', () => {
            offer = cloneOfferWithHelpers(offerWithOrigImg).withStatus('ACTIVE').value();
            offer.documents = { vin_resolution: 'OK' };

            expect(cardImageGallery(offer).badges.hd).toBeTruthy();
        });

        it('не должен добавить HD, если у оффера нет отчёта', () => {
            offer = cloneOfferWithHelpers(offerWithOrigImg).value();

            expect(cardImageGallery(offer).badges.hd).toBeFalsy();
        });

        it('не должен добавить HD, если у оффера нет hd-фоток', () => {
            offer = cloneOfferWithHelpers(offer).withTags([ 'vin_offers_history' ]).value();

            expect(cardImageGallery(offer).badges.hd).toBeFalsy();
        });
    });
});
