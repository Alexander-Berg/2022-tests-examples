/**
 * @jest-environment jsdom
 */
import 'jest-enzyme';
import React from 'react';
import { shallow } from 'enzyme';

import { OfferStatus } from '@vertis/schema-registry/ts-types-snake/auto/api/api_offer_model';
import { Car_EngineType } from '@vertis/schema-registry/ts-types-snake/auto/api/cars_model';

import createContextProvider from 'autoru-frontend/mocks/createContextProvider';
import contextMock from 'autoru-frontend/mocks/contextMock';
import cloneOfferWithHelpers from 'autoru-frontend/mockData/state/helpers/offer/cloneOfferWithHelpers';

import { nbsp } from 'auto-core/react/lib/html-entities';
import cardMock from 'auto-core/react/dataDomain/card/mocks/card.cars.mock';

import { TOfferVas } from 'auto-core/types/proto/auto/api/api_offer_model';

import type { Props } from './SalesItemNewDesign';
import SalesItemNewDesign from './SalesItemNewDesign';

const defaultProps = {
    offer: cardMock,
    updatePricesHandler: jest.fn(),
    openPaymentModal: jest.fn(),
    onAutoProlongationToggle: jest.fn(),
    onAutoRenewChange: jest.fn(),
    loadStatsForOffer: jest.fn(),
    loadMoreStatsForOffer: jest.fn(),
    onOfferPriceChange: jest.fn(),
    onOfferActivate: jest.fn(),
    onOfferDelete: jest.fn(),
    onDraftDelete: jest.fn(),
    onOfferHide: jest.fn(),
    onOfferUpdate: jest.fn(),
    onStateChange: jest.fn(),
    onSocialAccountConnect: jest.fn(),
    hasTiedCards: true,
    isUserBanned: false,
    isOfferExpanded: false,
    hasMosRuProfile: false,
};

describe('метрики и вызовы', () => {
    const eventMap = {} as Record<string, any>;

    beforeAll(() => {
        jest.spyOn(global, 'addEventListener').mockImplementation((event, cb) => {
            eventMap[event] = cb;
        });
    });

    it('в случае аккаунта МосРу', () => {
        const offer = cloneOfferWithHelpers(cardMock)
            .withStatus(OfferStatus.INACTIVE)
            .withMarkInfo({ code: 'MARK' })
            .withSellerGeoParentsIds([ 'REGION' ])
            .withServicePrices([ { service: TOfferVas.PLACEMENT, price: 0 } ])
            .withActiveVas([])
            .value();
        const onOfferActivateMock = jest.fn(() => Promise.resolve());
        const mosRuActivateBunker = {
            marks: [ 'MARK' ],
            regions: [ 'REGION' ],
        };
        const props = {
            ...defaultProps,
            offer,
            hasMosRuProfile: false,
            mosRuActivateBunker,
            onOfferActivate: onOfferActivateMock,
        };

        shallowRenderComponent(props);

        eventMap.message({ data: { source: 'auth_form', type: 'auth-result', result: { success: true, user: {} } } });

        expect(contextMock.metrika.sendPageEvent.mock.calls[0][0]).toEqual([ 'activate_with_mosru', 'show' ]);
        expect(contextMock.metrika.sendPageEvent.mock.calls[1][0]).toEqual([ 'activate_with_mosru', 'success' ]);
        expect(contextMock.metrika.sendPageEvent.mock.calls[2][0]).toEqual([ 'clicks', 'activate' ]);
        expect(onOfferActivateMock).toHaveBeenCalledWith({
            category: 'cars',
            keepOthersUnfolded: true,
            offerIDHash: '1085562758-1970f439',
            updateOffer: {
                category: 'cars',
                offerID: '1085562758-1970f439',
            },
        });
    });

    it('в случае черновика', () => {
        const offer = cloneOfferWithHelpers(cardMock)
            .withStatus(OfferStatus.DRAFT)
            .value();
        const props = {
            ...defaultProps,
            offer,
        };

        shallowRenderComponent(props);

        expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledWith([ 'draft_in_lk', 'show' ]);
    });

    it('раскрытие статы и ВАСов', () => {
        const onStateChangeMock = jest.fn();
        const wrapper = shallowRenderComponent({
            ...defaultProps,
            onStateChange: onStateChangeMock,
        });

        const expandToggle = wrapper.find('.SalesItemNewDesign__expandToggle');
        expandToggle.simulate('click');

        expect(contextMock.metrika.sendPageEvent.mock.calls[0][0]).toEqual([ 'offer-stats', 'show' ]);
        expect(onStateChangeMock).toHaveBeenCalledWith({
            isUnfolded: true,
            keepOthersUnfolded: true,
            offerId: '1085562758-1970f439',
        });
    });

    it('сокрытие статы и ВАСов', () => {
        const onStateChangeMock = jest.fn();
        const props = {
            ...defaultProps,
            isOfferExpanded: true,
            onStateChange: onStateChangeMock,
        };

        const wrapper = shallowRenderComponent(props);

        const expandToggle = wrapper.find('.SalesItemNewDesign__expandToggle');
        expandToggle.simulate('click');

        expect(contextMock.metrika.sendPageEvent.mock.calls[0][0]).toEqual([ 'offer-stats', 'close' ]);
        expect(onStateChangeMock).toHaveBeenCalledWith({
            isUnfolded: false,
            keepOthersUnfolded: true,
            offerId: '1085562758-1970f439',
        });
    });
});

