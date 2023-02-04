const elements = {
    advancedFilterButton: '.AdvancedFilterButton',
    advancedFilter: '.yb-desktop-docs-advanced-filter',
    btnSearch1: '.yb-docs-invoices-filter__primary button=[type=submit]',
    btnSearch2: '.yb-desktop-docs-advanced-filter__btn-Search',
    btnSettings: '.SearchSettingsButton',
    tmblShowTotals: '.SearchSettingsButton-Tumbler button',
    page: '.yb-user-content',
    pager: {
        page2: '.yb-user-pager__page*=2',
        page5: '.yb-user-pager__page*=5',
        dotts: '.yb-user-pager__page*=...'
    },
    preload: '.yb-preload_isLoading',
    invoices: {
        emptyList: '.EmptyList',
        table: '.yb-desktop-invoices__table',
        unusedFundsButton: '.DocsNotification_mode_unusedFunds .DocsNotification-BtnShow button'
    },
    filter: {
        paymentMethod: '.yb-desktop-docs-advanced-filter__sel-paymentMethodId',
        serviceId: '.yb-desktop-docs-advanced-filter__sel-serviceId',
        contractId: '.yb-desktop-docs-advanced-filter__sel-contractId',
        currency: '.yb-desktop-docs-advanced-filter__sel-currency'
    }
};

const hideElements = [
    '.yb-docs-invoices-primary-filter__filter-item_date-from .DatePicker',
    '.yb-docs-invoices-primary-filter__filter-item_date-to .DatePicker',
    '.yb-desktop-invoices__days-expired'
];

module.exports = {
    elements,
    hideElements
};
