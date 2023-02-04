import 'jest-enzyme';
import React from 'react';
import { shallow } from 'enzyme';

import createContextProvider from 'autoru-frontend/mocks/createContextProvider';
import contextMock from 'autoru-frontend/mocks/contextMock';

import reviewsSummary from 'auto-core/react/dataDomain/reviewsSummary/mocks/reviewsSummary.mock';

import type { TOfferCategory, TOfferSubCategory } from 'auto-core/types/proto/auto/api/api_offer_model';

import RatingSnippetItem from './RatingSnippetItem';

const ContextProvider = createContextProvider(contextMock);

const params = {
    category: 'cars' as TOfferCategory,
    subCategory: 'cars' as TOfferSubCategory,
    mark: 'VOLKSWAGEN',
    model: 'TIGUAN',
};

const review = reviewsSummary.reviews[0];

it('у RatingSnippetItem должна быть правильная ссылка с категорией cars', () => {
    const wrapper = shallow(
        <ContextProvider>
            <RatingSnippetItem
                from="card"
                review={ review }
                params={ params }
            />
        </ContextProvider>,
    ).dive();

    expect(wrapper.prop('url'))
        .toBe('link/review-card-cars/?mark=VOLKSWAGEN&model=TIGUAN&from=card&parent_category=cars&reviewId=273000524703610581&super_gen=6847474');
});

it('у RatingSnippetItem должна быть правильная ссылка c подкатегорией lcv', () => {
    params.subCategory = 'lcv';

    const wrapper = shallow(
        <ContextProvider>
            <RatingSnippetItem
                from="listing"
                review={ review }
                params={ params }
            />
        </ContextProvider>,
    ).dive();

    expect(wrapper.prop('url'))
        .toBe('link/review-card-cars/?mark=VOLKSWAGEN&model=TIGUAN&from=listing&parent_category=cars&reviewId=273000524703610581&super_gen=6847474');
});
