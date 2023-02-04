jest.mock('auto-core/react/lib/cookie', () => {
    return {
        get: jest.fn(),
        setForever: jest.fn(),
    };
});

const React = require('react');
const { shallow } = require('enzyme');
const { shallowToJson } = require('enzyme-to-json');
const cookie = require('auto-core/react/lib/cookie');

const CallsComplaintForm = require('./CallsComplaintForm');

const callMock = require('www-cabinet/react/dataDomain/calls/mocks/withCalls.mock').callsList.calls[0];

const baseProps = {
    call: callMock,
    sendComplaint: jest.fn(() => Promise.resolve()),
    onClose: jest.fn(),
};

const contextMock = require('autoru-frontend/mocks/contextMock').default;
const createContextProvider = require('autoru-frontend/mocks/createContextProvider').default;
const Context = createContextProvider(contextMock);

const EMAIL = 'demo@auto.ru';

it('если у звонка нет оффера не добавит пункт про секцию', () => {
    const props = {
        ...baseProps,
        call: {
            ...baseProps.call,
            offer: undefined,
        },
    };
    const tree = shallow(
        <Context><CallsComplaintForm { ...props }/></Context>,
    ).dive();

    const wrongSectionItem = tree.find('Item').findWhere((node) => node.prop('value') === 'wrong_section');

    expect(wrongSectionItem.isEmptyRender()).toBe(true);
});

it('должен рендерить инпут кастомной причины, если в селекте выбрано поле "Другая причина"', () => {
    const tree = shallow(
        <Context><CallsComplaintForm { ...baseProps }/></Context>,
    ).dive();

    tree.instance().onChangeReason([ 'other_reason' ]);

    const otherReasonInput = tree.find('.CallsComplaintForm__customReasonInput');

    expect(shallowToJson(otherReasonInput)).not.toBeNull();
});

it('должен подставить email из куки, если она есть', () => {
    cookie.get.mockImplementation((cookieName) => {
        if (cookieName === 'cookie-complaint-email') {
            return EMAIL;
        }
    });

    const tree = shallow(
        <Context><CallsComplaintForm { ...baseProps }/></Context>,
    ).dive();

    tree.instance().componentDidMount();

    expect(tree.state().email).toBe(EMAIL);
});

it('при сабмите должен заполнить куку с email из стейта', () => {
    const tree = shallow(
        <Context><CallsComplaintForm { ...baseProps }/></Context>,
    ).dive();

    const instance = tree.instance();

    instance.onChangeReason([ 'wrong_mark' ]);
    instance.onChangeEmail(EMAIL);
    instance.sendComplaint();

    expect(cookie.setForever).toHaveBeenCalledWith('cookie-complaint-email', EMAIL);
});

it('при сабмите должен вызвать экшен sendComplain с данными из стейта', () => {
    expect.assertions(2);

    const promise = Promise.resolve();

    baseProps.sendComplaint.mockImplementation(() => promise);

    const tree = shallow(
        <Context><CallsComplaintForm { ...baseProps }/></Context>,
    ).dive();

    const instance = tree.instance();

    instance.onChangeReason([ 'wrong_mark' ]);
    instance.onChangeEmail(EMAIL);

    instance.sendComplaint();

    return promise.then(() => {
        expect(baseProps.sendComplaint).toHaveBeenCalledWith({
            call: baseProps.call,
            text: 'Звонок по другой марке',
            email: EMAIL,
        });

        expect(baseProps.onClose).toHaveBeenCalled();

    });
});
