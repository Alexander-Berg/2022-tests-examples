jest.mock('auto-core/lib/event-log/statApi');
jest.mock('auto-core/react/components/common/GarageCardArticlesAndReviews/GarageCardArticlesAndReviews', () => () => null);

import _ from 'lodash';
import React from 'react';
import userEvent from '@testing-library/user-event';
import { Provider } from 'react-redux';
import { render } from '@testing-library/react';

import contextMock from 'autoru-frontend/mocks/contextMock';
import createContextProvider from 'autoru-frontend/mocks/createContextProvider';
import mockStore from 'autoru-frontend/mocks/mockStore';

import statApi from 'auto-core/lib/event-log/statApi';

import mockConfig from 'auto-core/react/dataDomain/config/mock';

import mockGarageCard from 'auto-core/server/resources/publicApiGarage/methods/getCard.fixtures';

import type { Props } from './GarageCard';
import GarageCard from './GarageCard';

const Context = createContextProvider(contextMock);

const state = {
    bunker: {},
    config: mockConfig,
    state: {
        authModal: {},
    },
    user: { data: {} },
    garageCard: {
        data: {
            card: mockGarageCard.response200().card,
            state: 'VIEW',
        },
    },
};

describe('события в евент-лог отчётов', () => {
    it('должно отправиться при клике на ссылку в меню слева', () => {
        const { container } = renderComponent({
            isAuth: true,
            isDealer: false,
        });

        const linkToReport = container.querySelector('[href="#block-garage-proauto"]');
        linkToReport && userEvent.click(linkToReport);

        expect(statApi.log).toHaveBeenCalledTimes(1);
        expect(statApi.log).toHaveBeenCalledWith({
            vas_click_navig_event: {
                product: 'REPORTS',
                context_page: 'PAGE_GARAGE_CARD',
                context_block: 'BLOCK_CARD',
            },
        });
    });

    it('не должно отправиться для незалогина', () => {
        const { container } = renderComponent({
            isAuth: false,
        });

        const linkToReport = container.querySelector('[href="#block-garage-proauto"]');
        linkToReport && userEvent.click(linkToReport);

        expect(statApi.log).toHaveBeenCalledTimes(0);
    });

    it('не должно отправиться для дилера', () => {
        const { container } = renderComponent({
            isAuth: true,
            isDealer: true,
        });

        const linkToReport = container.querySelector('[href="#block-garage-proauto"]');
        linkToReport && userEvent.click(linkToReport);

        expect(statApi.log).toHaveBeenCalledTimes(0);
    });
});

function renderComponent(props: Partial<Props> = {}) {
    return render(
        <Context>
            <Provider store={ mockStore(state) }>
                <GarageCard
                    garageCard={ mockGarageCard.response200().card }
                    onCreateDraft={ jest.fn().mockResolvedValue(undefined) }
                    onDelete={ _.noop }
                    onSwitchToEdit={ _.noop }
                    onSwitchToExpanded={ _.noop }
                    onSwitchToView={ _.noop }
                    state="VIEW"
                    hasLenta={ false }
                    { ...props }
                />
            </Provider>
        </Context>,
    );
}
