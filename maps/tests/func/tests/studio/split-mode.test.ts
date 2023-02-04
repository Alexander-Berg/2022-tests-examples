import {waitForSelector, hoverOverSelector, clickToSelector, openStudio} from '../../utils/commands';

const SELECTORS = {
    studio: '.studio',
    masterBranch: '.branch._master .branch__name',
    controls: {
        mapMenu: {
            container: '.map-container .map-menu',
            split: {
                container: '.map-container .map-menu .map-menu__split',
                items: {
                    oneFrame: {
                        active: '.map-menu__one-split:not(._disabled)',
                        disabled: '.map-menu__one-split._disabled'
                    },
                    vertical: {
                        active: '.map-menu__vertical-split:not(._disabled)',
                        disabled: '.map-menu__vertical-split._disabled'
                    },
                    horizontal: {
                        active: '.map-menu__horizontal-split:not(._disabled)',
                        disabled: '.map-menu__horizontal-split._disabled'
                    }
                }
            },
            zoomControls: '.map-menu .map-zoom-controls'
        },
        bugReport: '.report-button-view',
        search: '.search-input',
        commonItems: {
            toogleProdMode: '.map-statusbar-view .map-mode-view',
            layers: '.map-statusbar-view .map-hover-menu__open-menu-button .icons._layers',
            settings: '.map-statusbar-view .map-hover-menu__open-menu-button .icons._gear',
            coords: '.map-statusbar-view .map-presets'
        },
        additionalItems: {
            closeFrame: '.maps__remove-frame button.button',
            synchronizeMaps: '.maps__sync-frame'
        }
    },
    map: {
        primary: '.map-container .maps .maps__primary-map',
        additionalMaps: {
            horizontal: '.map-container .maps._split-type_horizontal .maps__additional-map',
            vertical: '.map-container .maps._split-type_vertical .maps__additional-map'
        }
    }
};

// https://testpalm.yandex-team.ru/testcase/gryadka-7
describe('Studio page', () => {
    beforeEach(async () => {
        await openStudio();
        await waitForSelector(SELECTORS.studio);
    });

    test('should show default arrange menu', async () => {
        const {mapMenu} = SELECTORS.controls;
        const splitTypes = mapMenu.split.items;

        await waitForSelector(mapMenu.container);
        await hoverOverSelector(mapMenu.split.container);
        await Promise.all(
            [splitTypes.oneFrame.disabled, splitTypes.vertical.active, splitTypes.horizontal.active].map(
                waitForSelector
            )
        );
    });

    describe.each<'horizontal' | 'vertical'>(['horizontal', 'vertical'])('Split mode.', (additionalMapType) => {
        const {controls, map} = SELECTORS;
        const {container: splitPopup, items: splitTypes} = controls.mapMenu.split;

        beforeEach(async () => {
            await hoverOverSelector(splitPopup);
            await clickToSelector(splitTypes[additionalMapType].active);
            await clickToSelector(SELECTORS.controls.additionalItems.synchronizeMaps);
        });

        afterEach(async () => {
            await hoverOverSelector(splitPopup);
            await Promise.all(
                Object.entries(splitTypes).map(([key, value]) =>
                    waitForSelector(key === additionalMapType ? value.disabled : value.active)
                )
            );
            await clickToSelector(splitTypes.oneFrame.active);
        });

        test(`check controls primary map in ${additionalMapType} mode`, () =>
            Promise.all(
                [
                    ...Object.values(controls.commonItems).map((selector) => `${map.primary} ${selector}`),
                    controls.bugReport,
                    controls.search
                ].map(waitForSelector)
            ));

        test(`check controls additional map in ${additionalMapType} mode`, () =>
            Promise.all(
                Object.values(controls.commonItems)
                    .map((selector) => `${map.additionalMaps[additionalMapType]} ${selector}`)
                    .map(waitForSelector)
            ));
    });
});
