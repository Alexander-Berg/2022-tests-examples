const SELECTORS = {
    librarySelector: '.library__selector',
    imageLibraryButton: '.library__selector-item._type_image',
    addNewButton: '.library__add-new-button',
    figmaButton: '.library__figma-button',
    backButton: '.header .header__back',
    sortControl: '.library .sort-select-view',
    tagsPanel: '.library__tags-panel .tags-panel',
    headerTitle: '.header .header__title',
    tagButton: '.header .edit-panel__add-tag-button',
    list: '.library .library-items',
    itemsCounter: '.filters-panel__title',
    errorNotification: '.notification-item-view._type_error',
    infoNotification: '.notification-item-view._type_info',
    colorPicker: {
        basicDesignPreview: '.library-color-picker._design_basic .library-color-picker__preview',
        basicHexInput: '.library-color-picker._design_basic .rgb-composer__color._hex input',
        basicRGBARedInput: '.library-color-picker._design_basic .rgb-composer__color._red input',
        basicRGBAGreenInput: '.library-color-picker._design_basic .rgb-composer__color._green input',
        basicRGBABlueInput: '.library-color-picker._design_basic .rgb-composer__color._blue input',
        basicRGBAAlphaInput: '.library-color-picker._design_basic .rgb-composer__color._alpha input',
        nightDesignPreview: '.library-color-picker._design_night .library-color-picker__preview',
        nightHexInput: '.library-color-picker._design_night .rgb-composer__color._hex input',
        nightRGBARedInput: '.library-color-picker._design_night .rgb-composer__color._red input',
        nightRGBAGreenInput: '.library-color-picker._design_night .rgb-composer__color._green input',
        nightRGBABlueInput: '.library-color-picker._design_night .rgb-composer__color._blue input',
        nightRGBAAlphaInput: '.library-color-picker._design_night .rgb-composer__color._alpha input'
    },
    image: {
        container: '.library-items .image-view',
        name: '.library-items .image-view .image-view__name'
    },
    color: {
        container: '.library-items .color-view',
        unused: '.library-items .color-view._unused',
        used: '.library-items .color-view:not(._unused)',
        title: '.library-items .color-view .color-view__title'
    },
    contextMenu: {
        container: '.library-item-menu__popup',
        removeButton: '.library-item-menu__popup .library-item-menu__delete',
        duplicateButton: '.library-item-menu__popup .library-item-menu__duplicate'
    },
    searchControl: {
        input: '.filters-panel__search input.input__control',
        clearButton: '.filters-panel__search .input__clear'
    },
    fileUploader: {
        container: '.file-uploader',
        input: '.file-uploader input[type=file]'
    },
    ranameDialog: {
        container: '.image-upload-panel__rename-dialog',
        replaceButton: '.image-upload-panel__rename-dialog .image-upload-panel__header-button._replace button',
        cancelButton: '.image-upload-panel__rename-dialog .image-upload-panel__header-button._cancel button'
    },
    colorEditPanel: {
        container: '.color-edit-panel',
        saveInput: '.edit-panel-template__footer .edit-panel__input input',
        saveButton: '.edit-panel-template__footer .button',
        saveButtonDisabled: '.edit-panel-template__footer .button._disabled',
        error: '.edit-panel-template__footer .edit-panel__input .edit-panel__message._error'
    },
    imageUploadPanel: {
        container: '.image-upload-panel',
        warning: '.image-upload-panel .image-view .image-view__message',
        saveButton: '.image-upload-panel .image-upload-panel__header-button._save button',
        cancelButton: '.image-upload-panel .image-upload-panel__header-button._cancel button',
        backButton: '.image-upload-panel .header__back',
        deleteUploadableButton: '.image-upload-panel .image-view .image-view__delete'
    },
    imageEditPanel: {
        container: '.image-edit-panel',
        dayUpdateInput: '.image-edit-panel .library-image-picker._design_basic .file-uploader input[type=file]',
        nightUpdateInput: '.image-edit-panel .library-image-picker._design_night .file-uploader input[type=file]',
        saveButton: '.image-edit-panel .edit-panel-template__footer .edit-panel__field button',
        saveInput: '.image-edit-panel .edit-panel-template__footer .edit-panel__input input',
        deleteButton: '.image-edit-panel .header .edit-panel__delete'
    },
    indexPage: {
        container: '.index',
        searchInput: '.search-input-view input',
        userBranch: '.branch:not(._master)',
        branchSpinner: '.branch:not(._master) .branch__spinner',
        branchStopFigmaButton: '.branch .branch-button-view._stop-figma'
    },
    studioPage: {
        container: '.studio',
        libraryLink: '.map-menu .map-menu__library',
        sidebarLeft: '.sidebar__left',
        sidebarRight: '.sidebar__right',
        layerWithImage: '.layer[data-setting=edge_photo]',
        tabWithImage: '.sidebar__right .tabs .tabs__title:nth-child(2)',
        layerWithColor: '.layer[data-setting=edge_photo]',
        tabWithColor: '.sidebar__right .tabs .tabs__title:nth-child(2)',
        imageProperty: {
            container: '.sidebar__right .property-view .image-input',
            text: '.sidebar__right .property-view .image-input .image-input__input-text'
        },
        imageInputPopup: {
            container: '.image-input__popup-content',
            searchInput: '.image-input__popup-content .library-input-suggest__filter input',
            suggestItem: '.image-input__popup-content .library-items-select__suggest .library-items-select__image'
        },
        colorProperty: {
            container: '.sidebar__right .property-view .color-input',
            text: '.sidebar__right .property-view .color-input .color-input__input-text'
        },
        colorInputPopup: {
            container: '.color-input__popup-content',
            librarySelector: '.color-input__selector .color-input__selector-icon._library',
            searchInput: '.color-input__popup-content .library-input-suggest__filter input',
            suggestItem: '.color-input__popup-content .library-items-select__suggest .library-items-select__color-item'
        }
    }
};

export {SELECTORS};
