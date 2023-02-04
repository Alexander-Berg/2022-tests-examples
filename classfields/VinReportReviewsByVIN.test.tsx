import React from 'react';
import { render } from '@testing-library/react';

import contextMock from 'autoru-frontend/mocks/contextMock';
import createContextProvider from 'autoru-frontend/mocks/createContextProvider';

import reviewsMock from 'auto-core/react/dataDomain/defaultVinReport/mocks/reviewsMock';

import VinReportReviewsByVIN from './VinReportReviewsByVIN';

const Context = createContextProvider(contextMock);

it('должен отрисовать блок отзывов, если есть отзывы', () => {
    const { container } = render(
        <Context>
            <VinReportReviewsByVIN
                reviews={ reviewsMock.value() }
                renderGallery={ () => null }
            />
        </Context>,
    );

    expect(container.querySelector('.VinReportReviewsByVIN')).not.toBeNull();
});

it('не должен отрисовать блок отзывов, если нет отзывов', () => {
    const { container } = render(
        <Context>
            <VinReportReviewsByVIN
                reviews={ (reviewsMock as any).withoutRecords().value() }
                renderGallery={ () => null }
            />
        </Context>,
    );
    expect(container.querySelector('.VinReportReviewsByVIN')).toBeNull();
});

it('не должен отрисовать блок отзывов, если статус не ок', () => {
    const { container } = render(
        <Context>
            <VinReportReviewsByVIN
                reviews={ (reviewsMock as any).withUnknownStatus().value() }
                renderGallery={ () => null }
            />
        </Context>,
    );
    expect(container.querySelector('.VinReportReviewsByVIN')).toBeNull();
});
