jest.mock('auto-core/lib/event-log/statApi');

import React from 'react';
import userEvent from '@testing-library/user-event';
import { render } from '@testing-library/react';

import contextMock from 'autoru-frontend/mocks/contextMock';
import createContextProvider from 'autoru-frontend/mocks/createContextProvider';

import statApi from 'auto-core/lib/event-log/statApi';

import FilterBannerReport, { css } from './FilterBannerReport';

const VALID_PARAMS = {
    product: 'REPORTS',
    context_page: 'PAGE_MAIN',
    context_block: 'BLOCK_GURU',
    context_service: 'SERVICE_AUTORU',
};

const Context = createContextProvider(contextMock);

it('отправляет метрики на показ и на клик для баннера отчётов', () => {
    const { container } = render(
        <Context>
            <FilterBannerReport/>
        </Context>,
    );
    expect(contextMock.metrika.reachGoal).toHaveBeenCalledTimes(1);
    expect(contextMock.metrika.reachGoal).toHaveBeenCalledWith('view-report_banner');
    expect(statApi.logImmediately).toHaveBeenCalledTimes(1);
    expect(statApi.logImmediately).toHaveBeenCalledWith({ vas_show_event: VALID_PARAMS });

    const bannerLink = container.querySelector(`.${ css.root }`);
    bannerLink && userEvent.click(bannerLink);

    expect(contextMock.metrika.reachGoal).toHaveBeenCalledTimes(2);
    expect(contextMock.metrika.reachGoal).toHaveBeenNthCalledWith(2, 'click-report_banner');
    expect(statApi.log).toHaveBeenCalledTimes(1);
    expect(statApi.log).toHaveBeenNthCalledWith(1, { vas_click_navig_event: VALID_PARAMS });
});
