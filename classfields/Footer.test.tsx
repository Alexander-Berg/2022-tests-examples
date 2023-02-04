import React from 'react';
import { shallow } from 'enzyme';

import { OfferStatus } from '@vertis/schema-registry/ts-types-snake/auto/api/api_offer_model';

import contextMock from 'autoru-frontend/mocks/contextMock';
import mockStore from 'autoru-frontend/mocks/mockStore';
import cloneOfferWithHelpers from 'autoru-frontend/mockData/state/helpers/offer/cloneOfferWithHelpers';

import offerMock from 'auto-core/react/dataDomain/card/mocks/card.cars.2.mock';
import vinReportMock from 'auto-core/react/dataDomain/vinReport/mocks/freeReport-mercedes.mock';
import configStateMock from 'auto-core/react/dataDomain/config/mock';

import Footer from './Footer';

it('должен установить класс для большого отступа снизу для активного объявления не-владельца', () => {
    const card = cloneOfferWithHelpers(offerMock).withStatus(OfferStatus.ACTIVE).withIsOwner(false).value();
    const store = mockStore({
        config: configStateMock.value(),
        card,
    });
    const tree = shallow(
        <Footer/>,
        { context: { ...contextMock, store } },
    ).dive();
    expect(tree.find('.Footer_withBottomSpace')).toHaveLength(1);
});

it('должен установить класс для большого отступа снизу для активного объявления владельца-частника', () => {
    const card = cloneOfferWithHelpers(offerMock).withStatus(OfferStatus.ACTIVE).withIsOwner(true).value();
    const store = mockStore({
        config: configStateMock.value(),
        card,
    });
    const tree = shallow(
        <Footer/>,
        { context: { ...contextMock, store } },
    ).dive();
    expect(tree.find('.Footer_withBottomSpace')).toHaveLength(1);
});

it('не должен установить класс для большого отступа снизу для неактивного объявления не-владельца', () => {
    const card = cloneOfferWithHelpers(offerMock).withStatus(OfferStatus.INACTIVE).withIsOwner(false).value();
    const store = mockStore({
        config: configStateMock.value(),
        card,
    });
    const tree = shallow(
        <Footer/>,
        { context: { ...contextMock, store } },
    ).dive();
    expect(tree.find('.Footer_withBottomSpace')).toHaveLength(0);
});

it('не должен установить класс для большого отступа снизу для неактивного объявления владельца', () => {
    const card = cloneOfferWithHelpers(offerMock).withStatus(OfferStatus.INACTIVE).withIsOwner(false).value();
    const store = mockStore({
        config: configStateMock.value(),
        card,
    });
    const tree = shallow(
        <Footer/>,
        { context: { ...contextMock, store } },
    ).dive();
    expect(tree.find('.Footer_withBottomSpace')).toHaveLength(0);
});

it('не должен установить класс для большого отступа снизу для активного объявления владельца-компании', () => {
    const card = cloneOfferWithHelpers(offerMock).withStatus(OfferStatus.INACTIVE).withIsOwner(true).withSellerTypeCommercial().value();
    const store = mockStore({
        config: configStateMock.value(),
        card,
    });
    const tree = shallow(
        <Footer/>,
        { context: { ...contextMock, store } },
    ).dive();
    expect(tree.find('.Footer_withBottomSpace')).toHaveLength(0);
});

it('должен установить класс для большого отступа снизу для страницы отчета', () => {
    const store = mockStore({
        vinReport: { data: vinReportMock },
        config: configStateMock.withPageType('proauto-report').value(),
    });
    const tree = shallow(
        <Footer/>,
        { context: { ...contextMock, store } },
    ).dive();
    expect(tree.find('.Footer_withBottomSpace')).toHaveLength(1);
});
