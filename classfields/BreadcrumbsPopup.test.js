const React = require('react');
const { shallow } = require('enzyme');

const BreadcrumbsPopup = require('./BreadcrumbsPopup');

it('Должен вернуть null, если levelData = null', () => {
    const tree = shallow(
        <BreadcrumbsPopup
            fetchStatus="SUCCESS"
            levelData={ null }
        />,
    );
    expect(tree.props().content).toBeNull();
});
