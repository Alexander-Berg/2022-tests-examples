const React = require('react');
const { shallow } = require('enzyme');
const _ = require('lodash');

const contextMock = require('autoru-frontend/mocks/contextMock').default;

const WalkInCollapseBlock = require('./WalkInCollapseBlock');

it('при схлопывании должен отсылать метрику', () => {
    const tree = shallow(
        <WalkInCollapseBlock isExpanded={ true } onCollapse={ _.noop } onExpand={ _.noop }/>,
        { context: contextMock },
    );

    const button = tree.find('.WalkInCollapseBlock__collapseButton');
    button.simulate('click');

    expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledWith([ 'legend', 'collapse' ]);
});

it('при расхлопывании отсылать метрику', () => {
    const tree = shallow(
        <WalkInCollapseBlock isExpanded={ false } onCollapse={ _.noop } onExpand={ _.noop }/>,
        { context: contextMock },
    );
    const button = tree.find('.WalkInCollapseBlock__collapseButton');
    button.simulate('click');

    expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledWith([ 'legend', 'expand' ]);
});
