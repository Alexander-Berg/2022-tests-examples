const React = require('react');
const { shallow } = require('enzyme');
const { shallowToJson } = require('enzyme-to-json');

const HeaderBalanceFormDumb = require('./HeaderBalanceFormDumb');

it('должен рендерить только компонент баланса, если нет доступа к пополнению баланса', () => {
    const wrapper = shallow(
        <HeaderBalanceFormDumb
            overdraftInfo={{}}
            isAlreadyPaidOverdraft={ false }
            canRechargeBalance={ false }
        />,
    );

    expect(shallowToJson(wrapper)).toMatchSnapshot();
});
