import React from 'react';
import { shallow } from 'enzyme';
import _ from 'lodash';

import contextMock from 'autoru-frontend/mocks/contextMock';
import mockStore from 'autoru-frontend/mocks/mockStore';

import gateApi from 'auto-core/react/lib/gateApi';
import setToRootForever from 'auto-core/react/dataDomain/cookies/actions/setToRootForever';
import { COOKIES_CHANGE } from 'auto-core/react/dataDomain/cookies/types';
import type { CookiesChangeAction } from 'auto-core/react/dataDomain/cookies/types';
import userMock from 'auto-core/react/dataDomain/user/mocks/withoutAuth.mock';
import userWithPhonesMock from 'auto-core/react/dataDomain/user/mocks/withPhones.mock';

import NewCarsLandingPhoneInputForm, { PLACE, FIRST_LETTER_TYPED_COOKIE } from './NewCarsLandingPhoneInputForm';
import type { OwnProps, AppState } from './NewCarsLandingPhoneInputForm';

jest.mock('auto-core/react/lib/gateApi', () => ({
    getResource: jest.fn(),
}));

jest.mock('auto-core/react/dataDomain/cookies/actions/setToRootForever');

const setToRootForeverMock = setToRootForever as jest.MockedFunction<typeof setToRootForever>;
setToRootForeverMock.mockReturnValue({ type: COOKIES_CHANGE, payload: { bar: 'bar' } } as CookiesChangeAction);

const getResource = gateApi.getResource as jest.MockedFunction<typeof gateApi.getResource>;

const responseOk = {
    response: {
        status: {
            code: 0,
        },
    },
};

const responseFail = {
    response: {
        status: {
            code: 4,
        },
    },
};

let state: AppState;
let props: OwnProps;

beforeEach(() => {
    props = {
        selectedMark: 'BMW',
        selectedModel: 'X5',
        place: PLACE.BANNER,
        onSubmit: jest.fn(),
    };

    state = {
        cookies: {},
        user: userMock,
    };
});

