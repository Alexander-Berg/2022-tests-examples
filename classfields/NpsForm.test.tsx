import React from 'react';
import userEvent from '@testing-library/user-event';
import { render } from '@testing-library/react';

import createContextProvider from 'autoru-frontend/mocks/createContextProvider';
import contextMock from 'autoru-frontend/mocks/contextMock';

import NpsForm from './NpsForm';
import type { Props } from './NpsForm';

let props: Props;

beforeEach(() => {
    props = {
        renderSubmittedScreen: () => <div/>,
        onStarClick: jest.fn(),
        onSubmit: jest.fn(),
    };
});

describe('тексты', () => {
    const CASES = [
        { name: '⭐️', num: 1, headerText: /Похоже, вы расстроены/i, subheaderText: /Расскажите, что не так/i },
        { name: '⭐️⭐️', num: 2, headerText: /Похоже, вы расстроены/i, subheaderText: /Расскажите, что не так/i },
        { name: '⭐️⭐️⭐️', num: 3, headerText: /Расскажите почему?/i, subheaderText: /Как нам стать лучше?/i },
        { name: '⭐️⭐️⭐️⭐️', num: 4, headerText: /Спасибо!/i, subheaderText: /Может, у вас есть совет, как нам стать ещё лучше?/i },
        { name: '⭐️⭐️⭐️⭐️⭐️', num: 5, headerText: /Вы нам тоже очень нравитесь!/i, subheaderText: /Расскажите, что вам понравилось?/i },
    ];

    CASES.forEach(({ headerText, name, num, subheaderText }) => {
        it(name, async() => {
            const { findByLabelText, queryByText } = shallowRenderComponent({ props });
            const star = await findByLabelText(`Оценка: ${ num }`);
            userEvent.click(star);
            const header = queryByText(headerText);
            const subheader = queryByText(subheaderText);
            expect(header).not.toBeNull();
            expect(subheader).not.toBeNull();
        });
    });
});

describe('звезды', () => {
    it('выбирает по клику', async() => {
        const { findByLabelText } = shallowRenderComponent({ props });

        const star = await findByLabelText('Оценка: 1');
        const starIcon = star.querySelector('svg');
        expect(starIcon?.classList.contains('NpsForm__star_active')).toBe(false);

        userEvent.click(star);

        expect(starIcon?.classList.contains('NpsForm__star_active')).toBe(true);
        expect(props.onStarClick).toHaveBeenCalledTimes(1);
        expect(props.onStarClick).toHaveBeenCalledWith(1);
    });

    it('клик на другую сделает активной ее и все предыдущие', async() => {
        const { findByLabelText } = shallowRenderComponent({ props });

        const star = await findByLabelText('Оценка: 1');
        userEvent.click(star);

        const star2 = await findByLabelText('Оценка: 2');
        userEvent.click(star2);

        const starIcon = star.querySelector('svg');
        const starIcon2 = star2.querySelector('svg');

        expect(starIcon?.classList.contains('NpsForm__star_active')).toBe(true);
        expect(starIcon2?.classList.contains('NpsForm__star_active')).toBe(true);
    });
});

describe('комментарий', () => {
    it('не показывается пока не выбрана оценка', async() => {
        const { findByLabelText, queryByRole } = shallowRenderComponent({ props });

        let comment = queryByRole('textbox');
        expect(comment).toBeNull();

        const star = await findByLabelText('Оценка: 1');
        userEvent.click(star);

        comment = queryByRole('textbox');
        expect(comment).not.toBeNull();
    });

    it('заполняется', async() => {
        const { findByLabelText, findByRole } = shallowRenderComponent({ props });

        const star = await findByLabelText('Оценка: 1');
        userEvent.click(star);

        const comment = await findByRole('textbox') as HTMLTextAreaElement;
        userEvent.type(comment, 'foo');

        expect(comment.value).toBe('foo');
    });
});

describe('сабмит формы', () => {
    it('покажет ошибку если коммент не заполнен', async() => {
        const { findByLabelText, findByText, queryByText } = shallowRenderComponent({ props });

        const star = await findByLabelText('Оценка: 1');
        userEvent.click(star);

        const button = await findByText('Отправить');
        userEvent.click(button);

        const error = await queryByText(/Напишите хоть что-нибудь, пожалуйста/i);

        expect(error).not.toBeNull();
        expect(props.onSubmit).toHaveBeenCalledTimes(0);
    });

    it('вызовет проп если комментарий заполнен', async() => {
        const { findByLabelText, findByRole, findByText, queryByText } = shallowRenderComponent({ props });

        const star = await findByLabelText('Оценка: 1');
        userEvent.click(star);

        const comment = await findByRole('textbox') as HTMLTextAreaElement;
        userEvent.type(comment, 'foo');

        const button = await findByText('Отправить');
        userEvent.click(button);

        const error = await queryByText(/Напишите хоть что-нибудь, пожалуйста/i);

        expect(error).toBeNull();
        expect(props.onSubmit).toHaveBeenCalledTimes(1);
        expect(props.onSubmit).toHaveBeenCalledWith(1, 'foo');
    });
});

function shallowRenderComponent({ props }: { props: Props }) {
    const ContextProvider = createContextProvider(contextMock);

    return render(
        <ContextProvider>
            <NpsForm { ...props }/>
        </ContextProvider>,
    );
}
