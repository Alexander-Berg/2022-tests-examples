import React from 'react';
import { shallow } from 'enzyme';
import { shallowToJson } from 'enzyme-to-json';

import createContextProvider from 'autoru-frontend/mocks/createContextProvider';
import contextMock from 'autoru-frontend/mocks/contextMock';

import ExclusiveOfferPopupMobile from './ExclusiveOfferPopupMobile';
import type { Props } from './ExclusiveOfferPopupMobile';

it('правильно формирует контент попапа для владельца', () => {
    const page = shallowRenderComponent({ isOwner: true });
    const popup = page.find('.ExclusiveOfferPopupMobile');

    expect(shallowToJson(popup)).toMatchSnapshot();
});

it('правильно формирует контент попапа для не владельца', () => {
    const page = shallowRenderComponent({ });
    const popup = page.find('.ExclusiveOfferPopupMobile');

    expect(shallowToJson(popup)).toMatchSnapshot();
});

it('в мобильной версии формирует модальное окно, а не попап', () => {
    const page = shallowRenderComponent({ });

    expect(page.find('Modal')).toExist();
});

it('правильно формирует контент для общего блока', () => {
    const page = shallowRenderComponent({ isGeneralBlock: true });
    const popup = page.find('.ExclusiveOfferPopupMobile');

    expect(shallowToJson(popup)).toMatchSnapshot();
});

it('при открытии модального окна отправит метрику', () => {
    shallowRenderComponent({ popupVisible: true });

    expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledTimes(0);
});

function shallowRenderComponent(props: Props) {
    const ContextProvider = createContextProvider(contextMock);

    return shallow(
        <ContextProvider>
            <ExclusiveOfferPopupMobile { ...props }/>
        </ContextProvider>,
    ).dive();
}
