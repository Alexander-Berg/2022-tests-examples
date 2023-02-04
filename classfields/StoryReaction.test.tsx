import React from 'react';
import { shallow } from 'enzyme';

import createContextProvider from 'autoru-frontend/mocks/createContextProvider';
import contextMock from 'autoru-frontend/mocks/contextMock';

import type { Props } from './StoryReaction';
import StoryReaction from './StoryReaction';

let props: Props;

const eventMock = {
    stopPropagation: jest.fn(),
};

beforeEach(() => {
    props = {
        id: 'foo-bar',
        onChangeReaction: jest.fn(),
    };
});

it('обработает лайк', async() => {
    const wrapper = shallowRenderComponent({ props });

    wrapper.find('.StoryReaction__control').first().simulate('click', eventMock);

    expect(props.onChangeReaction).toHaveBeenCalledTimes(1);
    expect(props.onChangeReaction).toHaveBeenCalledWith({
        id: props.id,
        isLiked: true,
        isDisliked: false,
    });
    expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledTimes(1);
    expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledWith([ 'stories', props.id, 'like', 'click' ]);
});

it('обработает дизлайк', async() => {
    const wrapper = shallowRenderComponent({ props });

    wrapper.find('.StoryReaction__control').last().simulate('click', eventMock);

    expect(props.onChangeReaction).toHaveBeenCalledTimes(1);
    expect(props.onChangeReaction).toHaveBeenCalledWith({
        id: props.id,
        isLiked: false,
        isDisliked: true,
    });
    expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledTimes(1);
    expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledWith([ 'stories', props.id, 'dislike', 'click' ]);
});

it('обработает снятие лайка', async() => {
    const wrapper = shallowRenderComponent({ props: { ...props, isLiked: true } });

    wrapper.find('.StoryReaction__control').first().simulate('click', eventMock);

    expect(props.onChangeReaction).toHaveBeenCalledTimes(1);
    expect(props.onChangeReaction).toHaveBeenCalledWith({
        id: props.id,
        isLiked: false,
        isDisliked: false,
    });
    expect(contextMock.metrika.sendPageEvent).not.toHaveBeenCalled();
});

it('обработает снятие дизлайка', async() => {
    const wrapper = shallowRenderComponent({ props: { ...props, isDisliked: true } });

    wrapper.find('.StoryReaction__control').last().simulate('click', eventMock);

    expect(props.onChangeReaction).toHaveBeenCalledTimes(1);
    expect(props.onChangeReaction).toHaveBeenCalledWith({
        id: props.id,
        isLiked: false,
        isDisliked: false,
    });
    expect(contextMock.metrika.sendPageEvent).not.toHaveBeenCalled();
});

it('не обработает лайк, если компонент задизейблен', async() => {
    props.isDisabled = true;
    const wrapper = shallowRenderComponent({ props });

    wrapper.find('.StoryReaction__control').first().simulate('click', eventMock);

    expect(props.onChangeReaction).toHaveBeenCalledTimes(0);
    expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledTimes(0);
});

it('не обработает дизлайк, если компонент задизейблен', async() => {
    props.isDisabled = true;
    const wrapper = shallowRenderComponent({ props });

    wrapper.find('.StoryReaction__control').last().simulate('click', eventMock);

    expect(props.onChangeReaction).toHaveBeenCalledTimes(0);
    expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledTimes(0);
});

function shallowRenderComponent({ props }: { props: Props }) {
    const ContextProvider = createContextProvider(contextMock);

    const page = shallow(
        <ContextProvider>
            <StoryReaction { ...props }/>
        </ContextProvider>,
    );

    return page.dive();
}
