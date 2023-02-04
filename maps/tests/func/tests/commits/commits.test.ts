import {
    clickToSelector,
    clickAndNavigate,
    waitForSelector,
    waitForRemoved,
    clickOutside,
    hoverOverSelector,
    openPage
} from '../../utils/commands';

const SELECTORS = {
    commits: {
        branchTitle: '.header .branch-content-view__title',
        backButton: '.header .header__back',
        controls: {
            container: '.edit-panel-template__header-content',
            archive: '.edit-panel-template__header-content .branch-button-view._archive'
        },
        pupup: {
            container: '.dialog .branch-panel-template-veiw',
            buttons: {
                close: '.dialog .branch-panel-template-veiw .branch-panel-template-veiw__header-close-button',
                submit: '.dialog .branch-panel-template-veiw .branch-panel-template-veiw__footer button.button'
            }
        }
    },
    index: {
        container: '.index',
        tabs: {
            container: '.tabs__header-content',
            archived: '.tabs__header-content :nth-child(5)',
            archivedActive: '.tabs__header-content :nth-child(5)._active'
        },
        notMasterBranch: {
            container: '.branch:not(._master)',
            name: '.branch .branch__name',
            actions: {
                commits: '.branch:not(._master) .branch-button-view._commits'
            }
        }
    }
};

describe('Commits page.', () => {
    beforeEach(async () => {
        await openPage();
        await hoverOverSelector(SELECTORS.index.notMasterBranch.container);
        await clickAndNavigate(SELECTORS.index.notMasterBranch.actions.commits);
    });

    // https://testpalm.yandex-team.ru/testcase/gryadka-4
    describe('Archive branch.', () => {
        describe('Popup.', () => {
            beforeEach(async () => {
                await clickToSelector(SELECTORS.commits.controls.archive);
                await waitForSelector(SELECTORS.commits.pupup.container);
            });

            test('should close popup on x click', async () => {
                await clickToSelector(SELECTORS.commits.pupup.buttons.close);
                await waitForRemoved(SELECTORS.commits.pupup.container);
            });

            test('should close popup on outside click', async () => {
                await clickOutside();
                await waitForRemoved(SELECTORS.commits.pupup.container);
            });
        });

        // TODO CARTOGRAPH-1522: [А] Исправить тест на архивацию ветки
        test.skip('should archive branch', async () => {
            await clickToSelector(SELECTORS.commits.controls.archive);
            await waitForSelector(SELECTORS.commits.pupup.container);
            await clickToSelector(SELECTORS.commits.pupup.buttons.submit);
            await waitForRemoved(SELECTORS.commits.pupup.container);
            await waitForRemoved(SELECTORS.commits.controls.archive);
        });
    });
});
