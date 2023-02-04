const React = require('react');
import { shallow } from 'enzyme';

const StatsNoData = require('auto-core/react/components/common/StatsNoData');

it('должен показать правильную ошибку, если не выбрана модель', () => {
    const catalogUrl = '#';
    const params = {
        mark: 'Audi',
    };
    const tree = shallow(
        <StatsNoData catalogUrl={ catalogUrl } params={ params }/>,
    );
    const element = tree.find('.StatsNoData__info-text');
    expect(element.text()).toEqual('Чтобы увидеть статистику цен,выберите конкретную модель.');
});

it('должен показать правильную ошибку, если нет результатов выборки', () => {
    const catalogUrl = '#';
    const params = {
        mark: 'Audi',
        model: 'Vesta',
    };
    const tree = shallow(
        <StatsNoData catalogUrl={ catalogUrl } params={ params }/>,
    );
    const element = tree.find('.StatsNoData__info-text');
    expect(element.text()).toEqual('У нас недостаточно данныхВыберите другую марку или модель');
});
