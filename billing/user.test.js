import React from 'react';
import { expect } from 'chai';
import { mount } from 'enzyme';
import sinon from 'sinon';
import { Provider } from 'react-redux';

import configureStore from 'configureStore';
import { constants as commonConstants } from 'modules/app';

import UserComponent from '../containers/user';
import UserLoggedOutComponent from '../containers/userLoggedOut';
import UserLoggedInComponent from '../components/userLoggedIn';

jest.mock('api');

describe('UserComponent', () => {
    const commonData = {
        app: {
            version: 'development',
            env: 'development',
        },
        user: {
            uid: '42',
            login: 'vasya.pupkin',
            balanceClientId: 42,
        },
        info: {
            debugPanelUser: true,
            projectId: '42',
        },
    };
    let store;
    let props;
    let mountedUserComponent;

    const mountUserComponent = () => {
        // eslint-disable-next-line max-len
        mountedUserComponent = mount(<Provider store={store}><UserComponent {...props} /></Provider>);

        return mountedUserComponent;
    };

    beforeEach(() => {
        props = {};
        store = configureStore();
        mountedUserComponent = undefined;
    });

    describe('if no "originalUid" in props', () => {
        beforeEach(() => {
            store.dispatch({
                type: commonConstants.ACTION_TYPES.getCommonSuccess,
                data: commonData,
            });
        });

        it('should render UserLoggedOutComponent', () => {
            const currentUserComponent = mountUserComponent();

            expect(currentUserComponent.find(UserLoggedOutComponent))
                .to.have.length(1);
            expect(currentUserComponent.find(UserLoggedInComponent))
                .to.have.length(0);
        });

        it('should have disabled submit button when value is empty', () => {
            const currentUserComponent = mountUserComponent();
            const currentUserLoggedOutComponent = currentUserComponent.find(UserLoggedOutComponent);

            expect(currentUserComponent.find('input').prop('value')).to.equal('');
            expect(currentUserLoggedOutComponent.instance().state.isInvalid).to.equal(true);
            expect(currentUserLoggedOutComponent.instance().state.uid).to.equal('');

            expect(currentUserComponent.find('button').props().disabled).to.equal(true);
        });

        it('should not call "onSubmitLogIn" when empty value is submitted', () => {
            sinon.stub(UserComponent.WrappedComponent.prototype, 'onSubmitLogIn');

            const currentUserComponent = mountUserComponent();

            currentUserComponent.find('form').simulate('submit');

            expect(UserComponent.WrappedComponent.prototype.onSubmitLogIn.called).to.equal(false);

            UserComponent.WrappedComponent.prototype.onSubmitLogIn.restore();
        });

        it('should not call "onSubmitLogIn" when value === props.uid is submitted', () => {
            sinon.stub(UserComponent.WrappedComponent.prototype, 'onSubmitLogIn');

            const currentUserComponent = mountUserComponent();
            const currentUserLoggedOutComponent = currentUserComponent.find(UserLoggedOutComponent);

            currentUserComponent.find('input').simulate('change', { target: { value: '42' } });

            expect(currentUserComponent.find('input').prop('value')).to.equal('42');
            expect(currentUserLoggedOutComponent.instance().state.isInvalid).to.equal(true);
            expect(currentUserLoggedOutComponent.instance().state.uid).to.equal('42');

            currentUserComponent.find('form').simulate('submit');

            expect(UserComponent.WrappedComponent.prototype.onSubmitLogIn.called).to.equal(false);

            UserComponent.WrappedComponent.prototype.onSubmitLogIn.restore();
        });

        it('should not call "onSubmitLogIn" when value with letters is submitted', () => {
            sinon.stub(UserComponent.WrappedComponent.prototype, 'onSubmitLogIn');

            const currentUserComponent = mountUserComponent();
            const currentUserLoggedOutComponent = currentUserComponent.find(UserLoggedOutComponent);

            currentUserComponent.find('input').simulate('change', { target: { value: '1q2w3e' } });

            expect(currentUserComponent.find('input').prop('value')).to.equal('1q2w3e');
            expect(currentUserLoggedOutComponent.instance().state.isInvalid).to.equal(true);
            expect(currentUserLoggedOutComponent.instance().state.uid).to.equal('1q2w3e');

            currentUserComponent.find('form').simulate('submit');

            expect(UserComponent.WrappedComponent.prototype.onSubmitLogIn.called).to.equal(false);

            UserComponent.WrappedComponent.prototype.onSubmitLogIn.restore();
        });

        it('should call "onSubmitLogIn" when valid value is submitted', () => {
            sinon.stub(UserComponent.WrappedComponent.prototype, 'onSubmitLogIn');

            const currentUserComponent = mountUserComponent();
            const currentUserLoggedOutComponent = currentUserComponent.find(UserLoggedOutComponent);

            currentUserComponent.find('input').simulate('change', { target: { value: '424242' } });

            expect(currentUserComponent.find('input').prop('value')).to.equal('424242');
            expect(currentUserLoggedOutComponent.instance().state.isInvalid).to.equal(false);
            expect(currentUserLoggedOutComponent.instance().state.uid).to.equal('424242');
            expect(currentUserComponent.find('button').props().disabled).to.not.equal(true);

            currentUserComponent.find('form').simulate('submit');

            expect(UserComponent.WrappedComponent.prototype.onSubmitLogIn.called).to.equal(true);

            UserComponent.WrappedComponent.prototype.onSubmitLogIn.restore();
        });
    });

    describe('if "originalUid" in props', () => {
        beforeEach(() => {
            const data = {
                ...commonData,
                user: {
                    ...commonData.user,
                    original: {
                        uid: '4242',
                    },
                },
            };

            store.dispatch({ type: commonConstants.ACTION_TYPES.getCommonSuccess, data });
        });

        it('should render UserLoggedInComponent', () => {
            const currentUserComponent = mountUserComponent();

            expect(currentUserComponent.find(UserLoggedInComponent))
                .to.have.length(1);
            expect(currentUserComponent.find(UserLoggedOutComponent))
                .to.have.length(0);
        });

        it('should call "onSubmitLogOut" when exit button is clicked', () => {
            sinon.stub(UserComponent.WrappedComponent.prototype, 'onSubmitLogOut');

            const currentUserComponent = mountUserComponent();

            currentUserComponent.find('button').simulate('click');

            expect(UserComponent.WrappedComponent.prototype.onSubmitLogOut.called).to.equal(true);

            UserComponent.WrappedComponent.prototype.onSubmitLogOut.restore();
        });
    });
});
