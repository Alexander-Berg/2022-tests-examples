import cssSelectors from '../../common/css-selectors';
import getSelectorByText from '../../lib/func/get-selector-by-text';
import counterGenerator from '../../lib/counter-generator';

counterGenerator({
    name: 'Контролы карты.',
    specs: [
        {
            name: 'maps_www',
            description: 'Карта',
            events: [
                {
                    type: 'show',
                    state: {region: '*'}
                }
            ]
        },
        {
            name: 'maps_www.geolocation',
            description: 'Контрол геолокации',
            selector: cssSelectors.mapControls.geolocation,
            events: [
                {
                    type: 'show'
                },
                {
                    type: 'click'
                }
            ]
        },
        {
            name: 'maps_www.traffic_control',
            description: 'Контрол пробок - выключенный',
            selector: cssSelectors.mapControls.traffic.mainControl,
            events: [
                {
                    type: 'show',
                    state: {checked: false}
                },
                {
                    type: 'click',
                    state: {checked: false}
                },
                {
                    type: 'change_state',
                    state: {checked: true},
                    setup: async (browser) => {
                        await browser.waitAndClick(cssSelectors.mapControls.traffic.mainControlDisabled);
                        await browser.waitForVisible(cssSelectors.mapControls.traffic.mainControlEnabled);
                    }
                }
            ]
        },
        {
            name: 'maps_www.traffic_control',
            description: 'Контрол пробок - включенный',
            selector: cssSelectors.mapControls.traffic.mainControl,
            url: '?l=trf',
            events: [
                {
                    type: 'show',
                    state: {checked: true}
                },
                {
                    type: 'click',
                    state: {checked: true}
                },
                {
                    type: 'change_state',
                    state: {checked: false},
                    setup: async (browser) => {
                        await browser.waitAndClick(cssSelectors.mapControls.traffic.mainControlEnabled);
                        await browser.waitForVisible(cssSelectors.mapControls.traffic.mainControlDisabled);
                    }
                }
            ]
        },
        {
            name: 'maps_www.search_form.routes_control',
            description: 'Контрол маршрутов',
            selector: cssSelectors.mapControls.routes,
            events: [
                {
                    type: 'show'
                },
                {
                    type: 'click'
                }
            ]
        },
        {
            name: 'maps_www.panorama_control',
            description: 'Контрол панорамы - выключенный',
            selector: cssSelectors.mapControls.panoramaPhoto.mainControl,
            events: [
                {
                    type: 'show',
                    state: {checked: false}
                },
                {
                    type: 'click',
                    state: {checked: false}
                },
                {
                    type: 'change_state',
                    state: {checked: true},
                    setup: async (browser) => {
                        await browser.waitAndClick(cssSelectors.mapControls.panoramaPhoto.mainControlDisabled);
                        await browser.waitForVisible(cssSelectors.mapControls.panoramaPhoto.mainControlEnabled);
                    }
                }
            ]
        },
        {
            name: 'maps_www.panorama_control',
            description: 'Контрол панорамы - включенный',
            url: '?l=stv,sta',
            selector: cssSelectors.mapControls.panoramaPhoto.mainControl,
            events: [
                {
                    type: 'show',
                    state: {checked: true}
                },
                {
                    type: 'click',
                    state: {checked: true}
                },
                {
                    type: 'change_state',
                    state: {checked: false},
                    setup: async (browser) => {
                        await browser.waitAndClick(cssSelectors.mapControls.panoramaPhoto.mainControlEnabled);
                        await browser.waitForVisible(cssSelectors.mapControls.panoramaPhoto.mainControlDisabled);
                    }
                }
            ]
        },
        {
            name: 'maps_www.panorama_switcher',
            description: 'Переключатель панорама-фото',
            selector: cssSelectors.mapControls.panoramaPhoto.switcher,
            url: '?l=stv,sta',
            events: [
                {
                    type: 'show'
                },
                {
                    type: 'hide',
                    setup: async (browser) => {
                        await browser.waitAndClick(cssSelectors.mapControls.panoramaPhoto.mainControlEnabled);
                        await browser.waitForVisible(cssSelectors.mapControls.panoramaPhoto.mainControlDisabled);
                    }
                }
            ]
        },
        {
            name: 'maps_www.panorama_switcher.switcher.panorama',
            description: 'Переключатель панорам - выключенный',
            url: '?l=pht',
            selector: cssSelectors.mapControls.panoramaPhoto.panoramaSwitcher.default,
            events: [
                {
                    type: 'show',
                    state: {checked: false}
                },
                {
                    type: 'hide',
                    state: {checked: false},
                    setup: async (browser) => {
                        await browser.waitAndClick(cssSelectors.mapControls.panoramaPhoto.mainControlEnabled);
                        await browser.waitForVisible(cssSelectors.mapControls.panoramaPhoto.mainControlDisabled);
                    }
                },
                {
                    type: 'click',
                    state: {checked: false},
                    setup: async (browser) => {
                        await browser.setViewportSize({width: 1280, height: 1024});
                    }
                },
                {
                    type: 'change_state',
                    state: {checked: true},
                    setup: async (browser) => {
                        await browser.setViewportSize({width: 1280, height: 1024});
                        await browser.waitAndClick(cssSelectors.mapControls.panoramaPhoto.panoramaSwitcher.default);
                    }
                }
            ]
        },
        {
            name: 'maps_www.panorama_switcher.switcher.panorama',
            description: 'Переключатель панорам - включенный',
            url: '?l=stv,sta',
            selector: cssSelectors.mapControls.panoramaPhoto.panoramaSwitcher.default,
            events: [
                {
                    type: 'show',
                    state: {checked: true}
                },
                {
                    type: 'hide',
                    state: {checked: true},
                    setup: async (browser) => {
                        await browser.waitAndClick(cssSelectors.mapControls.panoramaPhoto.mainControlEnabled);
                        await browser.waitForVisible(cssSelectors.mapControls.panoramaPhoto.mainControlDisabled);
                    }
                },
                {
                    type: 'click',
                    state: {checked: true},
                    setup: async (browser) => {
                        await browser.setViewportSize({width: 1280, height: 1024});
                    }
                },
                {
                    type: 'change_state',
                    state: {checked: false},
                    setup: async (browser) => {
                        await browser.setViewportSize({width: 1280, height: 1024});
                        await browser.waitAndClick(cssSelectors.mapControls.panoramaPhoto.photoSwitcher);
                        await browser.waitForVisible(cssSelectors.mapControls.panoramaPhoto.photoSwitcherEnabled);
                    }
                }
            ]
        },
        {
            name: 'maps_www.panorama_switcher.switcher.photo',
            description: 'Переключатель фото - выключенный',
            url: '?l=stv,sta',
            selector: cssSelectors.mapControls.panoramaPhoto.photoSwitcher,
            events: [
                {
                    type: 'show',
                    state: {checked: false},
                    setup: async (browser) => {
                        await browser.waitForVisible(cssSelectors.mapControls.panoramaPhoto.photoSwitcherDisabled);
                    }
                },
                {
                    type: 'hide',
                    state: {checked: false},
                    setup: async (browser) => {
                        await browser.waitAndClick(cssSelectors.mapControls.panoramaPhoto.mainControlEnabled);
                        await browser.waitForVisible(cssSelectors.mapControls.panoramaPhoto.mainControlDisabled);
                    }
                },
                {
                    type: 'click',
                    state: {checked: false}
                }
            ]
        },
        {
            name: 'maps_www.panorama_switcher.switcher.photo',
            description: 'Переключатель фото - включенный',
            url: '?l=pht',
            selector: cssSelectors.mapControls.panoramaPhoto.photoSwitcher,
            setup: async (browser) => {
                await browser.waitForVisible(cssSelectors.mapControls.panoramaPhoto.photoSwitcherEnabled);
            },
            events: [
                {
                    type: 'show',
                    state: {checked: true}
                },
                {
                    type: 'hide',
                    state: {checked: true},
                    setup: async (browser) => {
                        await browser.waitAndClick(cssSelectors.mapControls.panoramaPhoto.mainControl);
                        await browser.waitForVisible(cssSelectors.mapControls.panoramaPhoto.mainControlDisabled);
                    }
                },
                {
                    type: 'click',
                    state: {checked: true}
                }
            ]
        },
        {
            name: 'maps_www.layers_panel.bookmarks',
            description: 'Контрол закладок',
            selector: getSelectorByText('Закладки', cssSelectors.map.layers.view),
            setup: async (browser) => {
                await browser.waitAndClick(cssSelectors.map.layers.control);
            },
            login: true,
            events: [
                {
                    type: 'show'
                },
                {
                    type: 'click'
                }
            ]
        },
        {
            name: 'maps_www.profile_panel.user_menu.bookmarks_control',
            description: 'Контрол закладок (аватар)',
            selector: getSelectorByText('Закладки', cssSelectors.profile.panel),
            login: true,
            setup: async (browser) => {
                await browser.waitAndClick(cssSelectors.mapControls.menu.control);
            },
            events: [
                {
                    type: 'show'
                },
                {
                    type: 'click'
                }
            ]
        },
        {
            name: 'maps_www.zoom_in',
            description: 'Призум контрол',
            selector: cssSelectors.mapControls.zoom.in,
            events: [
                {
                    type: 'show'
                },
                {
                    type: 'click'
                }
            ]
        },
        {
            name: 'maps_www.zoom_out',
            description: 'Отзум контрол',
            selector: cssSelectors.mapControls.zoom.out,
            events: [
                {
                    type: 'show'
                },
                {
                    type: 'click'
                }
            ]
        },
        {
            name: 'maps_www.layers_control',
            description: 'Контрол слоев',
            selector: cssSelectors.map.layers.control,
            events: [
                {
                    type: 'show'
                },
                {
                    type: 'click'
                }
            ]
        },
        {
            name: 'maps_www.ruler_control',
            description: 'Контрол линейки - выключенный',
            selector: cssSelectors.mapControls.ruler.control,
            events: [
                {
                    type: 'show',
                    state: {checked: false}
                },
                {
                    type: 'click',
                    state: {checked: false}
                },
                {
                    type: 'change_state',
                    state: {checked: true},
                    setup: async (browser) => {
                        await browser.waitAndClick(cssSelectors.mapControls.ruler.control);
                    }
                }
            ]
        },
        {
            name: 'maps_www.ruler_control',
            description: 'Контрол линейки - включенный',
            selector: cssSelectors.mapControls.ruler.control,
            preparation: async (browser) => {
                await browser.click(cssSelectors.mapControls.ruler.control);
            },
            events: [
                {
                    type: 'click',
                    state: {checked: true}
                },
                {
                    type: 'change_state',
                    state: {checked: false},
                    setup: async (browser) => {
                        await browser.click(cssSelectors.mapControls.ruler.control);
                    }
                }
            ]
        },
        {
            name: 'maps_www.tools_list.print',
            description: 'Контрол печати',
            selector: getSelectorByText('Печать', cssSelectors.mapControls.tools.list),
            setup: async (browser) => {
                await browser.waitAndClick(cssSelectors.mapControls.tools.control);
            },
            events: [
                {
                    type: 'show'
                },
                {
                    type: 'click'
                }
            ]
        },
        {
            name: 'maps_www.tools_list.share',
            description: 'Контрол формы шаринга',
            selector: getSelectorByText('Поделиться', cssSelectors.mapControls.tools.list),
            setup: async (browser) => {
                await browser.waitAndClick(cssSelectors.mapControls.tools.control);
            },
            events: [
                {
                    type: 'show'
                },
                {
                    type: 'click'
                }
            ]
        },
        {
            name: 'maps_www.tools_list.feedback',
            description: 'Контрол фидбека',
            selector: getSelectorByText('Сообщить об ошибке', cssSelectors.mapControls.tools.list),
            setup: async (browser) => {
                await browser.waitAndClick(cssSelectors.mapControls.tools.control);
            },
            events: [
                {
                    type: 'show'
                },
                {
                    type: 'click'
                }
            ]
        }
    ]
});
