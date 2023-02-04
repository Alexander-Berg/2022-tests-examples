import React from 'react';
import { render } from '@testing-library/react';

import contextMock from 'autoru-frontend/mocks/contextMock';
import createContextProvider from 'autoru-frontend/mocks/createContextProvider';

import reviewsMock from 'auto-core/react/dataDomain/defaultVinReport/mocks/reviewsMock';

import VinReportPDFReviewsByVIN from './VinReportPDFReviewsByVIN';

const Context = createContextProvider(contextMock);

it('должен отрисовать блок отзывов, если есть отзывы', () => {
    const { container } = render(
        <Context>
            <VinReportPDFReviewsByVIN reviews={ reviewsMock.value() }/>
        </Context>,
    );

    expect(container.querySelector('.VinReportPDFReviewsByVIN')).not.toBeNull();
});

it('не должен отрисовать блок отзывов, если нет отзывов', () => {
    const { container } = render(
        <Context>
            <VinReportPDFReviewsByVIN reviews={ (reviewsMock as any).withoutRecords().value() }/>
        </Context>,
    );
    expect(container.querySelector('.VinReportPDFReviewsByVIN')).toBeNull();
});

it('не должен отрисовать блок отзывов, если статус не ок', () => {
    const { container } = render(
        <Context>
            <VinReportPDFReviewsByVIN reviews={ (reviewsMock as any).withUnknownStatus().value() }/>
        </Context>,
    );
    expect(container.querySelector('.VinReportPDFReviewsByVIN')).toBeNull();
});
