jest.mock('auto-core/lib/clipboard', () => ({ copyText: jest.fn() }));

import React from 'react';
import type { ShallowWrapper } from 'enzyme';
import { shallow } from 'enzyme';

import { copyText } from 'auto-core/lib/clipboard';

import CopyableTextWithTooltip from './CopyableTextWithTooltip';

const DEFAULT_TEXT = 'Хорошо, когда снег! Он срам земной прикрывает. И душа чище от него делается.';

it('должен показывать прочерк, если текста нет', () => {
    const tree = shallowRenderComponent(null);

    expect(tree.text()).toEqual('—');
});

it('должен копировать текст при клике на текст', () => {
    const tree = shallowRenderComponent();
    tree.simulate('click');

    expect(copyText).toHaveBeenCalledWith(DEFAULT_TEXT);
});

function shallowRenderComponent(text: string | null = DEFAULT_TEXT): ShallowWrapper {
    return shallow(
        <CopyableTextWithTooltip text={ text }/>,
    );
}
