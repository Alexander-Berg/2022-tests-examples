import React from 'react';
import { shallow } from 'enzyme';
import { shallowToJson } from 'enzyme-to-json';

import createContextProvider from 'autoru-frontend/mocks/createContextProvider';
import contextMock from 'autoru-frontend/mocks/contextMock';

import ExclusiveOfferPopupDesktop from './ExclusiveOfferPopupDesktop';
import type { Props } from './ExclusiveOfferPopupDesktop';

it('правильно формирует контент попап для владельца', () => {
    const page = shallowRenderComponent({ isOwner: true });
    const popup = page.find('InfoPopup');

    expect(shallowToJson(popup)).toMatchSnapshot();
});

it('правильно формирует контент попап для не владельца', () => {
    const page = shallowRenderComponent({ });
    const popup = page.find('InfoPopup');

    expect(shallowToJson(popup)).toMatchSnapshot();
});

it('правильно формирует контент для общего блока', () => {
    const page = shallowRenderComponent({ isGeneralBlock: true });
    const popup = page.find('InfoPopup');

    expect(shallowToJson(popup)).toMatchSnapshot();
});

it('при открытии попапа отправит метрику для не владельца если мышь не над самим попапом', () => {
    const page = shallowRenderComponent({ isOwner: false });
    const popup = page.find('InfoPopup');

    popup.simulate('showPopup', { isTargetPopup: false });

    expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledTimes(1);
    expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledWith([ 'exclusive', 'hint_pop_up' ]);
});

it('при открытии попапа отправит метрику для владельца если мышь не над самим попапом', () => {
    const page = shallowRenderComponent({ isOwner: true });
    const popup = page.find('InfoPopup');

    popup.simulate('showPopup', { isTargetPopup: false });

    expect(contextMock.metrika.params).toHaveBeenCalledTimes(1);
    expect(contextMock.metrika.params).toHaveBeenCalledWith([ 'Exclusive_settings', 'hint_pop_up' ]);
});

it('при открытии попапа отправит не метрику если мышь над самим попапом', () => {
    const page = shallowRenderComponent({});
    const popup = page.find('InfoPopup');

    popup.simulate('showPopup', { isTargetPopup: true });

    expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledTimes(0);
});

function shallowRenderComponent(props: Props) {
    const ContextProvider = createContextProvider(contextMock);

    return shallow(
        <ContextProvider>
            <ExclusiveOfferPopupDesktop { ...props }/>
        </ContextProvider>,
    ).dive();
}
