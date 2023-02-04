import mainMenuStyles from '../styles.module.css';
import expandedMenuStyles from '../MainMenuExpandedMenu/styles.module.css';

export const selectors = {
    tabSelectorFactory: (n: number) => `.${mainMenuStyles.navigation} .${mainMenuStyles.menuItem}:nth-child(${n})`,
    expandedMenuItemFactory: (n: number) =>
        `.${expandedMenuStyles.expandedItems} .${expandedMenuStyles.expandedItem}:nth-child(${n})`,
};

export enum WIDTHS {
    NARROW,
    MEDIUM,
    WIDE,
}

export const viewports = {
    [WIDTHS.NARROW]: { width: 1000, height: 450 },
    [WIDTHS.MEDIUM]: { width: 1440, height: 450 },
    [WIDTHS.WIDE]: { width: 1800, height: 450 },
};

export const translations = {
    [WIDTHS.NARROW]: 'Узкий',
    [WIDTHS.MEDIUM]: 'Средний',
    [WIDTHS.WIDE]: 'Широкий',
};
