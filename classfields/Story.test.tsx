import React from 'react';
import { shallow } from 'enzyme';

import createContextProvider from 'autoru-frontend/mocks/createContextProvider';
import contextMock from 'autoru-frontend/mocks/contextMock';

import mock from 'auto-core/react/dataDomain/stories/mock';

import Story from './Story';
import type { Props } from './Story';

let props: Props;

beforeEach(() => {
    props = {
        currentPage: 0,
        isActive: true,
        story: mock.story.value(),
        onReactionChange: jest.fn(),
        onRetryLoadClick: jest.fn(),
    };
});

describe('метрики', () => {
    describe('при маунте', () => {
        it('отправит, если стори активна и загружена', () => {
            shallowRenderComponent({ props });

            expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledTimes(2);
            expect(contextMock.metrika.sendPageEvent.mock.calls).toMatchSnapshot();
        });

        it('не отправит, если стори неактивна, но загружена', () => {
            props.isActive = false;
            shallowRenderComponent({ props });

            expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledTimes(0);
        });

        it('не отправит, если стори активна, но не загружена', () => {
            props.story = mock.story.withStatus('initial').value();
            shallowRenderComponent({ props });

            expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledTimes(0);
        });
    });

    describe('при переходе на слайд', () => {
        it('отправит, если стори уже загружена', () => {
            props.isActive = false;
            const page = shallowRenderComponent({ props });

            expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledTimes(0);

            page.setProps({ isActive: true } as any);

            expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledTimes(2);
            expect(contextMock.metrika.sendPageEvent.mock.calls).toMatchSnapshot();
        });

        it('не отправит, если стори не загружена', () => {
            props.isActive = false;
            props.story = mock.story.withStatus('initial').value();
            const page = shallowRenderComponent({ props });

            expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledTimes(0);

            page.setProps({ isActive: true } as any);

            expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledTimes(0);
        });
    });

    describe('при смене статуса на success', () => {
        it('отправит, если слайд активен', () => {
            props.story = mock.story.withStatus('initial').value();
            const page = shallowRenderComponent({ props });

            expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledTimes(0);

            page.setProps({ story: mock.story.value() } as any);

            expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledTimes(2);
            expect(contextMock.metrika.sendPageEvent.mock.calls).toMatchSnapshot();
        });

        it('не отправит, если слайд не активен', () => {
            props.isActive = false;
            props.story = mock.story.withStatus('initial').value();
            const page = shallowRenderComponent({ props });

            expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledTimes(0);

            page.setProps({ story: mock.story.value() } as any);

            expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledTimes(0);
        });
    });

    describe('при изменении страницы', () => {
        it('отправит, если слайд активен и стори загружена', () => {
            const page = shallowRenderComponent({ props });
            contextMock.metrika.sendPageEvent.mockClear();

            page.setProps({ currentPage: 1 } as any);

            expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledTimes(1);
            expect(contextMock.metrika.sendPageEvent.mock.calls).toMatchSnapshot();
        });

        it('не отправит, если слайд не активен, но стори загружена', () => {
            props.isActive = false;
            const page = shallowRenderComponent({ props });
            contextMock.metrika.sendPageEvent.mockClear();

            page.setProps({ currentPage: 1 } as any);

            expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledTimes(0);
        });

        it('не отправит, если слайд активен, но стори не загружена', () => {
            props.story = mock.story.withStatus('initial').value();
            const page = shallowRenderComponent({ props });
            contextMock.metrika.sendPageEvent.mockClear();

            page.setProps({ currentPage: 1 } as any);

            expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledTimes(0);
        });
    });
});

function shallowRenderComponent({ props }: { props: Props }) {
    const ContextProvider = createContextProvider(contextMock);

    const page = shallow(
        <ContextProvider>
            <Story { ...props }/>
        </ContextProvider>,
    );

    return page.dive();
}
