const React = require('react');
const { shallow } = require('enzyme');
const AutostrategyDumb = require('./AutostrategyDumb').default;

const contextMock = require('autoru-frontend/mocks/contextMock').default;

it('должен задизейблить кнопку автостратегий, если не выбраны mm и mmm', async() => {
    const wrapper = shallow(
        <AutostrategyDumb
            visible={ true }
            limit={ 5 }
            period={{
                to: '2022-01-18',
                from: '2021-01-18',
            }}
            mm={ false }
            mmm={ false }
        />,
        { context: contextMock },
    );

    expect(wrapper.find('.Autostrategy__footer').props().disabled).toBe(true);
});
