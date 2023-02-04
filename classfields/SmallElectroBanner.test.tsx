import React from 'react';
import { render } from '@testing-library/react';
import { mockAllIsIntersecting } from 'react-intersection-observer/test-utils';
import userEvent from '@testing-library/user-event';

import contextMock from 'autoru-frontend/mocks/contextMock';
import createContextProvider from 'autoru-frontend/mocks/createContextProvider';

import '@testing-library/jest-dom';
import SmallElectroBanner from 'auto-core/react/components/common/SmallElectroBanner/SmallElectroBanner';

const Context = createContextProvider(contextMock);

it('должен отправить метрику при показе', () => {
    renderComponent();
    mockAllIsIntersecting(true);
    expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledWith([ 'electro-banner', 'small', 'show' ]);
});

it('должен отправить метрику при клике', () => {
    renderComponent();
    const banner = document.querySelector('.SmallElectroBanner');
    banner && userEvent.click(banner);
    expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledWith([ 'electro-banner', 'small', 'click' ]);
});

function renderComponent() {
    return render(
        <Context>
            <SmallElectroBanner/>
        </Context>,
    );
}
