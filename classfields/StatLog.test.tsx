/* eslint-disable-next-line import-helpers/order-imports */
import mockLocalStatData from 'auto-core/react/lib/localStatData.mock';
jest.mock('auto-core/react/lib/localStatData', () => mockLocalStatData);

import React from 'react';
import { render } from '@testing-library/react';

import offerMock from 'auto-core/react/dataDomain/card/mocks/card.cars.2.mock';
import '@testing-library/jest-dom';

import StatLogDumb from './StatLogDumb';

const updateSearchID = jest.fn();

beforeEach(() => {
    updateSearchID.mockClear();
    mockLocalStatData.getSearchId.mockClear();
    mockLocalStatData.getSearchIdByOffer.mockClear();
    mockLocalStatData.getSearchIdByPageLifeTime.mockClear();
});

it('ДОЛЖЕН обновить searchID, если он уже есть из листинга (кейс листинга и морды)', () => {
    /* должен потому что берется id из разных мест, а сохраняется в одно и то же */
    const {
        rerender,
    } = render(
        <StatLogDumb
            pageParams={{}}
            updateSearchID={ updateSearchID }
            listingRequestId="123"
            listingSearchId="456"
            searchID="567"
        />,
    );

    expect(updateSearchID).toHaveBeenCalledWith('567', undefined);
    expect(mockLocalStatData.setSearchId).toHaveBeenCalledWith('567', undefined);
    expect(mockLocalStatData.setLastSearchId).toHaveBeenCalledWith('567', undefined);

    mockLocalStatData.getSearchId.mockImplementationOnce(() => ({
        searchId: '567',
        parentSearchId: undefined,
    }));

    rerender(
        <StatLogDumb
            pageParams={{}}
            updateSearchID={ updateSearchID }
            listingRequestId="1230"
            listingSearchId="4560"
            searchID="5670"
        />,
    );

    expect(updateSearchID).toHaveBeenNthCalledWith(2, '5670', '567');
    expect(mockLocalStatData.setSearchId).toHaveBeenNthCalledWith(2, '5670', '567');
    expect(mockLocalStatData.setLastSearchId).toHaveBeenNthCalledWith(2, '5670', '567');
});

it('ДОЛЖЕН обновить SearchID, если SearchID не передан, но есть id листинга (кейс групповой карточки при переходе из поиска)', () => {
    mockLocalStatData.getSearchIdByPageLifeTime.mockImplementationOnce(() => null);

    const {
        rerender,
    } = render(
        <StatLogDumb
            pageParams={{}}
            updateSearchID={ updateSearchID }
            listingRequestId="123"
            listingSearchId="456"
        />,
    );

    mockLocalStatData.getSearchId.mockImplementationOnce(() => ({
        searchId: '123',
        parentSearchId: undefined,
    }));

    rerender(
        <StatLogDumb
            pageParams={{}}
            updateSearchID={ updateSearchID }
            listingRequestId="1230"
            listingSearchId="4560"
        />,
    );

    expect(updateSearchID).toHaveBeenNthCalledWith(2, '1230', '123');
    expect(mockLocalStatData.setSearchId).toHaveBeenNthCalledWith(2, '1230', '123');
    expect(mockLocalStatData.setLastSearchId).toHaveBeenNthCalledWith(2, '1230', '123');
    expect(mockLocalStatData.getSearchIdByOffer).not.toHaveBeenCalled();
});

