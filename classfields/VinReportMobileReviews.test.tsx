import 'jest-enzyme';
import React from 'react';
import { shallow } from 'enzyme';

import contextMock from 'autoru-frontend/mocks/contextMock';

import type { ReportAllReviewsBlock } from 'auto-core/react/dataDomain/vinReport/types';
import DATA from 'auto-core/react/dataDomain/defaultVinReport/mocks/reviews';

import VinReportMobileReviews from './VinReportMobileReviews';

it('VinReportMobileReviews правильную ссылку составит', () => {
    const page = shallow(
        <VinReportMobileReviews reviews={ DATA as unknown as ReportAllReviewsBlock }/>,
        { context: { ...contextMock } },
    );

    const RESULT = 'link/reviews-listing/?from=report&mark=VOLKSWAGEN&model=POLO&super_gen=20113124&parent_category=cars';
    expect(page.find('Button').prop('url')).toBe(RESULT);
});
