jest.mock('react-redux', () => {
    const ActualReactRedux = jest.requireActual('react-redux');
    return {
        ...ActualReactRedux,
        useSelector: jest.fn(),
        useDispatch: jest.fn(),
    };
});

import React from 'react';
import { render } from '@testing-library/react';
import { Provider, useDispatch, useSelector } from 'react-redux';

import mockStore from 'autoru-frontend/mocks/mockStore';
import contextMock from 'autoru-frontend/mocks/contextMock';
import createContextProvider from 'autoru-frontend/mocks/createContextProvider';

import type { MagArticlePage } from 'auto-core/react/dataDomain/mag/StateMag';

import { blockMap } from '../ArticleBody/blocks';

import ArticleBody from './ArticleBody';

import '@testing-library/jest-dom';

const state = {
    ads: {
        data: {
            code: 'mag-article',
            data: {},
            settings: {
                c1: {},
            },
            statId: '100',
        },
        isFetching: false,
    },
};

const store = mockStore(state);

const ContextProvider = createContextProvider(contextMock);

it('должен вставить рекламу после последнего блока с опросом', () => {
    const post = {
        blocks: [
            { type: 'text', text: '123' },
            { type: 'quota', quota: { text: '123' } },
            { type: 'text', text: '123' },
            { type: 'poll', poll: { question: '?', answers: [ '!' ] } },
            { type: 'quota', quota: { text: '123' } },
        ],
    } as MagArticlePage;

    renderComponent(post);

    const adElement = document.querySelectorAll('.SectionBlock')[4];

    expect(adElement.getAttribute('class')).toContain('ArticleBody__ad');
});

it('должен вставить рекламу в конце, если нет блока с опросом', () => {
    const post = {
        blocks: [
            { type: 'text', text: '123' },
            { type: 'quota', quota: { text: '123' } },
            { type: 'text', text: '123' },
            { type: 'quota', quota: { text: '123' } },
        ],
    } as MagArticlePage;

    renderComponent(post);

    const adElement = document.querySelectorAll('.SectionBlock')[4];

    expect(adElement.getAttribute('class')).toContain('ArticleBody__ad');
});

function renderComponent(post: MagArticlePage) {
    (useDispatch as jest.MockedFunction<typeof useDispatch>).mockReturnValue(
        (...args) => store.dispatch(...args),
    );

    (useSelector as jest.MockedFunction<typeof useSelector>).mockImplementation(
        (selector) => selector(store.getState()),
    );

    return render(
        <ContextProvider>
            <Provider store={ store }>
                <ArticleBody post={ post } blockMap={ blockMap }/>
            </Provider>
        </ContextProvider>,
    );
}
