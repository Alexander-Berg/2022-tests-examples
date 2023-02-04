import {openStudio, waitForSelector, getNodeClassList, getNodeChildWithSelector} from '../../utils/commands';

const SELECTORS = {
    sidebarUndoRedo: '.sidebar__left .undo-redo',
    backButton: '.sidebar__left .main-menu__back-button',
    sidebarBranchTitle: '.sidebar__left .main-menu__title',
    layerList: '.sidebar__left .layers',
    layersMenu: '.sidebar__left .layer-menu',
    layerIdInput: '.sidebar__right .layer-id-input',
    jsonEditButton: '.sidebar__right .sidebar__mode-control',
    settingsTabs: '.sidebar__right .tabs',
    layerSettings: '.sidebar__right .layer-set',
    layer: {
        layer: '.layer:not(._active):not(._invisible)',
        toggleVisibilityButton: '.layer .layer__toggle-visibility-button',
        invisibleState: '_invisible'
    }
};

describe('Sidebar', () => {
    beforeEach(() => openStudio());

    // https://testpalm.yandex-team.ru/testcase/gryadka-244
    test('should open sidebar', async () => {
        await Promise.all(
            [
                SELECTORS.sidebarUndoRedo,
                SELECTORS.backButton,
                SELECTORS.sidebarBranchTitle,
                SELECTORS.layerList,
                SELECTORS.layersMenu
            ].map(waitForSelector)
        );

        const layer = await waitForSelector(SELECTORS.layer.layer);
        await layer.hover();

        const toggleButton = await getNodeChildWithSelector(layer, SELECTORS.layer.toggleVisibilityButton);
        if (toggleButton === null) {
            throw new Error('No toggle button');
        }

        await toggleButton.click();
        let layerClassList = await getNodeClassList(layer);
        expect(layerClassList.includes(SELECTORS.layer.invisibleState));

        await toggleButton.click();
        layerClassList = await getNodeClassList(layer);
        expect(!layerClassList.includes(SELECTORS.layer.invisibleState));
    });

    // https://testpalm.yandex-team.ru/testcase/gryadka-245
    test('should open sidebar with settings', async () => {
        return Promise.all(
            [SELECTORS.layerIdInput, SELECTORS.jsonEditButton, SELECTORS.settingsTabs, SELECTORS.layerSettings].map(
                waitForSelector
            )
        );
    });
});