describe('пробег/моточасы', () => {
    it('должен отрендерить все поля, что есть', () => {
        const offer = cloneOfferWithHelpers(cardMock)
            .withCategory('cars')
            .withSection('used')
            .withMilage(100000)
            .withStatus(OfferStatus.ACTIVE)
            .withSubCategory('cars')
            .value();

        const props = {
            ...defaultProps,
            offer,
        };

        const wrapper = shallowRenderComponent(props);

        expect(wrapper.find('.SalesItemNewDesign__vehicleDetails').text())
            .toEqual(
                `100 000 км${ nbsp }• Передний${ nbsp }• Бензин${ nbsp }• ` +
                `1.6${ nbsp }AMT${ nbsp }(122${ nbsp }л.с.)${ nbsp }• Внедорожник 5 дв.${ nbsp }• Z6FLXXEC*LH****28`,
            );
    });

    it('должен заменить пробег на моточасы, если они есть', () => {
        const offer = cloneOfferWithHelpers(cardMock)
            .withCategory('trucks')
            .withSection('used')
            .withMilage(100000)
            .withStatus(OfferStatus.ACTIVE)
            .withSubCategory('crane')
            .withOperatingHours(13000)
            .withEngineType(Car_EngineType.GASOLINE)
            .value();

        const props = {
            ...defaultProps,
            offer,
        };

        const wrapper = shallowRenderComponent(props);

        expect(wrapper.find('.SalesItemNewDesign__vehicleDetails').text())
            .toEqual(`13${ nbsp }000 моточасов${ nbsp }• Внедорожник 5 дв.${ nbsp }• Z6FLXXEC*LH****28`);
    });
});

describe('рендеринг snippetToggles', () => {
    it('рендерит если оффер активный', () => {
        const offer = cloneOfferWithHelpers(cardMock).withStatus(OfferStatus.ACTIVE).value();
        const wrapper = shallowRenderComponent({
            ...defaultProps,
            offer,
        });

        expect(wrapper.find('.SalesItemNewDesign__snippetToggles')).toExist();
    });

    it('не рендерит если оффер неактивный', () => {
        const offer = cloneOfferWithHelpers(cardMock).withStatus(OfferStatus.INACTIVE).value();
        const wrapper = shallowRenderComponent({
            ...defaultProps,
            offer,
        });

        expect(wrapper.find('.SalesItemNewDesign__snippetToggles')).not.toExist();
    });
});

function shallowRenderComponent(props: Props) {
    const ContextProvider = createContextProvider(contextMock);

    return shallow(
        <ContextProvider>
            <SalesItemNewDesign { ...props }/>
        </ContextProvider>,
    ).dive().dive();
}
