const React = require('react');
const { shallow } = require('enzyme');

const BankFrameDisclaimer = require('./BankFrameDisclaimer');

describe('рендерит дисклеймер для кредитов', () => {
    it('в десктопе', () => {
        const tree = shallow(
            <BankFrameDisclaimer companyName="ПАО TEST" bankProduct="credit" isMobile={ false }/>,
        );

        expect(tree.find('.BankFrameDisclaimer').text()).toMatchSnapshot();
    });

    it('в мобильной версии', () => {
        const tree = shallow(
            <BankFrameDisclaimer companyName="ПАО TEST" bankProduct="credit" isMobile={ true }/>,
        );

        expect(tree.find('.BankFrameDisclaimer').text()).toMatchSnapshot();
    });
});

describe('рендерит дисклеймер для страховок', () => {
    it('в десктопе', () => {
        const tree = shallow(
            <BankFrameDisclaimer companyName="ПАО TEST" bankProduct="osago" isMobile={ false }/>,
        );

        expect(tree.find('.BankFrameDisclaimer').text()).toMatchSnapshot();
    });

    it('в мобильной версии', () => {
        const tree = shallow(
            <BankFrameDisclaimer companyName="ПАО TEST" bankProduct="osago" isMobile={ true }/>,
        );

        expect(tree.find('.BankFrameDisclaimer').text()).toMatchSnapshot();
    });
});
