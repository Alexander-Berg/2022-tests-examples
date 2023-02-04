import offerNewMock from 'autoru-frontend/mockData/state/newCard.mock';
import cloneOfferWithHelpers from 'autoru-frontend/mockData/state/helpers/offer/cloneOfferWithHelpers';

import offerMock from 'auto-core/react/dataDomain/card/mocks/card.cars.mock';

import getLinkParamsToListing from './getLinkParamsToListing';

describe('должен формировать правильные search параметры для ссылки', () => {
    it('на листинг для легковых б/у', () => {
        expect(getLinkParamsToListing(offerMock)).toEqual({
            category: 'cars',
            geo_id: '213',
            geo_radius: 200,
            has_image: true,
            mark: 'FORD',
            model: 'ECOSPORT',
            section: 'used',
            super_gen: '20104320',
        });
    });

    it('на группу легковых новых', () => {
        expect(getLinkParamsToListing(offerNewMock.card)).toEqual({
            category: 'cars',
            configuration_id: '20692875',
            super_gen: '20692838',
            custom_state_key: 'CLEARED',
            geo_id: '213',
            geo_radius: 200,
            mark: 'TOYOTA',
            model: 'RAV_4',
            section: 'new',
            with_discount: true,
        });
    });

    it('на листинг для легковых б/у для битой тачки', () => {
        const offer = cloneOfferWithHelpers(offerMock)
            .withIsBeaten(true)
            .value();

        expect(getLinkParamsToListing(offer)).toEqual({
            category: 'cars',
            damage_group: 'ANY',
            geo_id: '213',
            geo_radius: 200,
            has_image: true,
            mark: 'FORD',
            model: 'ECOSPORT',
            section: 'used',
            super_gen: '20104320',
        });
    });
});
