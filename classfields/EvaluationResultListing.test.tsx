import React from 'react';
import { Provider } from 'react-redux';
import { render } from '@testing-library/react';

import mockStore from 'autoru-frontend/mocks/mockStore';
import contextMock from 'autoru-frontend/mocks/contextMock';
import createContextProvider from 'autoru-frontend/mocks/createContextProvider';

import configStateMock from 'auto-core/react/dataDomain/config/mock';

import evaluationListingStateMock from 'www-forms/react/reducers/evaluationListing.mock';
import evaluationDataStateMock from 'www-forms/react/dataDomain/evaluationData/mocks/evaluationData.mock';

import EvaluationResultListing from './EvaluationResultListing';

let initialState: unknown;
beforeEach(() => {
    initialState = {
        config: configStateMock.withIsMobile(false).value(),
        evaluation: {
            data: evaluationDataStateMock,
            isFetching: false,
        },
        evaluationListing: evaluationListingStateMock,
        formFields: { data: {} },
    };
});

const Container = () => {
    const store = mockStore(initialState);
    const Context = createContextProvider(contextMock);

    return (
        <Provider store={ store }>
            <Context>
                <EvaluationResultListing/>
            </Context>
        </Provider>
    );
};

it('should render offers from state.evaluationListing', () => {
    const result = render(<Container/>);

    const component = result.container.querySelectorAll('.ListingItemDesktop');
    expect(component).toHaveLength(2);
});
