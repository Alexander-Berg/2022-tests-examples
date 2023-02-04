/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

jest.mock('../../actions/form', () => {
    return {
        changeForm: jest.fn((...args) => ({ type: 'MOCK_CHANGE_FORM_ACTION', args })),
    };
});

const React = require('react');
const { shallow } = require('enzyme');

const mockStore = require('autoru-frontend/mocks/mockStore').default;

const Fingerprint = require('./Fingerprint');

it('должен вычислить fingerprint, если его нет', () => {
    const store = mockStore({});

    shallow(<Fingerprint params={{}}/>, {
        context: { store },
    }).dive();

    const expectedActions = [
        { type: 'MOCK_CHANGE_FORM_ACTION', args: [ 'fingerprint', 'fingerprint2_mock_value', {} ] },
    ];

    expect(store.getActions()).toEqual(expectedActions);
});

it('не должен вычислить fingerprint, если он есть', () => {
    const store = mockStore({
        formFields: {
            data: {
                fingerprint: { value: 'foo' },
            },
        },
    });

    shallow(<Fingerprint params={{}}/>, {
        context: { store },
    }).dive();

    expect(store.getActions()).toEqual([]);
});