it('ДОЛЖЕН обновить SearchID, если SearchID не передан, но есть id листинга и восстановленный SearchID (кейс групповой карточки при прямом переходе)', () => {
    mockLocalStatData.getSearchIdByPageLifeTime.mockImplementationOnce(() => 'getSearchIdByPageLifeTime');

    const {
        rerender,
    } = render(
        <StatLogDumb
            pageParams={{}}
            updateSearchID={ updateSearchID }
            listingRequestId="123"
            listingSearchId="456"
        />,
    );

    expect(updateSearchID).toHaveBeenCalledWith('getSearchIdByPageLifeTime', undefined);
    expect(mockLocalStatData.setSearchId).toHaveBeenCalledWith('getSearchIdByPageLifeTime', undefined);
    expect(mockLocalStatData.setLastSearchId).toHaveBeenCalledWith('getSearchIdByPageLifeTime', undefined);

    mockLocalStatData.getSearchId.mockImplementationOnce(() => ({
        searchId: 'getSearchIdByPageLifeTime',
        parentSearchId: undefined,
    }));

    mockLocalStatData.getSearchIdByPageLifeTime.mockImplementationOnce(() => 'getSearchIdByPageLifeTime2');

    rerender(
        <StatLogDumb
            pageParams={{}}
            updateSearchID={ updateSearchID }
            listingRequestId="1230"
            listingSearchId="4560"
        />,
    );

    expect(updateSearchID).toHaveBeenNthCalledWith(2, 'getSearchIdByPageLifeTime2', 'getSearchIdByPageLifeTime');
    expect(mockLocalStatData.setSearchId).toHaveBeenNthCalledWith(2, 'getSearchIdByPageLifeTime2', 'getSearchIdByPageLifeTime');
    expect(mockLocalStatData.setLastSearchId).toHaveBeenNthCalledWith(2, 'getSearchIdByPageLifeTime2', 'getSearchIdByPageLifeTime');
});

it('ДОЛЖЕН обновить SearchID, если ничего не передано, но передан оффер (кейс карточки, открытой в новом табе)', () => {
    mockLocalStatData.getSearchIdByOffer.mockImplementationOnce(() => 'getSearchIdByOffer');

    const {
        rerender,
    } = render(
        <StatLogDumb
            pageParams={{}}
            updateSearchID={ updateSearchID }
            offer={ offerMock }
            referrer="referrer"
        />,
    );

    expect(updateSearchID).toHaveBeenCalledWith('getSearchIdByOffer', undefined);
    expect(mockLocalStatData.setSearchId).toHaveBeenCalledWith('getSearchIdByOffer', undefined);
    expect(mockLocalStatData.setLastSearchId).toHaveBeenCalledWith('getSearchIdByOffer', undefined);
    expect(mockLocalStatData.getSearchIdByOffer).toHaveBeenCalledWith(offerMock, 'referrer');

    mockLocalStatData.getSearchIdByOffer.mockImplementationOnce(() => 'getSearchIdByOffer2');

    mockLocalStatData.getSearchId.mockImplementationOnce(() => ({
        searchId: 'getSearchIdByOffer',
        parentSearchId: undefined,
    }));

    const newOffer = {
        ...offerMock,
        id: 'new id',
    };

    rerender(
        <StatLogDumb
            pageParams={{}}
            updateSearchID={ updateSearchID }
            offer={ newOffer }
            referrer="referrer"
        />,
    );

    expect(updateSearchID).toHaveBeenNthCalledWith(2, 'getSearchIdByOffer2', 'getSearchIdByOffer');
    expect(mockLocalStatData.setSearchId).toHaveBeenNthCalledWith(2, 'getSearchIdByOffer2', 'getSearchIdByOffer');
    expect(mockLocalStatData.setLastSearchId).toHaveBeenNthCalledWith(2, 'getSearchIdByOffer2', 'getSearchIdByOffer');
    expect(mockLocalStatData.getSearchIdByOffer).toHaveBeenNthCalledWith(2, newOffer, 'referrer');
});

it('не обновляет в сторадже инфу, если переданный id такой же, но сует его в стор', () => {
    mockLocalStatData.getSearchId.mockImplementationOnce(() => ({
        searchId: '567',
        parentSearchId: undefined,
    }));

    render(
        <StatLogDumb
            pageParams={{}}
            updateSearchID={ updateSearchID }
            listingRequestId="123"
            listingSearchId="456"
            searchID="567"
        />,
    );

    expect(mockLocalStatData.setSearchId).toHaveBeenCalledTimes(0);
    expect(updateSearchID).toHaveBeenCalledTimes(1);
});
