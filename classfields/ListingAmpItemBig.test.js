const React = require('react');
const ListingAmpItemBig = require('./ListingAmpItemBig');
const { mount } = require('enzyme');
const mockOffer = require('autoru-frontend/mockData/responses/offer.mock');
const linkDesktop = require('auto-core/router/auto.ru/react/link');

it('Генерирует амп ссылку на группу автомобилей в ListingAmpItemHeader', () => {
    const wrapper = mount(
        <ListingAmpItemBig
            index={ 2 }
            isGroupItem={ true }
            groupSize={ 32 }
            offer={ mockOffer.offer }
            page={ 1 }
            key="listing"
        />,
        { context: { linkDesktop } });

    expect(wrapper.find('ListingAmpItemHeader'))
        .toHaveProp('url', 'https://autoru_frontend.base_domain/amp/cars/used/group/volkswagen/amarok/6500211-6501722/');
});

it('Генерирует амп ссылку на группу автомобилей в ListingAmpItemGallery', () => {
    const wrapper = mount(
        <ListingAmpItemBig
            index={ 2 }
            isGroupItem={ true }
            groupSize={ 32 }
            offer={ mockOffer.offer }
            page={ 1 }
            key="listing"
        />,
        { context: { linkDesktop } });

    expect(wrapper.find('ListingAmpItemGallery'))
        .toHaveProp('url', 'https://autoru_frontend.base_domain/amp/cars/used/group/volkswagen/amarok/6500211-6501722/');
});

it('Генерирует амп ссылку на автомобиль в ListingAmpItemHeader', () => {
    const wrapper = mount(
        <ListingAmpItemBig
            index={ 2 }
            isGroupItem={ false }
            groupSize={ 32 }
            offer={ mockOffer.offer }
            page={ 1 }
            key="listing"
        />,
        { context: { linkDesktop } });

    expect(wrapper.find('ListingAmpItemHeader'))
        .toHaveProp('url', 'https://autoru_frontend.base_domain/amp/cars/used/sale/volkswagen/amarok/1085612756-310ef607/');
});

it('Генерирует амп ссылку на автомобиль в ListingAmpItemGallery', () => {
    const wrapper = mount(
        <ListingAmpItemBig
            index={ 2 }
            isGroupItem={ false }
            groupSize={ 32 }
            offer={ mockOffer.offer }
            page={ 1 }
            key="listing"
        />,
        { context: { linkDesktop } });

    expect(wrapper.find('ListingAmpItemGallery'))
        .toHaveProp('url', 'https://autoru_frontend.base_domain/amp/cars/used/sale/volkswagen/amarok/1085612756-310ef607/');
});