describe('форма NewCarsLandingPhoneInputForm', () => {
    it('форматирует значение', () => {
        const wrapper = shallowRenderComponent({ props, state });

        const input = wrapper.find('TextInput');
        input.simulate('change', '+790935133');

        const updatedInput = wrapper.find('TextInput');
        expect(updatedInput.prop('value')).toBe('+7 909 351-33');
    });

    it('если до этого была ошибка то сбросит её', () => {
        const wrapper = shallowRenderComponent({ props, state });

        wrapper.instance().setState({ error: 'error' });

        const input = wrapper.find('TextInput');
        expect(input.prop('error')).toBe('error');

        input.simulate('change', '+790935133');

        const updatedInput = wrapper.find('TextInput');
        expect(updatedInput.prop('error')).toBe('');
    });

    it('отправляет форму при валидном телефоне', () => {
        const gateApiPromise = Promise.resolve(responseOk);
        getResource.mockImplementation(() => gateApiPromise);
        const wrapper = shallowRenderComponent({ props, state });

        const input = wrapper.find('TextInput');
        input.simulate('change', '+79093513333');

        const button = wrapper.find('Button');
        button.simulate('click');

        return gateApiPromise.then(() => {
            expect(props.onSubmit).toHaveBeenCalledTimes(1);
        });
    });

    it('не отправляет форму при невалидном телефоне и покажет ошибку', () => {
        const gateApiPromise = Promise.resolve(responseOk);
        getResource.mockImplementation(() => gateApiPromise);
        const wrapper = shallowRenderComponent({ props, state });

        const input = wrapper.find('TextInput');
        input.simulate('change', '+7909351');

        const button = wrapper.find('Button');
        button.simulate('click');

        const updatedInput = wrapper.find('TextInput');
        expect(updatedInput.prop('error')).toBe('Неверный формат номера');

        return gateApiPromise.then(() => {
            expect(props.onSubmit).toHaveBeenCalledTimes(0);
        });
    });

    it('отправляет форму при нажатии на Enter', () => {
        const wrapper = shallowRenderComponent({ props, state });
        const instance: any = wrapper.instance();
        instance.onSubmitForm = jest.fn();

        const input = wrapper.find('TextInput');
        input.simulate('change', '+79093513333');
        input.simulate('keyDown', { keyCode: 13 });

        expect(instance.onSubmitForm).toHaveBeenCalledTimes(1);
    });

    it('покажет нотифайку при проблемах с бэком', () => {
        const gateApiPromise = Promise.resolve(responseFail);
        getResource.mockImplementation(() => gateApiPromise);
        const wrapper = shallowRenderComponent({ props, state });
        const instance: any = wrapper.instance();
        instance.showErrorNotify = jest.fn();

        const input = wrapper.find('TextInput');
        input.simulate('change', '+79093513333');

        const button = wrapper.find('Button');
        button.simulate('click');

        return gateApiPromise.then(() => {
            expect(props.onSubmit).toHaveBeenCalledTimes(0);
            expect(instance.showErrorNotify).toHaveBeenCalledTimes(1);
        });
    });

    describe('метрики', () => {
        it('при отправке формы в баннере', () => {
            const gateApiPromise = Promise.resolve(responseOk);
            getResource.mockImplementation(() => gateApiPromise);
            const wrapper = shallowRenderComponent({ props, state });

            const input = wrapper.find('TextInput');
            input.simulate('change', '+79093513333');

            const button = wrapper.find('Button');
            button.simulate('click');

            return gateApiPromise.then(() => {
                expect(contextMock.metrika.reachGoal).toHaveBeenCalledTimes(2);
                expect(contextMock.metrika.reachGoal).toHaveBeenCalledWith('PHONE_LANDING_SHAPKA');
                expect(contextMock.metrika.reachGoal).toHaveBeenCalledWith('PHONE_LANDING_NEWAUTO_ALL');
            });
        });

        it('при отправке формы в модале', () => {
            const modalProps = _.cloneDeep(props);
            modalProps.place = PLACE.MODAL;
            const gateApiPromise = Promise.resolve(responseOk);
            getResource.mockImplementation(() => gateApiPromise);
            const wrapper = shallowRenderComponent({ props: modalProps, state });

            const input = wrapper.find('TextInput');
            input.simulate('change', '+79093513333');

            const button = wrapper.find('Button');
            button.simulate('click');

            return gateApiPromise.then(() => {
                expect(contextMock.metrika.reachGoal).toHaveBeenCalledTimes(2);
                expect(contextMock.metrika.reachGoal).toHaveBeenCalledWith('PHONE_LANDING_NEWAUTO_POPUP');
                expect(contextMock.metrika.reachGoal).toHaveBeenCalledWith('PHONE_LANDING_NEWAUTO_ALL');
            });
        });

        it('напечатал первый символ', () => {
            const wrapper = shallowRenderComponent({ props, state });

            const input = wrapper.find('TextInput');
            input.simulate('keyDown', { keyCode: 56 });
            input.simulate('keyDown', { keyCode: 57 });
            input.simulate('keyDown', { keyCode: 58 });

            expect(setToRootForeverMock).toHaveBeenCalledTimes(1);
            expect(setToRootForeverMock).toHaveBeenCalledWith(FIRST_LETTER_TYPED_COOKIE, 'true');

            expect(contextMock.metrika.reachGoal).toHaveBeenCalledTimes(1);
            expect(contextMock.metrika.reachGoal).toHaveBeenCalledWith('PHONE_TYPING_LANDING_SHAPKA');
        });

        it('напечатал первый символ и есть кука', () => {
            const newState = _.cloneDeep(state);
            newState.cookies[FIRST_LETTER_TYPED_COOKIE] = 'true';
            const wrapper = shallowRenderComponent({ props, state: newState });

            const input = wrapper.find('TextInput');
            input.simulate('keyDown', { keyCode: 56 });
            input.simulate('keyDown', { keyCode: 57 });
            input.simulate('keyDown', { keyCode: 58 });

            expect(setToRootForeverMock).toHaveBeenCalledTimes(0);
            expect(contextMock.metrika.reachGoal).toHaveBeenCalledTimes(0);
        });
    });

    it('подставит номер телефона, если есть в сессии', () => {
        const wrapper = shallowRenderComponent({ props, state: {
            ...state,
            user: userWithPhonesMock,
        } });
        const form = wrapper.find('.NewCarsLandingPhoneInputForm__form').find('TextInput');
        expect(form.prop('value')).toBe('+7 963 962-91-11');
    });
});

function shallowRenderComponent({ props, state }: { props: OwnProps; state: AppState }) {
    const store = mockStore(state);

    const wrapper = shallow(
        <NewCarsLandingPhoneInputForm { ...props }/>,
        { context: { ...contextMock, store } },
    ).dive();

    return wrapper;
}
