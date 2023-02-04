import React from 'react';
import { Provider } from 'react-redux';
import { shallow } from 'enzyme';

import createContextProvider from 'autoru-frontend/mocks/createContextProvider';
import contextMock from 'autoru-frontend/mocks/contextMock';
import mockStore from 'autoru-frontend/mocks/mockStore';

import CtbAuctionOld from './CtbAuctionOld/CtbAuctionOld';
import CtbAuctionNew from './CtbAuctionNew/CtbAuctionNew';
import CtbAuction from './CtbAuction';

describe('CtbAuction - компонент-обертка для показа старого или нового аукциона в лк', () => {
    it('должен показывать новый дизайн лк, если попали в эксперимент AUTORUFRONT-19219_new_lk_and_vas_block_design', () => {
        const hasNewDesignExp = true;
        const auctionPage = renderComponent(hasNewDesignExp);

        expect(auctionPage.find(CtbAuctionNew).exists()).toBe(true);
        expect(auctionPage.find(CtbAuctionOld).exists()).toBe(false);
    });

    it('должен показывать старый дизайн лк по умолчанию', () => {
        const hasNewDesignExp = false;
        const auctionPage = renderComponent(hasNewDesignExp);

        expect(auctionPage.find(CtbAuctionOld).exists()).toBe(true);
        expect(auctionPage.find(CtbAuctionNew).exists()).toBe(false);
    });
});

function renderComponent(hasNewDesignExp: boolean) {
    const store = mockStore();
    const context = {
        ...contextMock,
        hasExperiment: () => hasNewDesignExp,
    };
    const Context = createContextProvider(context);

    const page = shallow(
        <Context>
            <Provider store={ store }>
                <CtbAuction/>
            </Provider>
        </Context>,
    ).dive().dive();

    return page;
}
