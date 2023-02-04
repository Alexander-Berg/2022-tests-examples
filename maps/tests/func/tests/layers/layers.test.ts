import {
    openStudio,
    waitForSelector,
    clickToSelector,
    getSelectorCount,
    getElementText,
    waitForRemoved,
    getSelectorChildsCount,
    setValueToInput,
    acceptOnDialog,
    clickOutside,
    removeInputValue,
    getElementsText,
    getNodeChildsCountWithSelector,
    getNodeChildWithSelector
} from '../../utils/commands';

const SELECTORS = {
    sidebar: {
        container: '.sidebar',
        footerControls: {
            add: {
                open: '.sidebar .layer-menu .layer-menu__add:not(._rotate)',
                close: '.sidebar .layer-menu .layer-menu__add._rotate'
            },
            delete: '.sidebar .layer-menu .layer-menu__remove',
            duplicate: '.sidebar .layer-menu .layer-menu__duplicate',
            group: '.sidebar .layer-menu .layer-menu__group',
            ungroup: '.sidebar .layer-menu .layer-menu__ungroup',
            searchInput: '.layer-menu .layer-filter input'
        },
        styleContainer: '.sidebar .sidebar__right .layer-set',
        layers: {
            container: '.sidebar .layers .layers__container',
            firstLayer: '.sidebar .layers .layers__container > .layer',
            group: {
                collapsed: '.layer-group._collapsed',
                layer: '.layer-group > .layer-group__children .layer',
                collapser: '.layer-group__title .layer-group__collapser'
            },
            groups: '.sidebar .layers .layers__container > .layer-group',
            layers: '.sidebar .layers .layers__container > .layer'
        },
        layer: {
            notActive: '.layer:not(_active)',
            active: '.layer._active',
            title: '.layer .layer__title'
        },
        layerInfo: {
            title: {
                input: {
                    default: '.sidebar__right .sidebar__title .layer-id-input input',
                    editable: '.sidebar__right .sidebar__title .layer-id-input._editing input'
                },
                icons: {
                    edit: '.sidebar__right .sidebar__title .layer-id-input__icon._edit',
                    submit: '.sidebar__right .sidebar__title .layer-id-input__icon._submit',
                    close: '.sidebar__right .sidebar__title .layer-id-input__icon._close'
                }
            }
        },
        group: {
            closed: '.layer-group._collapsed',
            title: '.layer-group .layer-group__title'
        }
    },
    popups: {
        addLayer: {
            container: '.popup__content .add-layer-menu',
            name: '.add-layer-menu .add-layer-menu__control._kind_name input',
            source: {
                button: '.add-layer-menu .add-layer-menu__control._kind_sources button.button'
            },
            sourceLayerButton: {
                active: '.add-layer-menu__control._kind_source-layers button:not([disabled])',
                disabled: '.add-layer-menu__control._kind_source-layers button[disabled]'
            },
            layerTypeButton: {
                active: '.add-layer-menu__control._kind_type button:not([disabled])',
                disabled: '.add-layer-menu__control._kind_type button[disabled]'
            },
            popup: {
                container: '.popup__content .add-layer-menu__source-layer-menu',
                firstItem: '.popup__content .add-layer-menu__source-layer-menu .menu-item :nth-of-type(1)'
            },
            submitButton: {
                active: '.add-layer-menu > button.button:not([disabled])',
                disabled: '.add-layer-menu > button.button[disabled]'
            },
            zoomRange: {
                min: {
                    button: '.add-layer-menu__control._kind_range .select-range__select:nth-child(1) button',
                    item:
                        '.add-layer-menu__control._kind_range ' +
                        '.select-range__select:nth-child(1) .menu-item:first-child'
                },
                max: {
                    button: '.add-layer-menu__control._kind_range .select-range__select:nth-child(2) button',
                    item:
                        '.add-layer-menu__control._kind_range ' +
                        '.select-range__select:nth-child(2) .menu-item:nth-child(2)'
                }
            }
        }
    }
};

const SEARCH_STRING = 'CiTy_';

