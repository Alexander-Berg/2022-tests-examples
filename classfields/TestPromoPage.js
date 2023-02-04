const React = require('react');
const { connect } = require('react-redux');

const BasePromoPage = require('../BasePromoPage');

const getBunkerNodeSelector = require('auto-core/react/dataDomain/bunker/selectors/getNode');

require('./TestPromoPage.css');

class TestPromoPage extends BasePromoPage {
    renderContent() {
        return (
            <div className="TestPromoPage">
                { this.renderHeader() }
                { this.renderLead() }
                { this.renderCover() }
                { this.renderSections() }
                { this.renderForm('test-compaing-slug') }
            </div>
        );
    }

    renderCustomBlock(block, key) {
        const { type } = block;

        switch (type) {
            case 'code':
                return this.renderCode(block, key);

            case 'promo-button':
                return this.renderSuperPuperPromoButton(block, key);

            default:
                return null;
        }

    }

    renderCode({ code }, key) {
        return code ? (
            <pre className="TestPromoPage__code" dangerouslySetInnerHTML={{ __html: code }} key={ key }/>
        ) : null;
    }

    renderSuperPuperPromoButton({ text }, key) {
        return text ? (
            <div className="TestPromoPage__button" dangerouslySetInnerHTML={{ __html: text }} key={ key }/>
        ) : null;
    }
}

module.exports = connect((state) => ({
    data: getBunkerNodeSelector(state, 'promo/test_page'),
}))(TestPromoPage);
