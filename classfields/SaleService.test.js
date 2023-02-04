jest.mock('www-cabinet/react/components/ServicePopup/ServicePopup', () => 'ServicePopup');
const _ = require('lodash');
const React = require('react');
const { shallow } = require('enzyme');
const { shallowToJson } = require('enzyme-to-json');
const SaleService = require('./SaleService');

it('должен вернуть корректный элемент', () => {
    expect(
        shallowToJson(
            shallow(
                <SaleService
                    onServiceClick={ _.noop }
                    active={ true }
                    isFetching={ true }
                    activatedBy="user"
                    className="someClassname"
                    expireDate="20402"
                    deactivationAllowed={ true }
                    disabled={ true }
                    indicator={ 2 }
                    price={ 2000 }
                    service="turbo"
                    tooltip={{
                        title: 'someTooltip',
                    }}
                    iconName="turbo"
                    text="buttonText"
                />,
            ),
        )).toMatchSnapshot();
});