describe('Layers.', () => {
    beforeEach(async () => {
        await openStudio();
        await waitForSelector(SELECTORS.sidebar.container);
    });

    describe('Add menu.', () => {
        beforeEach(async () => {
            await clickToSelector(SELECTORS.sidebar.layers.firstLayer);
            await clickToSelector(SELECTORS.sidebar.footerControls.add.open);
            await waitForSelector(SELECTORS.popups.addLayer.container);

            await waitForSelector(SELECTORS.popups.addLayer.source.button);
            await waitForSelector(SELECTORS.popups.addLayer.sourceLayerButton.disabled);
            await waitForSelector(SELECTORS.popups.addLayer.layerTypeButton.disabled);
            await waitForSelector(SELECTORS.popups.addLayer.submitButton.disabled);
            await waitForSelector(SELECTORS.popups.addLayer.zoomRange.min.button);
            await waitForSelector(SELECTORS.popups.addLayer.zoomRange.max.button);
        });

        // https://testpalm.yandex-team.ru/testcase/gryadka-13
        test('should create layer', async () => {
            const layersCountBefore = await getSelectorChildsCount(SELECTORS.sidebar.layers.container);

            await setValueToInput(SELECTORS.popups.addLayer.name, `Layer-${Math.random()}`);

            await clickToSelector(SELECTORS.popups.addLayer.source.button);
            await clickToSelector(SELECTORS.popups.addLayer.popup.firstItem);
            await waitForRemoved(SELECTORS.popups.addLayer.popup.container);

            await clickToSelector(SELECTORS.popups.addLayer.sourceLayerButton.active);
            await clickToSelector(SELECTORS.popups.addLayer.popup.firstItem);
            await waitForRemoved(SELECTORS.popups.addLayer.popup.container);

            await clickToSelector(SELECTORS.popups.addLayer.layerTypeButton.active);
            await clickToSelector(SELECTORS.popups.addLayer.popup.firstItem);
            await waitForRemoved(SELECTORS.popups.addLayer.popup.container);

            await clickToSelector(SELECTORS.popups.addLayer.zoomRange.min.button);
            await clickToSelector(SELECTORS.popups.addLayer.zoomRange.min.item);

            await clickToSelector(SELECTORS.popups.addLayer.zoomRange.max.button);
            await clickToSelector(SELECTORS.popups.addLayer.zoomRange.max.item);

            await clickToSelector(SELECTORS.popups.addLayer.submitButton.active);

            const layersCountAfter = await getSelectorChildsCount(SELECTORS.sidebar.layers.container);

            expect(layersCountBefore).toEqual(layersCountAfter - 1);
        });

        // https://testpalm.yandex-team.ru/testcase/gryadka-12
        test('should close menu after outside click', async () => {
            await clickOutside();
            await waitForRemoved(SELECTORS.popups.addLayer.container);
        });
    });

    // https://testpalm.yandex-team.ru/testcase/gryadka-14
    test('should delete layer', async () => {
        acceptOnDialog();

        const layersCountBefore = await getSelectorChildsCount(SELECTORS.sidebar.layers.container);

        await clickToSelector(SELECTORS.sidebar.layers.firstLayer);
        await waitForSelector(SELECTORS.sidebar.styleContainer);
        await clickToSelector(SELECTORS.sidebar.footerControls.delete);

        const layersCountAfter = await getSelectorChildsCount(SELECTORS.sidebar.layers.container);

        expect(layersCountAfter).toEqual(layersCountBefore - 1);
    });

    // https://testpalm.yandex-team.ru/testcase/gryadka-16
    test('should group layer', async () => {
        await waitForSelector(SELECTORS.sidebar.layers.firstLayer);

        const layersCountBefore = await getSelectorCount(SELECTORS.sidebar.layers.layers);
        const groupsCountBefore = await getSelectorCount(SELECTORS.sidebar.layers.groups);

        await clickToSelector(SELECTORS.sidebar.layers.firstLayer);
        await waitForSelector(SELECTORS.sidebar.styleContainer);
        await clickToSelector(SELECTORS.sidebar.footerControls.group);

        const layersCountAfterGroup = await getSelectorCount(SELECTORS.sidebar.layers.layers);
        const groupCountAfterGroup = await getSelectorCount(SELECTORS.sidebar.layers.groups);

        expect(layersCountBefore).toEqual(layersCountAfterGroup + 1);
        expect(groupsCountBefore).toEqual(groupCountAfterGroup - 1);

        await clickToSelector(SELECTORS.sidebar.footerControls.ungroup);

        const layersCountAfterUngroup = await getSelectorCount(SELECTORS.sidebar.layers.layers);
        const groupCountAfterUngroup = await getSelectorCount(SELECTORS.sidebar.layers.groups);

        expect(layersCountAfterGroup).toEqual(layersCountAfterUngroup - 1);
        expect(groupCountAfterGroup).toEqual(groupCountAfterUngroup + 1);
    });

    // https://testpalm.yandex-team.ru/testcase/gryadka-15
    test('should copy layer', async () => {
        await clickToSelector(SELECTORS.sidebar.layer.notActive);
        await waitForSelector(SELECTORS.sidebar.layer.active);
        const prevLayersCount = await getSelectorChildsCount(SELECTORS.sidebar.layers.container);
        await clickToSelector(SELECTORS.sidebar.footerControls.duplicate);
        let nextLayersCount = await getSelectorChildsCount(SELECTORS.sidebar.layers.container);
        expect(nextLayersCount === prevLayersCount + 1);

        acceptOnDialog();

        await clickToSelector(SELECTORS.sidebar.footerControls.delete);
        nextLayersCount = await getSelectorChildsCount(SELECTORS.sidebar.layers.container);
        expect(nextLayersCount === prevLayersCount);
    });

    // https://testpalm.yandex-team.ru/testcase/gryadka-18
    describe('Renaming layer.', () => {
        let firstLayerName: string;
        let firstLayerNewName: string;

        beforeEach(async () => {
            firstLayerName = (await getElementText(SELECTORS.sidebar.layers.firstLayer))!;
            firstLayerNewName = `${firstLayerName}-copy`;

            await clickToSelector(SELECTORS.sidebar.layers.firstLayer);
            await waitForSelector(SELECTORS.sidebar.styleContainer);

            await waitForSelector(SELECTORS.sidebar.layerInfo.title.icons.edit);
            await clickToSelector(SELECTORS.sidebar.layerInfo.title.input.default);
            await waitForSelector(SELECTORS.sidebar.layerInfo.title.input.editable);
            await waitForSelector(SELECTORS.sidebar.layerInfo.title.icons.submit);
            await removeInputValue(SELECTORS.sidebar.layerInfo.title.input.editable);
        });

        test('should rename layer on enter', async () => {
            await setValueToInput(SELECTORS.sidebar.layerInfo.title.input.editable, firstLayerNewName);
            await page.keyboard.press('Enter');
            await waitForSelector(SELECTORS.sidebar.layerInfo.title.icons.edit);

            expect(await getElementText(SELECTORS.sidebar.layers.firstLayer)).toEqual(firstLayerNewName);
        });

        test('should rename layer on submit click', async () => {
            await setValueToInput(SELECTORS.sidebar.layerInfo.title.input.editable, firstLayerNewName);
            await clickToSelector(SELECTORS.sidebar.layerInfo.title.icons.submit);
            await waitForSelector(SELECTORS.sidebar.layerInfo.title.icons.edit);

            expect(await getElementText(SELECTORS.sidebar.layers.firstLayer)).toEqual(firstLayerNewName);
        });

        test('should not rename to existing name', async () => {
            const secondLayerName = (await getElementsText(SELECTORS.sidebar.layers.layers))[1]!;

            await setValueToInput(SELECTORS.sidebar.layerInfo.title.input.editable, secondLayerName);
            await waitForSelector(SELECTORS.sidebar.layerInfo.title.icons.close);
            await page.keyboard.press('Enter');

            expect(await getElementText(SELECTORS.sidebar.layers.firstLayer)).toEqual(firstLayerName);
        });
    });

    // https://testpalm.yandex-team.ru/testcase/gryadka-17
    describe('Collapse/expand group', () => {
        test('should expand group', async () => {
            const group = await waitForSelector(SELECTORS.sidebar.layers.group.collapsed);
            const collapser = await getNodeChildWithSelector(group, SELECTORS.sidebar.layers.group.collapser);
            await collapser.click();

            const layersCount = await getNodeChildsCountWithSelector(group, SELECTORS.sidebar.layers.group.layer);

            expect(layersCount > 0).toBeTruthy();
        });

        test('should collapse group', async () => {
            const group = await waitForSelector(SELECTORS.sidebar.layers.group.collapsed);
            const collapser = await getNodeChildWithSelector(group, SELECTORS.sidebar.layers.group.collapser);
            await collapser.click();
            await collapser.click();

            const layersCount = await getNodeChildsCountWithSelector(group, SELECTORS.sidebar.layers.group.layer);

            expect(layersCount === 0).toBeTruthy();
        });
    });

    test('Поиск по слоям', async () => {
        await setValueToInput(SELECTORS.sidebar.footerControls.searchInput, SEARCH_STRING);
        const searchStrInLowerCase = SEARCH_STRING.toLowerCase();
        let idsInLowerCase = await getLayersIdsInLowerCase();
        const closedGroups = await getSelectorCount(SELECTORS.sidebar.group.closed);
        expect(closedGroups === 0).toBeTruthy();
        expect(idsInLowerCase.every((id) => id.includes(searchStrInLowerCase))).toBeTruthy();

        await setValueToInput(SELECTORS.sidebar.footerControls.searchInput, '');
        idsInLowerCase = await getLayersIdsInLowerCase();
        expect(idsInLowerCase.every((id) => id.includes(searchStrInLowerCase))).toBeFalsy();
    });
});

async function getLayersIdsInLowerCase(): Promise<string[]> {
    const laeyrsIds = await getElementsText(SELECTORS.sidebar.layer.title);

    return laeyrsIds.map((id) => id.toLowerCase());
}
