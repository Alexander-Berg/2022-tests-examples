import 'jest-enzyme';
import React from 'react';
import { shallow } from 'enzyme';

import type { OfferStatus } from '@vertis/schema-registry/ts-types-snake/auto/api/api_offer_model';

import cloneOfferWithHelpers from 'autoru-frontend/mockData/state/helpers/offer/cloneOfferWithHelpers';

import offerMock from 'auto-core/react/dataDomain/card/mocks/card.cars.2.mock';

import OFFER_STATUSES from 'auto-core/data/offer-statuses.json';

import type { CabinetOffer } from 'www-cabinet/react/types';

import OfferSnippetRoyalInfo from './OfferSnippetRoyalInfo';

const offerObj = cloneOfferWithHelpers(offerMock).withStatus(OFFER_STATUSES.active as OfferStatus).withDaysOnSale(5);

describe('покажет правильный текст для дней', () => {
    it('объявление активно', () => {
        const tree = shallowRenderComponent(offerObj.value() as CabinetOffer);

        expect(tree.find('.OfferSnippetRoyalInfo__days').text()).toEqual('В продаже5дней');
    });

    it('объявление неактивно', () => {
        const tree = shallowRenderComponent(offerObj.withStatus(OFFER_STATUSES.inactive as OfferStatus).value() as CabinetOffer);

        expect(tree.find('.OfferSnippetRoyalInfo__days').text()).toEqual('Создано5дней назад');
    });
});

function shallowRenderComponent(offer: CabinetOffer) {
    return shallow(
        <OfferSnippetRoyalInfo metrikaParams="" offer={ offer }/>,
    );
}
