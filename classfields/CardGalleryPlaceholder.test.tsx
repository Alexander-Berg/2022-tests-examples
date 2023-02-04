import React from 'react';
import { shallow } from 'enzyme';

import contextMock from 'autoru-frontend/mocks/contextMock';
import cloneOfferWithHelpers from 'autoru-frontend/mockData/state/helpers/offer/cloneOfferWithHelpers';

import offerMock from 'auto-core/react/dataDomain/card/mocks/card.cars.2.mock';

import CardGalleryPlaceholder from './CardGalleryPlaceholder';

it('должен отправить владельца на форму добавления фото', () => {
    const offer = cloneOfferWithHelpers(offerMock).withIsOwner(true).value();

    const wrapper = shallow(
        <CardGalleryPlaceholder offer={ offer }/>,
        { context: contextMock },
    );
    expect(wrapper.find('Link').props()).toHaveProperty('url', 'link/offer-photos-add/?parent_category=cars&sale_id=1085562758-1970f439');
});
