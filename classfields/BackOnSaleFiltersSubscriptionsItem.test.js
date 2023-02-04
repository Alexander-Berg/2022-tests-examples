import React from 'react';
import { shallow } from 'enzyme';
import BackOnSaleFiltersSubscriptionsItem from './BackOnSaleFiltersSubscriptionsItem';
import subscription from './mocks/subscription';

describe('сохранение почты', () => {
    it('Должен показать ошибку, если пользователь сохраняет невалидный email', () => {
        const backOnSaleFiltersSubscriptionsItem = shallow(
            <BackOnSaleFiltersSubscriptionsItem
                subscription={ subscription }
            />);

        backOnSaleFiltersSubscriptionsItem.find('TextInput').first().simulate('change', 'melnik88');
        backOnSaleFiltersSubscriptionsItem.find('Button').first().simulate('click');
        expect(backOnSaleFiltersSubscriptionsItem.find('TextInput').first().props().error).toBe('Некорректный email адрес');
    });

    it('Должен сохранить email и добавить новый input для добавления email, если все email сохранены', async() => {
        const backOnSaleFiltersSubscriptionsItem = shallow(
            <BackOnSaleFiltersSubscriptionsItem
                saveAction={ () => Promise.resolve(true) }
                subscription={ subscription }
            />);
        backOnSaleFiltersSubscriptionsItem.find('TextInput').at(1).simulate('change', 'melnik88@msn2.com');
        backOnSaleFiltersSubscriptionsItem.find('Button').at(0).simulate('click');
        await Promise.resolve();
        expect(backOnSaleFiltersSubscriptionsItem.find('TextInput')).toHaveLength(3);
    });

    it('Должен сохранить email и не добавлять новое поле, если есть несохраненные email', async() => {
        const backOnSaleFiltersSubscriptionsItem = shallow(
            <BackOnSaleFiltersSubscriptionsItem
                saveAction={ () => Promise.resolve(true) }
                subscription={ subscription }
            />);
        backOnSaleFiltersSubscriptionsItem.find('TextInput').at(0).simulate('change', 'melnik88@msn2.com');
        backOnSaleFiltersSubscriptionsItem.find('TextInput').at(1).simulate('change', 'melnik88@msn3.com');
        backOnSaleFiltersSubscriptionsItem.find('Button').at(0).simulate('click');
        await Promise.resolve();
        expect(backOnSaleFiltersSubscriptionsItem.find('TextInput')).toHaveLength(2);
    });
});
