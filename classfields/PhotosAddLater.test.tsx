/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */
jest.mock('www-poffer/react_bem/lib/metrika', () => ({
    sendParams: jest.fn(),
}));

import React from 'react';
import { Provider } from 'react-redux';
import { mount, shallow } from 'enzyme';
import type { ShallowWrapper } from 'enzyme';

import mockStore from 'autoru-frontend/mocks/mockStore';
import contextMock from 'autoru-frontend/mocks/contextMock';
import createContextProvider from 'autoru-frontend/mocks/createContextProvider';

import metrika from 'www-poffer/react_bem/lib/metrika';

import type { Phone, Props, State } from './PhotosAddLaterDumb';
import PhotosAddlaterDumb from './PhotosAddLaterDumb';
import PhotosAddLater from './PhotosAddLater';

const sendParamsMock = metrika.sendParams as jest.MockedFunction<typeof metrika.sendParams>;
const Context = createContextProvider(contextMock);

describe('PhotosAddLater', () => {
    describe('метрика', () => {
        it('должен отправить лог на маунт компонента', () => {
            const store = mockStore({
                formFields: { data: { phones: { value: [ { phone: '79999999999' } ] } } },
                user: { data: {} },
            });

            mount(
                <Context>
                    <Provider store={ store }>
                        <PhotosAddLater/>
                    </Provider>
                </Context>,
            );

            expect(sendParamsMock).toHaveBeenCalledTimes(1);
            expect(sendParamsMock).toHaveBeenLastCalledWith([
                'ADD_FORM_USER', 'cars', 'add', 'long_form', 'optional',
                'photo', 'phone', 'valid_number', 'verified', 'autofill',
            ]);
        });

        it('должен отправить лог при сбросе номера', () => {
            const formFieldsData = { phones: { value: [ { phone: '79999999999' } ] } };

            const component = renderComponent(formFieldsData);

            component.find('PhoneInput').simulate('reset');

            expect(sendParamsMock).toHaveBeenCalledTimes(2);
            expect(sendParamsMock.mock.calls[1][0]).toEqual([
                'ADD_FORM_USER', 'cars', 'add', 'long_form', 'optional',
                'photo', 'phone', 'delete_number',
            ]);
        });

        it('должен отправить лог при ошибке подтверждения номера', () => {
            const formFieldsData = {};

            const component = renderComponent(formFieldsData);

            component.find('Connect(LazyPhoneAuthAbstract)').simulate('phoneError');

            expect(sendParamsMock).toHaveBeenCalledTimes(1);
            expect(sendParamsMock.mock.calls[0][0]).toEqual([
                'ADD_FORM_USER', 'cars', 'add', 'long_form', 'optional',
                'photo', 'phone', 'valid_number', 'error',
            ]);
        });

        it('должен отправить лог при неправильно набранном номере', () => {
            const formFieldsData = {};

            const component = renderComponent(formFieldsData);

            component.find('Connect(LazyPhoneAuthAbstract)').simulate('phoneError', 'PHONE_INVALID');

            expect(sendParamsMock).toHaveBeenCalledTimes(1);
            expect(sendParamsMock.mock.calls[0][0]).toEqual([
                'ADD_FORM_USER', 'cars', 'add', 'long_form', 'optional',
                'photo', 'phone', 'error',
            ]);
        });

        it('должен отправить лог при успешном подтверждении номера', () => {
            const formFieldsData = {};

            const component = renderComponent(formFieldsData);

            component.find('Connect(LazyPhoneAuthAbstract)').simulate('authSuccess');

            expect(sendParamsMock).toHaveBeenCalledTimes(1);
            expect(sendParamsMock.mock.calls[0][0]).toEqual([
                'ADD_FORM_USER', 'cars', 'add', 'long_form', 'optional',
                'photo', 'phone', 'valid_number', 'verified',
            ]);
        });

        it('должен отправить лог при успешном подтверждении номера и использовании для этого саджеста', () => {
            const formFieldsData = {};

            const component = renderComponent(formFieldsData);

            component.find('Connect(LazyPhoneAuthAbstract)').simulate('select');
            component.find('Connect(LazyPhoneAuthAbstract)').simulate('authSuccess');

            expect(sendParamsMock).toHaveBeenCalledTimes(1);
            expect(sendParamsMock.mock.calls[0][0]).toEqual([
                'ADD_FORM_USER', 'cars', 'add', 'long_form', 'optional',
                'photo', 'phone', 'valid_number', 'verified', 'selected',
            ]);
        });
    });

    describe('window._channels', () => {
        const mockedEmit = jest.fn();
        beforeAll(() => {
            Object.defineProperty(window, '_channels', {
                value: () => ({ emit: mockedEmit }),
                writable: true,
            });
        });

        describe('phone-save', () => {
            it('отправит в channels ивент phone-save с null, если номер и так на первом месте', () => {
                const component = renderDumbComponent();

                const newPhones = [ { phone: '77777777777', call_from: '9', call_till: '21' } ];
                component.setProps({ phones: newPhones });

                component.find('Connect(LazyPhoneAuthAbstract)').simulate('select');
                component.find('Connect(LazyPhoneAuthAbstract)').simulate('authSuccess', '77777777777');

                expect(mockedEmit).toHaveBeenCalledWith('phone-save', null);
            });

            it('отправит в channels ивент phone-save с со списком номеров, где первый на первом месте, если его не было', () => {
                const component = renderDumbComponent();

                const newPhones = [ { phone: '78888888888', call_from: '9', call_till: '21' } ];
                component.setProps({ phones: newPhones });

                component.find('Connect(LazyPhoneAuthAbstract)').simulate('select');
                component.find('Connect(LazyPhoneAuthAbstract)').simulate('authSuccess', '77777777777');

                expect(mockedEmit).toHaveBeenCalledWith(
                    'phone-save',
                    '[{"phone":"77777777777","call_from":"9","call_till":"21"},{"phone":"78888888888","call_from":"9","call_till":"21"}]',
                );
            });

            it('отправит в channels ивент phone-save с со списком номеров, где первый на первом месте, если он был не на первом', () => {
                const component = renderDumbComponent();

                const newPhones = [ { phone: '78888888888', call_from: '9', call_till: '21' } ];
                component.setProps({ phones: newPhones });

                component.find('Connect(LazyPhoneAuthAbstract)').simulate('select');
                component.find('Connect(LazyPhoneAuthAbstract)').simulate('authSuccess', '77777777777');

                expect(mockedEmit).toHaveBeenCalledWith(
                    'phone-save',
                    '[{"phone":"77777777777","call_from":"9","call_till":"21"},{"phone":"78888888888","call_from":"9","call_till":"21"}]',
                );
            });
        });

        describe('reset-exp', () => {
            it('отправит в channels ивент reset-exp, если пользователь оказался перекупом', () => {
                const component = renderDumbComponent();

                component.find('Connect(LazyPhoneAuthAbstract)').simulate('select');
                component.find('Connect(LazyPhoneAuthAbstract)').simulate('authResponse', { user: { moderation_status: { reseller: true } } });

                expect(mockedEmit).toHaveBeenCalledWith('reset-exp');
            });

            it('отправит в channels ивент reset-exp, если пользователь оказался дилером', () => {
                const component = renderDumbComponent();

                component.find('Connect(LazyPhoneAuthAbstract)').simulate('select');
                component.find('Connect(LazyPhoneAuthAbstract)').simulate('authResponse', { user: { client_id: '123' } });

                expect(mockedEmit).toHaveBeenCalledWith('reset-exp');
            });

            it('не отправит в channels ивент reset-exp, если пользователь ни дилер, ни перекуп', () => {
                const component = renderDumbComponent();

                component.find('Connect(LazyPhoneAuthAbstract)').simulate('select');
                component.find('Connect(LazyPhoneAuthAbstract)').simulate('authResponse', { user: {} });

                expect(mockedEmit).not.toHaveBeenCalled();
            });
        });

        describe('phone-reset', () => {
            it('отправит в channels ивент phone-reset, если пользователь сбросил номер', () => {
                const phones = [ { phone: '78888888888', call_from: '9', call_till: '21' } ];
                const component = renderDumbComponent({ phones });

                component.find('PhoneInput').simulate('reset');

                expect(mockedEmit).toHaveBeenCalledWith('phone-reset');
            });
        });
    });

    it('сбрасывает confirmedPhone в стейте, когда удалили номер из блока контактов', () => {
        const component = renderDumbComponent({ willAddPhotosLater: true });

        component.setState({ confirmedPhone: '77777777777' });
        component.setProps({ willAddPhotosLater: false });

        expect(component.state().confirmedPhone).toEqual('');
    });
});

function renderComponent(formFieldsData: Record<string, { value: any }>) {
    const store = mockStore({
        formFields: {
            data: formFieldsData,
            isPending: false,
            isCollapsed: false,
            formErrorText: '',
            needActionBeforeSubmit: false,
        },
        user: { data: {} },
    });

    return shallow(
        <Context>
            <Provider store={ store }>
                <PhotosAddLater/>
            </Provider>
        </Context>,
    ).dive().dive().dive();
}

function renderDumbComponent(props?: { phones?: Array<Phone>; willAddPhotosLater?: boolean }): ShallowWrapper<Props, State> {
    const store = mockStore({
        user: { data: {} },
    });

    return shallow<PhotosAddlaterDumb, Props, State>(
        <Context>
            <Provider store={ store }>
                <PhotosAddlaterDumb
                    phones={ props?.phones || [] }
                    images={ [] }
                    willAddPhotosLater={ Boolean(props?.willAddPhotosLater) }
                    sendFormLog={ jest.fn() }
                />
            </Provider>
        </Context>,
    ).dive().dive();
}
