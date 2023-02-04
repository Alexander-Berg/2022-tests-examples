import React from 'react';
import { shallow } from 'enzyme';

import { ResponseStatus } from '@vertis/schema-registry/ts-types/auto/api/response_model';
import type { FeaturesResponse } from '@vertis/schema-registry/ts-types-snake/auto/api/reviews/reviews_response_model';

import contextMock from 'autoru-frontend/mocks/contextMock';

import reviewsFeaturesMock from 'auto-core/react/dataDomain/reviewsFeatures/mocks/reviewsFeatures.mock';

import CardReviewsFeatures from './CardReviewsFeatures';

it('отрисует блок при наличии отзывов', () => {
    const tree = shallow(
        <CardReviewsFeatures features={ reviewsFeaturesMock.data.features }
            resourceParams={{ mark: 'AUDI', model: 'A4', category: 'cars', section: 'all' }}/>,
        { context: contextMock },
    );

    expect(tree.isEmptyRender()).toBe(false);
});

it('не отрисует блок, если нет ни одного вида отзывов', () => {
    const features: FeaturesResponse = {
        positive: [],
        negative: [],
        controversy: [],
        status: ResponseStatus.SUCCESS,
    };

    const tree = shallow(
        <CardReviewsFeatures
            features={ features }
            resourceParams={{ mark: 'AUDI', model: 'A4', category: 'cars', section: 'all' }}/>,
        { context: contextMock },
    );

    expect(tree.isEmptyRender()).toBe(true);
});
