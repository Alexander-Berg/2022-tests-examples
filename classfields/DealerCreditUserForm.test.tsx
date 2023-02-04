import 'jest-enzyme';
import React from 'react';
import { shallow } from 'enzyme';
import _ from 'lodash';

import contextMock from 'autoru-frontend/mocks/contextMock';
import mockStore from 'autoru-frontend/mocks/mockStore';

import userStateMock from 'auto-core/react/dataDomain/user/mocks/withAuth.mock';

import DealerCreditUserForm from './DealerCreditUserForm';

const storeMock = {
    user: _.cloneDeep(userStateMock),
};

const submitFuncMock = jest.fn();

const defaultProps = {
    dealerName: 'Питер Брейгель',
    isMobile: false,
    onSubmit: submitFuncMock,
};

it('не вызовет onSubmit, если есть неправильно заполненные поля', () => {
    const wrapper = shallow(
        <DealerCreditUserForm { ...defaultProps }/>,
        { context: { ...contextMock, store: mockStore(storeMock) } },
    ).dive();

    wrapper.setState({ phoneErrorMessage: 'ошибка' });

    expect(submitFuncMock).not.toHaveBeenCalled();
});

it('покажет дисклэймер для десктопа', () => {
    const wrapper = shallow(
        <DealerCreditUserForm { ...defaultProps }/>,
        { context: { ...contextMock, store: mockStore(storeMock) } },
    ).dive();

    expect(wrapper.find('InfoPopup')).toExist();
});

it('покажет дисклэймер для мки', () => {
    const wrapper = shallow(
        <DealerCreditUserForm { ...defaultProps } isMobile={ true }/>,
        { context: { ...contextMock, store: mockStore(storeMock) } },
    ).dive();

    expect(wrapper.find('Modal')).toExist();
});
