import React from 'react';
import userEvent from '@testing-library/user-event';
import { render } from '@testing-library/react';
import { Provider } from 'react-redux';

jest.mock('auto-core/lib/event-log/statApi');
import { ContextBlock, ContextPage, ContextService } from '@vertis/schema-registry/ts-types-snake/auto/api/stat_events';

import createContextProvider from 'autoru-frontend/mocks/createContextProvider';
import contextMock from 'autoru-frontend/mocks/contextMock';
import mockStore from 'autoru-frontend/mocks/mockStore';

import { SECOND } from 'auto-core/lib/consts';
import statApi from 'auto-core/lib/event-log/statApi';

import type { TStateSearchID } from 'auto-core/react/dataDomain/searchID/TStateSearchID';
import type { StateAutoPopup } from 'auto-core/react/dataDomain/autoPopup/types';
import { AutoPopupNames } from 'auto-core/react/dataDomain/autoPopup/types';

import NpsModal from './NpsModal';

interface AppState {
    autoPopup: StateAutoPopup;
    searchID: TStateSearchID;
}
let initialState: AppState;

beforeEach(() => {
    initialState = {
        autoPopup: { id: AutoPopupNames.NPS_MODAL },
        searchID: {
            searchID: 'searchID',
            parentSearchId: 'parentSearchID',
        },
    };

    jest.useFakeTimers();
});

describe('event logs', () => {
    it('show', async() => {
        shallowRenderComponent({ initialState });

        expect(statApi.logImmediately).toHaveBeenCalledTimes(1);
        expect(statApi.logImmediately).toHaveBeenCalledWith({
            nps_show: {
                context_block: ContextBlock.BLOCK_POPUP,
                context_page: ContextPage.PAGE_LK,
                context_service: ContextService.SERVICE_AUTORU,
                question: 'Как вам Авто.ру?',
                stars: 0,
                feedback: '',
                search_query_id: 'searchID',
            },
        });

        expect(contextMock.metrika.sendParams).toHaveBeenCalledTimes(1);
        expect(contextMock.metrika.sendParams).toHaveBeenCalledWith([ 'NPS', 'd_offer_success_publish', 'show' ]);
    });

    it('close', async() => {
        const { findByLabelText } = shallowRenderComponent({ initialState });

        const closer = await findByLabelText(/close/i);
        userEvent.click(closer);

        expect(statApi.logImmediately).toHaveBeenCalledTimes(2);
        expect(statApi.logImmediately).toHaveBeenNthCalledWith(2, {
            nps_close: {
                context_block: ContextBlock.BLOCK_POPUP,
                context_page: ContextPage.PAGE_LK,
                context_service: ContextService.SERVICE_AUTORU,
                question: 'Как вам Авто.ру?',
                stars: 0,
                feedback: '',
                search_query_id: 'searchID',
            },
        });

        expect(contextMock.metrika.sendParams).toHaveBeenCalledTimes(2);
        expect(contextMock.metrika.sendParams).toHaveBeenNthCalledWith(2, [ 'NPS', 'd_offer_success_publish', 'close' ]);
    });

    it('rate', async() => {
        const { findByLabelText } = shallowRenderComponent({ initialState });

        const star = await findByLabelText(`Оценка: 3`);
        userEvent.click(star);

        expect(statApi.logImmediately).toHaveBeenCalledTimes(2);
        expect(statApi.logImmediately).toHaveBeenNthCalledWith(2, {
            nps_submit: {
                context_block: ContextBlock.BLOCK_POPUP,
                context_page: ContextPage.PAGE_LK,
                context_service: ContextService.SERVICE_AUTORU,
                question: 'Как вам Авто.ру?',
                stars: 3,
                feedback: '',
                search_query_id: 'searchID',
            },
        });

        expect(contextMock.metrika.sendParams).toHaveBeenCalledTimes(2);
        expect(contextMock.metrika.sendParams).toHaveBeenNthCalledWith(2, [ 'NPS', 'd_offer_success_publish', 'rate' ]);
    });

    it('comment', async() => {
        await simulateCommentSubmit(3, 'foo');

        expect(statApi.logImmediately).toHaveBeenCalledTimes(3);
        expect(statApi.logImmediately).toHaveBeenNthCalledWith(3, {
            nps_submit: {
                context_block: ContextBlock.BLOCK_POPUP,
                context_page: ContextPage.PAGE_LK,
                context_service: ContextService.SERVICE_AUTORU,
                question: 'Как вам Авто.ру?',
                stars: 3,
                feedback: 'foo',
                search_query_id: 'searchID',
            },
        });

        expect(contextMock.metrika.sendParams).toHaveBeenCalledTimes(3);
        expect(contextMock.metrika.sendParams).toHaveBeenNthCalledWith(3, [ 'NPS', 'd_offer_success_publish', 'comment' ]);
    });
});

it('после сабмита комментария скроет модал через 2 секунды', async() => {
    const { findByRole } = await simulateCommentSubmit(1, 'foo');

    const modal = await findByRole('dialog');
    expect(modal.classList.contains('Modal_visible')).toBe(true);

    jest.advanceTimersByTime(2 * SECOND);

    expect(modal.classList.contains('Modal_visible')).toBe(false);
});

async function simulateCommentSubmit(mark: number, text: string) {
    const result = shallowRenderComponent({ initialState });

    const star = await result.findByLabelText(`Оценка: ${ mark }`);
    userEvent.click(star);

    const comment = await result.findByRole('textbox') as HTMLTextAreaElement;
    userEvent.type(comment, text);

    const button = await result.findByText('Отправить');
    userEvent.click(button);

    return result;
}

function shallowRenderComponent({ initialState }: { initialState: AppState }) {
    const ContextProvider = createContextProvider(contextMock);
    const store = mockStore(initialState);

    return render(
        <ContextProvider>
            <Provider store={ store }>
                <NpsModal/>
            </Provider>
        </ContextProvider>,
    );
}
