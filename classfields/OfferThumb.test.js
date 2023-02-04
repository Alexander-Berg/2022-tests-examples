const React = require('react');
const { shallow } = require('enzyme');

const OfferThumb = require('./OfferThumb');
const cloneOfferWithHelpers = require('autoru-frontend/mockData/state/helpers/offer/cloneOfferWithHelpers');
const cardMock = require('auto-core/react/dataDomain/card/mocks/card.cars.mock');

const { imgs } = require('./mock');

let offer;

beforeEach(() => {
    offer = cloneOfferWithHelpers(cardMock).withImages(imgs);
});

it('при наличии внутренней панорамы и отсутствии внешней подмешает превью первой к картинкам', () => {
    offer = offer.withPanoramaInterior().value();
    const wrapper = shallow(
        <OfferThumb
            hasExtended={ true }
            url="/"
            offer={ offer }
        />,
    ).dive();

    const brazzers = wrapper.find('Brazzers');
    expect(brazzers.prop('images')).toHaveLength(4);
    expect(brazzers.prop('count')).toBe(4);
    // eslint-disable-next-line max-len
    expect(brazzers.prop('images')[1].url).toBe('https://autoru-panorama-internal.s3.mdst.yandex.net/interior/artifacts/403/650/4036500412-1595961776760-KD1uu/preview/320x240.jpg');
});

it('при наличии и внешней и внутренней панорамы, нарисует только внешнюю', () => {
    offer = offer.withPanoramaExterior().withPanoramaInterior().value();

    const wrapper = shallow(
        <OfferThumb
            hasExtended={ true }
            url="/"
            offer={ offer }
        />,
    ).dive();

    const brazzers = wrapper.find('Brazzers');
    expect(brazzers.isEmptyRender()).toBe(true);

    const offerPanorama = wrapper.find('OfferPanorama');
    expect(offerPanorama.isEmptyRender()).toBe(false);
    expect(offerPanorama.prop('imageNum')).toBe(3);
});

describe('если есть внешняя панорама, правильно рассчитывает точки начала и конца вращения', () => {
    [ 0, 1, 2, 3 ].forEach((index) => {
        it(`для тумбы номер #${ index }`, () => {
            offer = offer.withPanoramaExterior().value();
            const wrapper = shallow(
                <OfferThumb
                    url="/"
                    offer={ offer }
                    index={ index }
                    spinPanoramaOnScroll
                />,
            ).dive();

            const offerPanorama = wrapper.find('OfferPanorama');
            const spinPoints = {
                start: offerPanorama.prop('spinRelativeStart'),
                end: offerPanorama.prop('spinRelativeEnd'),
            };

            expect(spinPoints).toMatchSnapshot();
        });
    });

});

describe('бейдж профото из каталога', () => {
    it('покажется если есть тэг', () => {
        offer = offer.withTags([ 'catalog_photo' ]).value();
        const wrapper = shallow(
            <OfferThumb
                url="/"
                offer={ offer }
            />,
        ).dive();
        const catalogLabel = wrapper.find('OfferCatalogPhotoLabel');

        expect(catalogLabel.isEmptyRender()).toBe(false);
    });

    it('не покажется если есть внешняя панорама', () => {
        offer = offer.withPanoramaExterior().withTags([ 'catalog_photo' ]).value();
        const wrapper = shallow(
            <OfferThumb
                url="/"
                offer={ offer }
            />,
        ).dive();
        const catalogLabel = wrapper.find('OfferCatalogPhotoLabel');

        expect(catalogLabel.isEmptyRender()).toBe(true);
    });

    it('не покажется без тэга', () => {
        offer = offer.value();
        const wrapper = shallow(
            <OfferThumb
                url="/"
                offer={ offer }
            />,
        ).dive();
        const catalogLabel = wrapper.find('OfferCatalogPhotoLabel');

        expect(catalogLabel.isEmptyRender()).toBe(true);
    });
});
