const React = require('react');
const { shallow } = require('enzyme');
const { shallowToJson } = require('enzyme-to-json');
const contextMock = require('autoru-frontend/mocks/contextMock').default;

const BankPromoSectionHeader = require('./BankPromoSectionHeader');

describe('рендерит секцию логотипов банковской промо-страницы', () => {
    it('только с фреймом банка', () => {
        const tree = shallow(
            <BankPromoSectionHeader
                onlyFrame={ true }
                isMobile={ false }
                deviceMod="desktop"
                bankName="Test"
            />,
            { context: contextMock },
        );

        expect(shallowToJson(tree)).toMatchSnapshot();
    });

    it('с полным контентом страницы', () => {
        const tree = shallow(
            <BankPromoSectionHeader
                onlyFrame={ false }
                isMobile={ false }
                deviceMod="desktop"
                bankName="Test"
            />,
            { context: contextMock },
        );

        expect(shallowToJson(tree)).toMatchSnapshot();
    });

    it('в мобильной версии', () => {
        const tree = shallow(
            <BankPromoSectionHeader
                onlyFrame={ false }
                isMobile={ true }
                deviceMod="mobile"
                bankName="Test"
            />,
            { context: contextMock },
        );

        expect(shallowToJson(tree)).toMatchSnapshot();
    });
});
