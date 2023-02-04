import {openStudio, waitForSelector, clickToSelector, clickAndNavigate, waitForRemoved} from '../../utils/commands';

const SELECTORS = {
    index: '.index',
    save: '.save',
    sidebar: '.sidebar',
    backButton: '.sidebar .main-menu .main-menu__back-button',
    diffView: {
        container: '.unsaved-changes-view',
        saveButton: '.unsaved-changes-view__button-group > .button._view_blue-rounded',
        discardButton: '.unsaved-changes-view__button-group > .button._view_air'
    },
    properties: {
        discrete: {
            container: '.sidebar .sidebar__right .input._view_discrete',
            decrease: '.sidebar .sidebar__right .discrete-input__decrease',
            increase: '.sidebar .sidebar__right .discrete-input__increase'
        }
    },
    branch: {
        menu: {
            container: '.popup .main-menu__menu',
            clearChanges: '.popup .main-menu__menu .gui-submenu-item._clearLocalChanges'
        },
        title: '.main-menu .main-menu__title-branch',
        changes: '.main-menu .main-menu__title-changes'
    }
};

describe.skip('Unsaved changes view.', () => {
    beforeEach(() => openStudio());

    test("should't open dialog", async () => {
        await waitForSelector(SELECTORS.sidebar);
        await clickAndNavigate(SELECTORS.backButton);

        return waitForSelector(SELECTORS.index);
    });

    test('should open dialog', async () => {
        await waitForSelector(SELECTORS.sidebar);
        await clickToSelector(SELECTORS.properties.discrete.increase);
        await clickToSelector(SELECTORS.backButton);

        return waitForSelector(SELECTORS.diffView.container);
    });

    describe('Dialog actions.', () => {
        beforeEach(async () => {
            await clickToSelector(SELECTORS.properties.discrete.increase);
            await clickToSelector(SELECTORS.backButton);
        });

        test('should redirect to save page', async () => {
            await clickToSelector(SELECTORS.diffView.saveButton);

            return waitForSelector(SELECTORS.save);
        });

        test('should redirect to index page', async () => {
            await clickToSelector(SELECTORS.diffView.discardButton);

            return waitForSelector(SELECTORS.index);
        });
    });

    // https://testpalm.yandex-team.ru/testcase/gryadka-10
    test('should clear local changes', async () => {
        await clickToSelector(SELECTORS.properties.discrete.increase);
        await clickToSelector(SELECTORS.branch.title);
        await waitForSelector(SELECTORS.branch.menu.container);
        await clickAndNavigate(SELECTORS.branch.menu.clearChanges);

        await waitForSelector(SELECTORS.branch.title);
        await waitForRemoved(SELECTORS.branch.menu.clearChanges);
    });
});
