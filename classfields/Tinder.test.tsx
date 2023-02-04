import React from 'react';
import userEvent from '@testing-library/user-event';
import { render } from '@testing-library/react';

import type { Props as AbstractProps, State as AbstractState } from './Tinder';
import Tinder from './Tinder';

type Props = AbstractProps<number>;

const INITIAL_PROPS: Props = {
    slideList: [ 1, 2, 3, 4 ],
    onLike: jest.fn(),
    onDislike: jest.fn(),
    onCurrentSlideSet: jest.fn(),
};

class TestTinder extends Tinder<number, Props, AbstractState> {
    renderSlide(options: { item: number }) {
        return <div>{ options.item }</div>;
    }
}

describe('Tinder', () => {
    describe('onCurrentSlideSet', () => {
        it('вызовет на маунт', async() => {
            const onCurrentSlideSetMocked = jest.fn();
            const props = {
                ...INITIAL_PROPS,
                onCurrentSlideSet: onCurrentSlideSetMocked,
            };

            render(getComponent(props));

            expect(onCurrentSlideSetMocked).toHaveBeenCalledTimes(1);
            expect(onCurrentSlideSetMocked).toHaveBeenCalledWith(1);
        });

        it('вызовет когда обновится список слайдов', async() => {
            const onCurrentSlideSetMocked = jest.fn();
            const props = {
                ...INITIAL_PROPS,
                onCurrentSlideSet: onCurrentSlideSetMocked,
            };

            const { rerender } = await render(getComponent(props));

            rerender(getComponent({
                ...props,
                slideList: [ ...props.slideList, 5, 6, 7, 8 ],
            }));

            expect(onCurrentSlideSetMocked).toHaveBeenCalledTimes(2);
            expect(onCurrentSlideSetMocked).toHaveBeenCalledWith(1);
        });
    });

    describe('классы', () => {
        const onLikeMocked = jest.fn();
        const onDislikeMocked = jest.fn();
        const onCurrentSlideSetMocked = jest.fn();

        const props = {
            ...INITIAL_PROPS,
            onLike: onLikeMocked,
            onDislike: onDislikeMocked,
            onCurrentSlideSet: onCurrentSlideSetMocked,
        };

        it('добавляет слайду класс _dislike после дизлайка, дёргает onCurrentSlideSet и onDislike', async() => {
            const { container } = await render(getComponent(props));

            const dislikeButton = document.getElementsByClassName('Tinder__mainControl_dislike')[0];

            userEvent.click(dislikeButton);

            expect(container.getElementsByClassName('Tinder__itemWrapper_prev_dislike')).toHaveLength(1);
            expect(onDislikeMocked).toHaveBeenCalledTimes(1);
            expect(onCurrentSlideSetMocked).toHaveBeenCalledTimes(2);
            expect(onCurrentSlideSetMocked).toHaveBeenCalledWith(1);
        });

        it('добавляет слайду класс _like после дизлайка, дёргает onCurrentSlideSet и onLike', async() => {
            const { container } = await render(getComponent(props));

            const likeButton = document.getElementsByClassName('Tinder__mainControl_like')[0];

            userEvent.click(likeButton);

            expect(container.getElementsByClassName('Tinder__itemWrapper_prev_like')).toHaveLength(1);
            expect(onLikeMocked).toHaveBeenCalledTimes(1);
            expect(onCurrentSlideSetMocked).toHaveBeenCalledTimes(2);
            expect(onCurrentSlideSetMocked).toHaveBeenCalledWith(1);
        });
    });
});

function getComponent(props: Props = INITIAL_PROPS) {
    return (
        <TestTinder { ...props }/>
    );
}
