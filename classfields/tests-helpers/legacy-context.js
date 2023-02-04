const React = require('react');
const PropTypes = require('prop-types');

module.exports = function(data) {
    class LegacyContext extends React.Component {
        getChildContext() {
            return data;
        }

        render() {
            return this.props.children;
        }
    }

    LegacyContext.childContextTypes = Object.keys(data).reduce((acc, key) => {
        acc[key] = PropTypes.any;
        return acc;
    }, {});

    return LegacyContext;
};
