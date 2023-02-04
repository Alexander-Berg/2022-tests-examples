import React from 'react';
import { shallow } from 'enzyme';
import { noop } from 'lodash';

import createContextProvider from 'autoru-frontend/mocks/createContextProvider';
import contextMock from 'autoru-frontend/mocks/contextMock';

import FiltersPopup from 'auto-core/react/components/mobile/FiltersPopup/FiltersPopup';

const Context = createContextProvider(contextMock);

it('должен рендерить хедер с тенью при проскролле', () => {
    const wrapper = shallow(
        <Context>
            <FiltersPopup
                onClose={ noop }
                title="Параметры"
                onDone={ noop }
                doneText="Готово"
            >
                Контент
            </FiltersPopup>
        </Context>,
    ).dive();

    wrapper.find('.FiltersPopup__content').simulate('scroll', { currentTarget: { scrollTop: 100 } });
    expect(wrapper.find('.FiltersPopup__header_withShadow')).toExist();
});
