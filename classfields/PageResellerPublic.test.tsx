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

import type { TSearchParameters } from 'auto-core/types/TSearchParameters';
import LISTING_OUTPUT_TYPE from 'auto-core/data/listing/OutputTypes';

import type { Props } from './PageResellerPublicDumb';
import PageResellerPublicDumb from './PageResellerPublicDumb';

const defaultState = {
    listing: { data: {} },
};

const DefaultContext = createContextProvider({ ...contextMock, store: mockStore(defaultState) });

const fetchListingMock = jest.fn();

const defaultProps: Props = {
    searchParams: { status: [ 'ACTIVE' ] } as TSearchParameters,
    params: {},
    outputType: LISTING_OUTPUT_TYPE.LIST,
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
    sellerPopupClose: jest.fn(),
    sellerPopupOpen: jest.fn(),
    hideOffer: jest.fn(),
    showOffer: jest.fn(),
    sendMarketingEventByListingOffer: jest.fn(),
    isDealer: false,
    isModerator: false,
    stateSupportData: {},
    listingRequestId: '',
    hiddenOffers: [],
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

it('обновить ссылку при изменении outputType', () => {
    const { rerender } = render(getComponent(), { wrapper: getWrapper() });

    rerender(getComponent({ ...defaultProps, outputType: LISTING_OUTPUT_TYPE.CAROUSEL }));

    expect(contextMock.pushState).toHaveBeenCalledWith('link/reseller-public-page/?output_type=carousel');
});

it('запросит новый листинг при изменении фильтров', () => {
    render(getComponent(), { wrapper: getWrapper() });
    const statusInactiveSwitch = screen.getByText('Продано');

    userEvent.click(statusInactiveSwitch);

    expect(fetchListingMock).toHaveBeenCalledWith({ status: 'INACTIVE' }, { method: 'getOtherUserOffers' });
});

describe('дефолтные параметры в ссылке', () => {
    describe('outputType', () => {
        it('дефолтный outputType в экспе AUTORUFRONT-19853_carousel', () => {
            const Context = createContextProvider(
                { ...contextMock, store: mockStore(defaultState), hasExperiment: (exp: string) => exp === 'AUTORUFRONT-19853_carousel' },
            );
            const { rerender } = render(getComponent(), { wrapper: getWrapper(Context) });

            rerender(getComponent({ ...defaultProps, outputType: LISTING_OUTPUT_TYPE.CAROUSEL }));

            expect(contextMock.pushState).toHaveBeenCalledWith('link/reseller-public-page/?');
        });

        it('не дефолтный outputType в экспе AUTORUFRONT-19853_carousel', () => {
            const Context = createContextProvider(
                { ...contextMock, store: mockStore(defaultState), hasExperiment: (exp: string) => exp === 'AUTORUFRONT-19853_carousel' },
            );
            const { rerender } = render(getComponent({ ...defaultProps, outputType: LISTING_OUTPUT_TYPE.CAROUSEL }), { wrapper: getWrapper(Context) });

            rerender(getComponent(defaultProps));

            expect(contextMock.pushState).toHaveBeenCalledWith('link/reseller-public-page/?output_type=list');
        });

        it('дефолтный outputType без экспа', () => {
            const props = { ...defaultProps, outputType: LISTING_OUTPUT_TYPE.CAROUSEL };
            const { rerender } = render(getComponent(props), { wrapper: getWrapper() });

            rerender(getComponent(defaultProps));

            expect(contextMock.pushState).toHaveBeenCalledWith('link/reseller-public-page/?');
        });

        it('не дефолтный outputType без экспа', () => {
            const { rerender } = render(getComponent(), { wrapper: getWrapper() });

            rerender(getComponent({ ...defaultProps, outputType: LISTING_OUTPUT_TYPE.CAROUSEL }));

            expect(contextMock.pushState).toHaveBeenCalledWith('link/reseller-public-page/?output_type=carousel');
        });
    });

    describe('status', () => {
        it('меняется на  дефолтный ACTIVE', () => {
            const props = { ...defaultProps, searchParams: { ...defaultProps.searchParams, status: [ 'INACTIVE' ] } as TSearchParameters };
            render(getComponent(props), { wrapper: getWrapper() });
            const statusActiveSwitch = screen.getByText('В наличии');

            userEvent.click(statusActiveSwitch);

            expect(contextMock.pushState).toHaveBeenCalledWith('link/reseller-public-page/?');
        });
    });
});

describe('сортировка', () => {
    it('сбрасывается клике на продано', async() => {
        const props = { ...defaultProps, searchParams: { ...defaultProps.searchParams, sort: 'price-asc' } as TSearchParameters };
        render(getComponent(props), { wrapper: getWrapper() });

        const statusInactiveSwitch = screen.getByText('Продано');

        userEvent.click(statusInactiveSwitch);

        expect(fetchListingMock).toHaveBeenCalledWith({ status: 'INACTIVE' }, { method: 'getOtherUserOffers' });
        expect(contextMock.pushState).toHaveBeenCalledWith('link/reseller-public-page/?status=INACTIVE');
    });
});

describe('информация в шапке', () => {
    it('правильно рисует информацию в шапке когда есть вся информация', () => {
        render(getComponent(), { wrapper: getWrapper() });

        const items = screen.getAllByRole('listitem');

        expect(items).toHaveLength(3);
        expect(items.map(item => item.textContent)).toEqual([
            `6 в наличии`,
            `9 продано`,
            `На Авто.ру 3${ nbsp }года`,
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
        render(getComponent(props), { wrapper: getWrapper() });

        const items = screen.queryAllByRole('listitem');

        expect(items).toHaveLength(0);
    });
});

function getWrapper(Context: any = DefaultContext) {
    const wrapper = ({ children }: { children: JSX.Element }) => (
        <Context>
            { children }
        </Context>
    );
    return wrapper;
}

function getComponent(props: Props = defaultProps) {
    return (
        <PageResellerPublicDumb { ...props }/>
    );
}
