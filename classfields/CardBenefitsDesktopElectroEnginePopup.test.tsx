import React from 'react';
import { render } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import contextMock from 'autoru-frontend/mocks/contextMock';
import createContextProvider from 'autoru-frontend/mocks/createContextProvider';

import '@testing-library/jest-dom';
import CardBenefitsDesktopElectroEnginePopup
    from 'www-desktop/react/components/CardBenefitsDesktop/CardBenefitsDesktopElectroEnginePopup/CardBenefitsDesktopElectroEnginePopup';

const Context = createContextProvider(contextMock);

it('должен отправить метрику при клике', () => {
    renderComponent();
    const banner = document.querySelector('.CardBenefitsDesktopElectroEnginePopup__button');
    banner && userEvent.click(banner);
    expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledWith([ 'advantages', 'electro_engine', 'popup', 'button', 'click' ]);
});

function renderComponent() {
    return render(
        <Context>
            <CardBenefitsDesktopElectroEnginePopup/>
        </Context>,
    );
}
