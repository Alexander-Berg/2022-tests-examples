import cssSelectors from '../../common/css-selectors';
import counterGenerator from '../../lib/counter-generator';
import getSelectorByText from '../../lib/func/get-selector-by-text';

const url = '?orgpage[id]=104142921758';

counterGenerator({
    name: 'Orgpage.',
    specs: [
        {
            name: 'maps_www.orgpage.content.header.phones.principal.show_phone',
            description: 'Кнопка "Показать телефон"',
            url,
            selector: cssSelectors.orgpage.header.phones.moreButton,
            events: [
                {
                    type: 'click'
                }
            ]
        },
        {
            name: 'maps_www.orgpage.content.actions.carousel.bookmark',
            description: 'Кнопка "Добавить в избранное"',
            url,
            selector: cssSelectors.actionBar.bookmarkButton,
            login: true,
            events: [
                {
                    type: 'click',
                    state: {state: 'unsaved'}
                },
                {
                    type: 'change_state',
                    state: {state: 'saved'},
                    setup: async (browser) => {
                        await browser.waitAndClick(cssSelectors.actionBar.bookmarkButton);
                        await browser.waitAndClick(
                            getSelectorByText('Избранное', cssSelectors.bookmarks.selectFolder.view)
                        );
                        await browser.waitAndClick(cssSelectors.actionBar.bookmarkButton);
                        await browser.waitForVisible(cssSelectors.actionBar.bookmarkButtonChecked);
                    }
                }
            ]
        },
        {
            name: 'maps_www.orgpage.content.actions.carousel.build_route',
            description: 'Кнопка "Построить маршрут"',
            url,
            selector: cssSelectors.actionBar.routeButton,
            events: [
                {
                    type: 'click'
                }
            ]
        },
        {
            name: 'maps_www.orgpage.content.actions.carousel.send_to_phone',
            description: 'Кнопка шаринга ссылки',
            url,
            selector: cssSelectors.actionBar.sendToPhoneButton,
            events: [
                {
                    type: 'click'
                }
            ]
        },
        {
            name: 'maps_www.orgpage.content.show_entrances',
            description: 'Кнопка "Показать входы"',
            url,
            selector: cssSelectors.orgpage.contacts.showEntrances,
            events: [
                {
                    type: 'click'
                }
            ]
        },
        {
            name: 'maps_www.orgpage.content.panorama',
            description: 'Панорама',
            url,
            selector: cssSelectors.orgpage.contacts.panorama,
            events: [
                {
                    type: 'click'
                }
            ]
        },
        {
            name: 'maps_www.orgpage.content.masstransit.metro_route',
            description: 'Расстояние до остановки',
            url,
            selector: cssSelectors.orgpage.contacts.masstransit.metro.distance,
            events: [
                {
                    state: {name: '*'},
                    type: 'click'
                }
            ]
        }
    ]
});
