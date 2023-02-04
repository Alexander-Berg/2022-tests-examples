import React from 'react';
import { shallow } from 'enzyme';
import 'jest-enzyme';

import cloneOfferWithHelpers from 'autoru-frontend/mockData/state/helpers/offer/cloneOfferWithHelpers';
import createContextProvider from 'autoru-frontend/mocks/createContextProvider';
import contextMock from 'autoru-frontend/mocks/contextMock';

import offerMock from 'auto-core/react/dataDomain/card/mocks/card.cars.2.mock';

import SalesItemNewDesignOfferInfo from './SalesItemNewDesignOfferInfo';
import type { Props } from './SalesItemNewDesignOfferInfo';

const defaultOffer = cloneOfferWithHelpers(offerMock)
    .withCreationDate(String(new Date('2021-03-15T12:00:00Z').getTime()))
    .withExpireDate(String(new Date('2021-04-03T12:00:00Z').getTime()))
    .withServices(offerMock.services.map(item => ({
        ...item, expire_date: String(new Date('2021-04-03T12:00:00Z').getTime()),
    })))
    .value();

const defaultProps = {
    offer: defaultOffer,
    isOfferExpanded: false,
};

it('если оффер свернут, то рендерит инфу о тачке', () => {
    const wrapper = shallowRenderComponent(defaultProps);

    expect(wrapper.find('.SalesItemNewDesignOfferInfo__item')).toExist();
    expect(wrapper.find('BadgeForExclusiveOfferDesktop')).not.toExist();
    expect(wrapper.find('BadgeVinReportDesktop')).not.toExist();
});

it('если оффер развернут, то рендерит бейджи', () => {
    const wrapper = shallowRenderComponent({
        ...defaultProps,
        isOfferExpanded: true,
    });

    expect(wrapper.find('.SalesItemNewDesignOfferInfo__item')).not.toExist();
    expect(wrapper.find('BadgeForExclusiveOfferDesktop')).toExist();
    expect(wrapper.find('BadgeVinReportDesktop')).toExist();
});

function shallowRenderComponent(props: Props) {
    const ContextProvider = createContextProvider(contextMock);

    const wrapper = shallow(
        <ContextProvider>
            <SalesItemNewDesignOfferInfo { ...props }/>
        </ContextProvider>,
    );

    return wrapper.dive();
}
