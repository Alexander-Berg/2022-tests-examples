/**
 * @jest-environment node
 */
import React from 'react';
import { shallow } from 'enzyme';

import { ROUTER_SERVICE_MOCK_1 } from 'core/services/router/mocks/routerService.mock';
import { ServiceId } from 'core/services/ServiceId';
import {
    REALTY_OFFER_MEDIUM_PREVIEW_TOP_IMAGE_MOCK_1,
    REALTY_OFFER_MEDIUM_PREVIEW_TOP_IMAGE_MOCK_2,
} from 'core/client/components/RealtyOfferMediumPreviewTopImage/mocks/offer.mock';
import { Button } from 'core/client/components/Button/Button';
import { mockStore } from 'core/mocks/store.mock';

import { PostBlockRealtyOffers } from './PostBlockRealtyOffers';

it('у кнопки правильная ссылка на листинг', () => {
    mockStore( { [ServiceId.ROUTER]: ROUTER_SERVICE_MOCK_1 });

    const wrapper = shallow(
        <PostBlockRealtyOffers
            data={{
                type: 'realtyOffers',
                realtyOffers: {
                    btnTitle: 'Смотреть больше объявлений',
                    filterUrl: '/snyat/kvartira/?yandexRent=YES&isFastlink=true',
                    listingUrl: '/moskva/snyat/kvartira/yandex-arenda/?isFastlink=true',
                    offers: [
                        REALTY_OFFER_MEDIUM_PREVIEW_TOP_IMAGE_MOCK_1,
                        REALTY_OFFER_MEDIUM_PREVIEW_TOP_IMAGE_MOCK_2,
                    ],
                },
            }}
            positionIndex={ 0 }
        />
    );

    expect(wrapper.find(Button).prop('href')).toBe('/moskva/snyat/kvartira/yandex-arenda/?isFastlink=true');
});
