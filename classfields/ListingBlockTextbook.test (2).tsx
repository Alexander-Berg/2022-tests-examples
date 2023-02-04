/**
 * @jest-environment node
 */
import React from 'react';
import { shallow } from 'enzyme';

import { ROUTER_SERVICE_MOCK_1 } from 'core/services/router/mocks/routerService.mock';
import { LISTING_BLOCK_TEXTBOOK_FOR_MAP_MOCK } from 'core/services/postListing/mocks/listingBlock.mock';
import { mockStore } from 'core/mocks/store.mock';
import { Link } from 'core/client/components/Link/Link';
import { Actionable } from 'core/client/components/Actionable/Actionable';
import { MediumPreviewPostLeftText } from 'core/client/components/MediumPreviewPostLeftText/MediumPreviewPostLeftText';
import { ServiceId } from 'core/services/ServiceId';

import { ListingBlockTextbook } from './ListingBlockTextbook';

describe('правильно выставляет атрибуты', () => {
    mockStore({ [ServiceId.ROUTER]: ROUTER_SERVICE_MOCK_1 });

    const wrapper = shallow(
        <ListingBlockTextbook { ...LISTING_BLOCK_TEXTBOOK_FOR_MAP_MOCK }/>,
    );

    it('ссылки для заголовка и кнопки', async() => {
        const href = '/journal/category/uchebnik/';
        expect(wrapper.find(Link).prop('href')).toBe(href);
        expect(wrapper.find(Actionable).prop('href')).toBe(href);
    });

    it('предзагружается только первая картинка', async() => {
        expect(wrapper.find(MediumPreviewPostLeftText).at(0).prop('isPreloadImage')).toBe(true);
        expect(wrapper.find(MediumPreviewPostLeftText).at(1).prop('isPreloadImage')).toBe(false);
        expect(wrapper.find(MediumPreviewPostLeftText).at(2).prop('isPreloadImage')).toBe(false);
    });
});
