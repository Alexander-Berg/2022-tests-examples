const _ = require('lodash');
const React = require('react');
const { shallow } = require('enzyme');
const { TradeInInfoDumb } = require('./TradeInInfo');

jest.mock('./img/trade_in_button.png', () => 'how-to-image');
jest.mock('./img/trade_in_table.png', () => 'how-to-image');

describe('render тесты', () => {
    it('должен отрендерить корректный компонент', () => {
        const tradeInInfoDumbInstance = shallow(
            <TradeInInfoDumb
                canWrite={ true }
                title="title"
                subtitle="subtitle"
                howToTitle="howToTitle"
                aboutCard={ '<p>about&nbsp;card</p>' }
                aboutTable={ '<p>about&nbsp;table</p>' }
                newCarsSwitcher={{ id: 'newCarsSwitcher' }}
                usedCarsSwitcher={{ id: 'usedCarsSwitcher' }}
                onSwitcherToggle={ _.noop }
            />,
        ).instance();

        expect(tradeInInfoDumbInstance.render()).toMatchSnapshot();
    });
});
