import React from 'react';
import MockDate from 'mockdate';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import '@testing-library/jest-dom';

import cloneOfferWithHelpers from 'autoru-frontend/mockData/state/helpers/offer/cloneOfferWithHelpers';
import createContextProvider from 'autoru-frontend/mocks/createContextProvider';
import contextMock from 'autoru-frontend/mocks/contextMock';
import mockStore from 'autoru-frontend/mocks/mockStore';

import { nbsp } from 'auto-core/react/lib/html-entities';
import cardMock from 'auto-core/react/dataDomain/card/mocks/card.cars.2.mock';
import publicUserInfoMock from 'auto-core/react/dataDomain/publicUserInfo/mock';
import configMock from 'auto-core/react/dataDomain/config/mock';

import type { TSearchParameters } from 'auto-core/types/TSearchParameters';

import type { Props } from './PageResellerPublicMobileDumb';
import PageResellerPublicMobileDumb from './PageResellerPublicMobileDumb';

const defaultState = {
    listing: { data: {} },
    geo: { data: {} },
    config: configMock.value(),
};

const DefaultContext = createContextProvider({ ...contextMock, store: mockStore(defaultState) });

const fetchListingMock = jest.fn();

const defaultProps: Props = {
    searchParams: { } as TSearchParameters,
    params: {},
    offers: [ cardMock, cloneOfferWithHelpers(cardMock).withSaleId('123-321').value() ],
    pageType: 'reseller-public-page',
    pagination: {
        page: 1,
        page_size: 10,
        current: 1,
        total_offers_count: 20,
        total_page_count: 2,
        from: 1,
        to: 10,
    },
    isPending: false,
    fetchPhones: jest.fn(),
    fetchListing: fetchListingMock,
    fetchMoreListing: jest.fn(),
    replaceUrl: jest.fn(),
    sendMarketingEventByListingOffer: jest.fn(),
    listingRequestId: '',
    geo: [],
    geoRadius: 0,
    userInfo: publicUserInfoMock.withRegistrationDate('2018-01-13').value().data,
};

beforeEach(() => {
    MockDate.set('2021-01-13');
});

afterEach(() => {
    MockDate.reset();
});

it('запросит новый листинг при изменении фильтров', () => {
    render(getComponentWithWrapper(defaultProps));
    const statusInactiveSwitch = screen.getByText('Продано');

    userEvent.click(statusInactiveSwitch);

    expect(fetchListingMock).toHaveBeenCalledWith({ status: 'INACTIVE' }, { method: 'getOtherUserOffers' });
});

describe('дефолтные параметры в ссылке', () => {
    describe('status', () => {
        it('меняется на  дефолтный ACTIVE', () => {
            const props = { ...defaultProps, searchParams: { ...defaultProps.searchParams, status: [ 'INACTIVE' ] } as TSearchParameters };
            render(getComponentWithWrapper(props));
            const statusActiveSwitch = screen.getByText('В наличии');

            userEvent.click(statusActiveSwitch);

            expect(contextMock.pushState).toHaveBeenCalledWith('link/reseller-public-page/?');
        });
    });
});

describe('информация в шапке', () => {
    it('правильно рисует информацию в шапке когда есть вся информация', () => {
        render(getComponentWithWrapper(defaultProps));

        const items = screen.getAllByRole('listitem');

        expect(items).toHaveLength(2);
        expect(items.map(item => item.textContent)).toEqual([
            `На Авто.ру 3${ nbsp }года`,
            '6 в наличии / 9 продано',
        ]);
    });

    it('не будет рендерить инфу если у пользователя не пришла дата регистрации и у него нет авто нигде', () => {
        const props: Props = {
            ...defaultProps,
            userInfo: publicUserInfoMock
                .withRegistrationDate('')
                .withOfferStats({
                    CARS: {
                        active_offers_count: 0,
                        inactive_offers_count: 0,
                    },
                    MOTO: {
                        active_offers_count: 0,
                        inactive_offers_count: 0,
                    },
                    TRUCKS: {
                        active_offers_count: 0,
                        inactive_offers_count: 0,
                    },
                    ALL: {
                        active_offers_count: 0,
                        inactive_offers_count: 0,
                    },
                })
                .value().data,
        };
        render(getComponentWithWrapper(props));

        const items = screen.queryAllByRole('listitem');

        expect(items).toHaveLength(0);
    });
});

describe('сортировка', () => {
    it('сбрасывается клике на продано', async() => {
        const props = { ...defaultProps, searchParams: { ...defaultProps.searchParams, sort: 'price-asc' } as TSearchParameters };
        render(getComponentWithWrapper(props));

        const statusInactiveSwitch = screen.getByText('Продано');

        userEvent.click(statusInactiveSwitch);

        expect(fetchListingMock).toHaveBeenCalledWith({ status: 'INACTIVE' }, { method: 'getOtherUserOffers' });
        expect(contextMock.pushState).toHaveBeenCalledWith('link/reseller-public-page/?status=INACTIVE');
    });
});

function getComponentWithWrapper(props: Props, Context?: any) {
    const ContextComponent = Context || DefaultContext;
    return (
        <ContextComponent>
            <PageResellerPublicMobileDumb { ...props }/>
        </ContextComponent>
    );
}
