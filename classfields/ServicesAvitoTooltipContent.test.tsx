import 'jest-enzyme';
import React from 'react';
import { shallow } from 'enzyme';

import type { ServiceNamesAvito } from 'auto-core/types/Multiposting';

import type { ServiceConfig } from 'www-cabinet/react/components/ServicesAvito/ServicesAvito';

import ServicesAvitoTooltipContent from './ServicesAvitoTooltipContent';

const viewsUpMock = {
    name: 'x2',
    text: 'Просмотров',
    icon: 'icon',
    options: [
        { name: 'x2_1' as ServiceNamesAvito, price: '155' },
        { name: 'x2_7' as ServiceNamesAvito, price: '1000' },
    ],
};
const appearanceMock = {
    name: 'xl',
    text: 'XL',
    icon: 'icon',
    tooltipTitle: 'XL-объявление',
    tooltipText: 'XL-объявления показываются на гигантском билборде в Калифорнии',
    price: '2509',
};

const onViewsUpButtonClick = () => {};

describe('покажет текст про цену, если она пришла', () => {
    it('для услуг увеличения просмотров', () => {
        const tree = shallowRenderComponent(viewsUpMock);
        const buttons = tree.find('.ServicesAvitoTooltipContent__viewsUpButton');

        expect([ buttons.at(0).dive().text(), buttons.at(1).dive().text() ]).toEqual([ 'за 155 ₽1 день', 'за 1 000 ₽7 дней' ]);
    });

    it('для услуг изменения внешнего вида', () => {
        const tree = shallowRenderComponent(appearanceMock);
        expect(tree.find('.ServicesAvitoTooltipContent__appearancePrice')).toExist();
    });
});

describe('не покажет текст про цену, если она не пришла', () => {
    it('для услуг увеличения просмотров', () => {
        const tree = shallowRenderComponent({ ...viewsUpMock, options: [ { name: 'x2_1' }, { name: 'x2_7' } ] });
        const buttons = tree.find('.ServicesAvitoTooltipContent__viewsUpButton');

        expect([ buttons.at(0).dive().text(), buttons.at(1).dive().text() ]).toEqual([ '1 день', '7 дней' ]);
    });

    it('для услуг изменения внешнего вида', () => {
        const tree = shallowRenderComponent({ ...appearanceMock, price: undefined });
        expect(tree.find('.ServicesAvitoTooltipContent__appearancePrice')).not.toExist();
    });
});

function shallowRenderComponent(serviceConfig: ServiceConfig) {
    return shallow(<ServicesAvitoTooltipContent
        onViewsUpButtonClick={ onViewsUpButtonClick }
        serviceConfig={ serviceConfig }
    />);
}
