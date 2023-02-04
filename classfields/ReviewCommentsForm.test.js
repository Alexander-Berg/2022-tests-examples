/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

const React = require('react');
const { shallow } = require('enzyme');

const contextMock = require('autoru-frontend/mocks/contextMock').default;
const mockStore = require('autoru-frontend/mocks/mockStore').default;

const ReviewCommentsForm = require('./ReviewCommentsForm');

const userMock = require('auto-core/react/dataDomain/user/mocks').default;

let state;
let props;

beforeEach(() => {
    state = {
        reviewComments: {},
        user: userMock.withAuth(true).value(),
    };

    props = {
        commentId: 'comment-id',
        isJournal: false,
        isMobile: false,
        postComment: jest.fn(() => Promise.resolve()),
    };
});

it('должен брать имя пользователя для формы комментария (если есть)', () => {
    state.user = userMock.withAuth(true).withFullName('Рудольф').value();
    const wrapper = shallowRenderComponent({ props, state });

    expect(wrapper.find('.ReviewCommentsItem__header-wrapper').childAt(0).text()).toBe('Рудольф');
});

it('должен брать алиас пользователя для формы комментария (если имени нет)', () => {
    state.user = userMock.withAuth(true).withFullName('').value();
    const wrapper = shallowRenderComponent({ props, state });

    expect(wrapper.find('.ReviewCommentsItem__header-wrapper').childAt(0).text()).toBe('J.DOE');
});

describe('кнопка отправки комментария', () => {
    it('покажется, если инпут заполнен (с фокусом и без)', () => {
        const wrapper = shallowRenderComponent({ props, state });

        const input = wrapper.find('.ReviewComments__textarea');

        // ставим фокус
        input.simulate('focusChange', true);
        const sendButton1 = wrapper.find('.ReviewCommentsForm__textareaIcon');
        expect(sendButton1.isEmptyRender()).toBe(false);

        // меняем инпут
        input.simulate('change', 'foo');
        const sendButton2 = wrapper.find('.ReviewCommentsForm__textareaIcon');
        expect(sendButton2.isEmptyRender()).toBe(false);

        // снимаем фокус
        input.simulate('focusChange', false);
        const sendButton3 = wrapper.find('.ReviewCommentsForm__textareaIcon');
        expect(sendButton3.isEmptyRender()).toBe(false);
    });

    it('не покажется, если инпут не заполнен и нет фокуса', () => {
        const wrapper = shallowRenderComponent({ props, state });

        const input = wrapper.find('.ReviewComments__textarea');

        // изначально ничего не заполнено, фокуса нет
        const sendButton1 = wrapper.find('.ReviewCommentsForm__textareaIcon');
        expect(sendButton1.isEmptyRender()).toBe(true);

        // ставим фокус
        input.simulate('focusChange', true);
        const sendButton2 = wrapper.find('.ReviewCommentsForm__textareaIcon');
        expect(sendButton2.isEmptyRender()).toBe(false);

        // снимаем фокус
        input.simulate('focusChange', false);
        const sendButton3 = wrapper.find('.ReviewCommentsForm__textareaIcon');
        expect(sendButton3.isEmptyRender()).toBe(true);
    });

    describe('при клике', () => {
        it('если инпут заполнен не только пробелами, вызовется проп', () => {
            const text = '  foo  bar     42    ';
            const wrapper = shallowRenderComponent({ props, state });

            const input = wrapper.find('.ReviewComments__textarea');
            input.simulate('focusChange', true);
            input.simulate('change', text);

            const sendButton = wrapper.find('.ReviewCommentsForm__textareaIcon');
            sendButton.simulate('click');

            expect(props.postComment).toHaveBeenCalledTimes(1);
            expect(props.postComment).toHaveBeenCalledWith(text, props.commentId);
        });

        it('если инпут заполнен только пробелами, не вызовется проп', () => {
            const text = '      ';
            const wrapper = shallowRenderComponent({ props, state });

            const input = wrapper.find('.ReviewComments__textarea');
            input.simulate('focusChange', true);
            input.simulate('change', text);

            const sendButton = wrapper.find('.ReviewCommentsForm__textareaIcon');
            sendButton.simulate('click');

            expect(props.postComment).toHaveBeenCalledTimes(0);
        });

        it('если инпут не заполнен, не вызовется проп', () => {
            const text = '';
            const wrapper = shallowRenderComponent({ props, state });

            const input = wrapper.find('.ReviewComments__textarea');
            input.simulate('focusChange', true);
            input.simulate('change', text);

            const sendButton = wrapper.find('.ReviewCommentsForm__textareaIcon');
            sendButton.simulate('click');

            expect(props.postComment).toHaveBeenCalledTimes(0);
        });
    });
});

function shallowRenderComponent({ props, state }) {
    const store = mockStore(state);
    const wrapper = shallow(
        <ReviewCommentsForm { ...props }/>, { context: { ...contextMock, store } },
    );

    return wrapper.dive();
}
