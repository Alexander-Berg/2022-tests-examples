import cssSelectors from '../common/css-selectors';

type MobileViewState = 'panel' | 'minicard' | 'microcard';

function getSidebarNestedSelector(selector: string, mobileViewState?: MobileViewState): string {
    return mobileViewState ? cssSelectors.sidebar[mobileViewState] + ' ' + selector : selector;
}

export {getSidebarNestedSelector, MobileViewState};
