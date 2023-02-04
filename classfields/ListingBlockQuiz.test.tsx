/**
 * @jest-environment node
 */
import React from 'react';
import { shallow } from 'enzyme';

import { ROUTER_SERVICE_MOCK_1 } from 'core/services/router/mocks/routerService.mock';
import { LISTING_BLOCK_QUIZ_MOCK_1 } from 'core/services/postListing/mocks/listingBlock.mock';
import { mockStore } from 'core/mocks/store.mock';
import { Actionable } from 'core/client/components/Actionable/Actionable';

import { ListingBlockQuiz } from './ListingBlockQuiz';

describe('в разметке присутствуют ссылки', () => {
    mockStore({ router: ROUTER_SERVICE_MOCK_1 });

    const wrapper = shallow(
        <ListingBlockQuiz
            data={ LISTING_BLOCK_QUIZ_MOCK_1 }
            posts={{}}
            isMobile={ false }
            positionIndex={ 0 }
        />
    );

    it('заголовок - на листинг', () => {
        expect(wrapper.find(Actionable).at(0).prop('href')).toBe('/journal/tag/research/');
    });

    it('левая кнопка - на пост', () => {
        expect(wrapper.find(Actionable).at(1).prop('href')).toBe('/journal/post/kviz-ugadayte-skolko-stoit-arenda-kvartiry-na-foto/');
    });

    it('левая кнопка - на листинг', () => {
        expect(wrapper.find(Actionable).at(2).prop('href')).toBe('/journal/tag/research/');
    });

    it('правая кнопка - на пост', () => {
        expect(wrapper.find(Actionable).at(3).prop('href')).toBe('/journal/post/kviz-ugadayte-skolko-stoit-arenda-kvartiry-na-foto/');
    });
});
