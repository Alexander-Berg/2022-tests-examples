import cloneOfferWithHelpers from 'autoru-frontend/mockData/state/helpers/offer/cloneOfferWithHelpers';

import cardMock from 'auto-core/react/dataDomain/card/mocks/card.cars.2.mock';

import PreloadImages from './PreloadImages';

describe('карточка', () => {
    it('в десктопе предзагрузит одну фотку', () => {
        const card = cloneOfferWithHelpers(cardMock).withImages([
            { sizes: { '1200x900n': 'first-image' } },
            { sizes: { '1200x900n': 'second-image' } },
            { sizes: { '1200x900n': 'third-image' } },
        ]).value();
        const result = PreloadImages({ card }, 'card');
        expect(result).toMatchSnapshot();
    });

    it('в мобилке предзагрузит две фотки', () => {
        const card = cloneOfferWithHelpers(cardMock).withImages([
            { sizes: { '456x342': 'first-image' } },
            { sizes: { '456x342': 'second-image' } },
            { sizes: { '456x342': 'third-image' } },
        ]).value();
        const result = PreloadImages({ card }, 'card', true);
        expect(result).toMatchSnapshot();
    });

    it('если в начале стоят не картинки ничего не будет для них делать', () => {
        const card = cloneOfferWithHelpers(cardMock)
            .withPanoramaExterior()
            .withImages([
                { sizes: { '456x342': 'first-image' } },
                { sizes: { '456x342': 'second-image' } },
                { sizes: { '456x342': 'third-image' } },
            ])
            .value();
        const result = PreloadImages({ card }, 'card', true);
        expect(result).toMatchSnapshot();
    });
});
