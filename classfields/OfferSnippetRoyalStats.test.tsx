import { screen } from '@testing-library/react';
import React from 'react';
import _ from 'lodash';

import cloneOfferWithHelpers from 'autoru-frontend/mockData/state/helpers/offer/cloneOfferWithHelpers';

import offerMock from 'auto-core/react/dataDomain/card/mocks/card.cars.2.mock';

import '@testing-library/jest-dom';

import { renderComponent } from 'www-poffer/react/utils/testUtils';
import type { CabinetOffer } from 'www-cabinet/react/types';

import OfferSnippetRoyalStats from './OfferSnippetRoyalStats';

const baseProps = {
    client: {},
    clientId: '16453',
    isClient: true,
    canWriteSaleResource: true,
    dispatch: _.noop,
    isMultipostingEnabled: false,
};

it('должен написать заголовок про звонки, если подключен коллтрекинг и подключен подключен коллтрекинг по объявлению', async() => {
    await renderComponent(<OfferSnippetRoyalStats
        offer={ offerMock as CabinetOffer }
        getPriceHistoryData={ () => Promise.resolve(true) }
        getMoneySpent={ () => Promise.resolve(true) }
        callTrackingSettings={{
            calltracking_enabled: true,
            offers_stat_enabled: true,
        }}
        { ...baseProps }
    />);

    expect(screen.getByText('Звонки в зависимости от цены, ₽')).toBeDefined();
});

it('должен написать заголовок про звонки, если объявление легковое новое и дилер из Мск/Спб или области', async() => {
    const offer = cloneOfferWithHelpers(offerMock).withSection('new').value();
    await renderComponent(<OfferSnippetRoyalStats
        offer={ offer as unknown as CabinetOffer }
        isClientFromMoscowOrSpb={ true }
        getPriceHistoryData={ () => Promise.resolve(true) }
        getMoneySpent={ () => Promise.resolve(true) }
        callTrackingSettings={{}}
        { ...baseProps }
    />);

    expect(screen.getByText('Звонки в зависимости от цены, ₽')).toBeDefined();
});

it('должен написать заголовок про Просмотры, если подключен коллтрекинг, но не подключен коллтрекинг по объявлениям', async() => {
    await renderComponent(<OfferSnippetRoyalStats
        offer={ offerMock as CabinetOffer }
        getPriceHistoryData={ () => Promise.resolve(true) }
        getMoneySpent={ () => Promise.resolve(true) }
        callTrackingSettings={{
            calltracking_enabled: true,
            offers_stat_enabled: false,
        }}
        { ...baseProps }
    />);

    expect(screen.getByText('Просмотры в зависимости от цены, ₽')).toBeDefined();
});

it('должен написать текст-заглушку про коллтрекинг, если не подключен коллтрекинг', async() => {
    await renderComponent(<OfferSnippetRoyalStats
        offer={ offerMock as CabinetOffer }
        getPriceHistoryData={ () => Promise.resolve(true) }
        getMoneySpent={ () => Promise.resolve(true) }
        callTrackingSettings={{
            calltracking_enabled: false,
            offers_stat_enabled: false,
        }}
        { ...baseProps }
    />);

    expect(screen.getByText('Звонки в зависимости от цены, ₽')).toBeDefined();
    expect(screen.getByText('Анализируйте поступившие звонки. Мы автоматически выделим целевые и уникальные звонки.')).toBeDefined();
});
