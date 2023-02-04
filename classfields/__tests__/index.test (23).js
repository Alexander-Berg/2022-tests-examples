import { shallow } from 'enzyme';
import Link from 'vertis-react/components/Link';
import { withContext } from 'view/lib/test-helpers';
import { FormGroupTypeErrorComponent } from '..';

const getWrapperWithProps = props => withContext(
    shallow,
    <FormGroupTypeErrorComponent {...props} />
);

const getError = wrapper => wrapper.find('.offer-form__submit-error');

describe('FormGroupTypeError', () => {
    it("doesn't show error when form is not failed to submit", () => {
        [ undefined, 'success', 'pending' ].forEach(formStatus => {
            const wrapper = getWrapperWithProps({
                errors: { submit: {} },
                formStatus
            });

            expect(wrapper).toBeEmptyRender();
        });
    });

    it("doesn't show error when there are no submit errors", () => {
        const wrapper = getWrapperWithProps({
            errors: {},
            formStatus: 'fail'
        });

        expect(wrapper).toBeEmptyRender();
    });

    it('shows general error', () => {
        const wrapper = getWrapperWithProps({
            errors: { submit: {} },
            formStatus: 'fail'
        });

        expect(getError(wrapper)).toHaveText('Произошла ошибка. Перезагрузите страницу или попробуйте позже.');
    });

    it('shows captcha error', () => {
        const wrapper = getWrapperWithProps({
            errors: { submit: { code: 'CAPTCHA_CHECK_FAILED' } },
            formStatus: 'fail'
        });

        expect(getError(wrapper)).toHaveText('Вы неверно ввели символы с картинки. Попробуйте ещё раз.');
    });

    it('shows support link for potential spammer', () => {
        const wrapper = getWrapperWithProps({
            errors: { submit: { code: 'USER_LOOKS_LIKE_A_SPAMMER' } },
            formStatus: 'fail'
        });

        const errorNode = getError(wrapper);

        expect(getError(wrapper)).toHaveText(
            'Наш искусственный интеллект заметил у вас подозрительную активность. ' +
            'Если вы считаете, что это ошибка, напишите в службу поддержки.'
        );
        expect(errorNode.find(Link)).toHaveText('напишите в службу поддержки');
    });
});
