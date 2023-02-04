/**
 * @jest-environment node
 */
import React from 'react';
import { shallow } from 'enzyme';

import { ROUTER_SERVICE_MOCK_1 } from 'core/services/router/mocks/routerService.mock';
import { ServiceId } from 'core/services/ServiceId';
import { mockStore } from 'core/mocks/store.mock';
import { htmlEntities } from 'core/client/lib/html/htmlEntities';
import { LinkOverlay } from 'core/client/components/LinkOverlay/LinkOverlay';
import { Actionable } from 'core/client/components/Actionable/Actionable';
import { REALTY_OFFER_MEDIUM_PREVIEW_TOP_IMAGE_MOCK_1 } from 'core/client/components/RealtyOfferMediumPreviewTopImage/mocks/offer.mock.ts';

import { RealtyOfferMediumPreviewTopImage } from './RealtyOfferMediumPreviewTopImage';

it('правильные ссылки на листинг', () => {
    mockStore( { [ServiceId.ROUTER]: ROUTER_SERVICE_MOCK_1 });

    const wrapper = shallow(
        <RealtyOfferMediumPreviewTopImage
            offer={ REALTY_OFFER_MEDIUM_PREVIEW_TOP_IMAGE_MOCK_1 }
            offerType="offer"
        />
    );

    expect(wrapper.find(LinkOverlay).prop('href')).toBe('/offer/4290853562119205321/');
    expect(wrapper.find(Actionable).prop('href')).toBe('/offer/4290853562119205321/');
});

it('заголовок обернут в ссылку', () => {
    mockStore( { [ServiceId.ROUTER]: ROUTER_SERVICE_MOCK_1 });

    const wrapper = shallow(
        <RealtyOfferMediumPreviewTopImage
            offer={ REALTY_OFFER_MEDIUM_PREVIEW_TOP_IMAGE_MOCK_1 }
            offerType="offer"
        />
    );

    expect(wrapper.find(Actionable).prop('children')).toBe(`39${ htmlEntities.nbsp }м², 1-комнатная`);
});
