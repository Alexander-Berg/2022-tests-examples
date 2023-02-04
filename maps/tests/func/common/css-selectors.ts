const CSS_SELECTORS = {
    auth: {
        profileInfo: '.profile',
        firstAccountInList: '.AuthAccountListItem.AuthAccountListItem_default'
    },
    common: {
        logo: '.logo__image',
        dialog: {
            container: '.dialog'
        }
    },
    errors: {
        appError: '.app-error'
    },
    products: {
        list: {
            duplicateButton: "a[data-name='duplicate-product']"
        },
        create: {
            submit: "button[type='submit']"
        }
    }
} as const;

export default CSS_SELECTORS;
