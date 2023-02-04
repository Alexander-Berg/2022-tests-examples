const React = require('react');
const BanMessage = require('./BanMessage');
const { shallow } = require('enzyme');
const offerMock = require('auto-core/react/dataDomain/card/mocks/card.cars.mock');
const contextMock = require('autoru-frontend/mocks/contextMock').default;
import cloneOfferWithHelpers from 'autoru-frontend/mockData/state/helpers/offer/cloneOfferWithHelpers';

it('должен нарисовать кнопку редактировать объявление для частника, если actions.edit = true', () => {
    const offer = cloneOfferWithHelpers(offerMock).withIsOwner(true).withStatus('BANNED').value();
    offer.human_reasons_ban = [ { text: '111' } ];
    const tree = shallow(
        <BanMessage
            offer={ offer }
        />,
        {
            context: { ...contextMock },
        });

    expect(tree.find('.BanMessage__Button').at(0).props().children).toBe('Редактировать объявление');
});

it('должен нарисовать кнопку редактировать объявление для дилера, ' +
    'у которого есть доступ к редактировнию объявлений и actions.edit = true', () => {
    const offer = cloneOfferWithHelpers(offerMock)
        .withSellerTypeCommercial()
        .withIsOwner(true)
        .withStatus('BANNED')
        .value();
    offer.human_reasons_ban = [ { text: '111' } ];
    const tree = shallow(
        <BanMessage
            offer={ offer }
            canWrite={ true }
        />,
        {
            context: { ...contextMock },
        });

    expect(tree.find('.BanMessage__Button').at(0).props().children).toBe('Редактировать объявление');
});

it('не должен нарисовать кнопку редактировать объявление для дилера, ' +
    'если у него нет прав редактировать объявление', () => {
    const offer = cloneOfferWithHelpers(offerMock)
        .withSellerTypeCommercial()
        .withIsOwner(true)
        .withStatus('BANNED')
        .value();
    offer.human_reasons_ban = [ { text: '111' } ];
    const tree = shallow(
        <BanMessage
            offer={ offer }
            canWrite={ false }
        />,
        {
            context: { ...contextMock },
        });

    expect(tree.find('.BanMessage__Button').at(0).props().children).not.toBe('Редактировать объявление');
});
