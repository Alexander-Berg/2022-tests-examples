const React = require('react');
const { shallow } = require('enzyme');

const { nbsp } = require('auto-core/react/lib/html-entities');

const CallsTotalStats = require('./CallsTotalStats');

it('должен рендерить в заголовке круговой диаграммы сумму звонков', () => {
    const tree = shallowRenderComponent();

    const callCount = tree.find('.CallsTotalStats__total > .CallsTotalStats__title > span').at(1);

    expect(callCount.text()).toBe('50 шт.');
});

it('должен рендерить в заголовке статистики период', () => {
    const tree = shallowRenderComponent();
    const diagramTitle = tree.find('.CallsTotalStats__graph > .CallsTotalStats__title');

    expect(diagramTitle.text()).toBe(`Статистика звонков с${ nbsp }29 марта по${ nbsp }28 апреля`);
});

function shallowRenderComponent() {
    return shallow(
        <CallsTotalStats
            callsTotalStats={{ succeed_calls_amount: 20, failed_calls_amount: 30 }}
            callsDailyStats={ [] }
            period={{ from: '2020-03-29', to: '2020-04-28' }}
        />,
    );
}
