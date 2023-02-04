/* eslint-disable max-len */
const selectors = {
    mapBody: 'body',
    appReady: 'body._app_ready',
    mapReady: 'body._map_ready',
    vectorReady: 'body._vector_ready',
    urlControlled: 'body._url_controlled',
    fontsLoaded: 'body._font_loaded',
    closeButton: '.small-search-form-view__button:last-child button',
    backButton: '.small-search-form-view__icon._type_back',
    mobileSearchForm: '.mobile-small-search-form-view',
    mobileBackButton: '.mobile-small-search-form-view__icon._type_back',
    mobileUserMenu: '.mobile-small-search-form-view__menu',
    scrollbar: '.scroll__scrollbar',
    backgroundImage: '.image__bg',
    collapsedCard: '.collapsed-content-view',
    collapsedCardTitle: '.collapsed-content-view .card-title-view__title',
    expandButton: '.expand-button',
    closeButtonElement: '.close-button',
    bannerView: '.banner-view',
    qrCodeView: '.qr-code-view',
    favicon: {
        svg: "link[rel~='icon'][type='image/svg+xml']"
    },
    emergency: {
        view: '.emergency-notification-view',
        viewText: '.emergency-notification-view__text',
        viewClose: '.emergency-notification-view__close',
        viewLink: '.emergency-notification-view__link',
        stripe: '.emergency-stripe-view',
        stripeLink: '.emergency-stripe-view__link',
        stripeClose: '.emergency-stripe-view__close',
        stripeText: '.emergency-stripe-view__text'
    },
    userIcon: {
        icon: '.user-icon-view__icon',
        plus: '.user-icon-view__plus-wrapper'
    },
    stickyWrapper: '.sticky-wrapper',
    linkWrapper: '.link-wrapper',
    sidebar: {
        view: '.sidebar-view',
        expandedViews: {
            small: '.sidebar-view._view_small',
            full: '.sidebar-view._view_full'
        },
        wide: '.sidebar-view._view_wide-full',
        narrow: '.sidebar-view:not(._view_wide):not(._view_wide-full)',
        expanded: '.sidebar-view._state_expanded',
        panel: '.sidebar-view__panel',
        visiblePanel: '.sidebar-view__panel:not(._hidden)',
        card: '.sidebar-view__card',
        visibleCard: '.sidebar-view__card:not(._hidden)',
        home: '.sidebar-view._name_home',
        minicard: '.sidebar-view__card._mini:not(._hidden)',
        microcard: '.sidebar-view__card._micro',
        closeButton: '.sidebar-view__card:not(._hidden) .close-button',
        panelCloseButton: '.sidebar-view__panel:not(._hidden) .close-button',
        miniCardCloseButton: '.sidebar-view__card._mini:not(._hidden) .close-button',
        loadingIndicator: '.sidebar-panel-view__loading-indicator',
        title: '.sidebar-panel-header-view__title',
        controls: '.sidebar-view__panel-controls',
        shutterControl: '.sidebar-view__panel .expand-button'
    },
    panelOverlay: {
        view: '.panel-overlay',
        buttons: '.panel-overlay__buttons'
    },
    carousel: {
        view: '.carousel',
        item: '.carousel__item',
        firstItem: '.carousel__item:first-of-type',
        lastItem: '.carousel__item:last-of-type',
        nthItem: '.carousel__item:nth-child(%i)',
        prevButton: '.carousel__arrow-wrapper._prev .carousel__arrow',
        nextButton: '.carousel__arrow-wrapper:not(._prev) .carousel__arrow'
    },
    datepicker: {
        dayN: '.react-datepicker__day[aria-label="day-%i"]',
        selected: '.react-datepicker__day--selected',
        today: '.react-datepicker__day._today',
        disabled: '.react-datepicker__day--disabled',
        enabled: '.react-datepicker__day:not(.react-datepicker__day--disabled)'
    },
    home: {
        view: '.home-panel-content-view',
        header: '.home-panel-content-view__header',
        weather: '.home-panel-content-view__weather',
        loadingIndicator: '.sidebar-view._name_home._show-loading_header',
        closeButton: '.sidebar-view._name_home .sidebar-panel-view__close-container .close-button',
        rubrics: {
            view: '.home-panel-content-view__catalog',
            item: '.catalog-grid-view__item',
            text: '.catalog-grid-view__text',
            food: '.catalog-grid-view__item._id_food',
            more: '.catalog-grid-view__item._id_more',
            brandingIcon: 'div[class^="catalog-grid-view__item _id_branding"]'
        },
        promoWrapper: '.home-panel-content-view__promo',
        tip: '.home-panel-content-view__special-tip'
    },
    catalog: {
        main: {
            view: '.catalog-main-view',
            title: '.catalog-main-view__title',
            rubric: '.catalog-main-view .catalog-rubrics-view__item',
            catalog: '.catalog-main-view__catalog'
        },
        group: {
            view: '.catalog-group-view',
            header: '.catalog-group-view__header',
            title: '.catalog-group-view__title',
            allOrganizationsLink: '.catalog-group-view__show-all',
            filter: {
                view: '.catalog-group-view__filter',
                input: '.catalog-group-view__filter input'
            },
            empty: '.catalog-group-view__not-found',
            rubric: '.catalog-group-view__rubric',
            firstRubric: {
                view: '.catalog-group-view__rubric:first-of-type',
                title: '.catalog-group-view__rubric:first-of-type .catalog-group-view__rubric-title'
            }
        },
        header: {
            title: '.catalog-header-view__title',
            closeButton: '.catalog-header-view .close-button._color_black._circle',
            backButton: '.catalog-header-view__back-icon'
        }
    },
    masstransitEmptySchedule: {
        view: '.masstransit-empty-schedule-view',
        addSchedule: '.masstransit-empty-schedule-view__empty-control'
    },
    cardOffer: {
        view: '.card-offer-view',
        more: '.card-offer-view__more',
        content: '.card-offer-view__content-wrapper',
        link: '.card-offer-view__link'
    },
    cardDirect: {
        view: '.card-direct-view'
    },
    businessCardTitle: {
        view: '.business-card-title-view',
        advert: '.business-card-title-view__advert',
        header: '.business-card-title-view__header'
    },
    search: {
        panel: '.sidebar-view._name_search',
        loadingIndicator: '.small-search-form-view__spinner',
        input: '.search-form-view input[type="search"]',
        inputFocused: '.search-form-view .input._focused',
        mobileInput: '.search-form-view .input .input__control',
        command: '.search-command-view',
        commandCollapsed: '.search-collapsed-card-view__command',
        form: {
            view: '.search-form-view',
            input: '.search-form-view__input input',
            clearInput: '.search-form-view__input .input__clear',
            button: '.small-search-form-view__button',
            catalog: '.search-form-view__catalog',
            catalogSecondItem: '.search-form-view__catalog .catalog-grid-view__item:nth-child(2)',
            catalogFourthItem: '.search-form-view__catalog .catalog-grid-view__item:nth-child(3)',
            carouselCatalogSecondItem: '.search-form-view__catalog .carousel__item:nth-child(2)',
            carouselCatalogThirdItem: '.search-form-view__catalog .carousel__item:nth-child(3)',
            suggested: '.search-form-view._suggested',
            close: '.mobile-small-search-form-view__cancel'
        },
        searchButton: '.small-search-form-view__icon._type_search',
        serpSearchButton: '.small-search-form-view__serp-submit',
        mobileSearchButton: '.mobile-small-search-form-view__icon._type_search',
        closeButton: '.small-search-form-view__icon._type_close',
        routeButton: '.small-search-form-view__icon._type_route',
        suggest: {
            panel: '.suggest__content',
            item: '.suggest-item-view',
            itemButtom: '.suggest-item-view__button',
            subtitle: '.suggest-item-view__subtitle',
            firstItem: '.suggest-item-view:first-child',
            secondItem: '.suggest-item-view:nth-child(2)',
            clearButton: '.suggest .input .input__clear'
        },
        filters: {
            mapFilters: {
                bar: '.search-filters-map-view .search-filters-bar',
                disabledButton: '.search-filters-map-view .search-filters-bar__button .button._disabled',
                showMoreButton: '.search-filters-map-view .search-filters-bar__show-more-button',
                filterButton: '.search-filters-map-view .search-filters-bar__filter .button',
                nthFilterButton:
                    '.search-filters-map-view .search-filters-bar__filter:not(.search-filters-bar__show-more-button):nth-child(%i) .button',
                checkedFilterButton: '.search-filters-map-view .search-filters-bar__filter .button._checked',
                flippedFilterButton: '.search-filters-map-view .search-filters-bar__filter .button._flipped-tick',
                enumOption: '.menu-item'
            },
            serpFilters: {
                bar: '.sidebar-view__panel .search-filters-view .search-filters-bar',
                showMoreButton: '.sidebar-view__panel .search-filters-view .search-filters-bar__show-more-button',
                filterButton: '.sidebar-view__panel .search-filters-view .button',
                checkedFilterButton: '.sidebar-view__panel .search-filters-view .button._checked',
                enumOption: '.menu-item'
            },
            miniSerpFilters: {
                bar: '.search-collapsed-card-view__list .search-filters-bar',
                showMoreButton: '.search-collapsed-card-view__list .search-filters-bar__show-more-button',
                filterButton: '.search-collapsed-card-view__list .button',
                checkedFilterButton: '.search-collapsed-card-view__list .button._checked'
            },
            dropdown: {
                view: '.select-air .select._shown .select__dropdown',
                option: '.select-air .select._shown .select__dropdown .menu-item',
                checkedOption: '.select-air .select._shown .select__dropdown .menu-item._checked'
            },
            shutter: {
                view: '.filter-shutter',
                closeButton: '.filter-shutter__close-button',
                resetButton: '.filter-shutter__header > button',
                option: '.filter-shutter .menu-item',
                checkedOption: '.filter-shutter .menu-item._checked'
            },
            full: {
                closeButton: '.search-full-filters-view__close',
                resetButton: '.search-full-filters-view__reset',
                view: '.search-full-filters-view',
                submitButton: '.search-full-filters-view__submit',
                boolean: '.search-full-filters-view__bool-filter',
                booleanChecked: '.search-full-filters-view__bool-filter .button._checked',
                secondFilter: '.search-full-filters-view__filter:nth-child(2)',
                thirdFilter: '.search-full-filters-view__filter:nth-child(3)',
                enumCheckbox: '.search-full-filters-view__enum-item > .checkbox',
                enumCheckboxChecked: '.search-full-filters-view__enum-item > .checkbox._checked',
                secondEnumCheckbox: '.search-full-filters-view__enum-item:nth-child(4) > .checkbox',
                enumButton: '.search-full-filters-view__button-group button',
                hotelEnumButton:
                    '.search-full-filters-view__filter:nth-child(4) .search-full-filters-view__button-group-item button',
                disabledBooleanFilter: '.search-full-filters-view__bool-filter button._disabled',
                worktime: {
                    view: '.search-full-filters-view__filter._worktime',
                    desktopDialog: '.search-full-filters-view__dialog',
                    desktopClose: '.search-full-filters-view__dialog .close-button',
                    mobileShutter: '.search-full-filters-view__shutter',
                    mobileClose: '.search-full-filters-view__shutter .close-button',
                    notChecked:
                        '.search-full-filters-view__filter._worktime' +
                        ' .search-full-filters-view__bool-filter .button:not(._checked)'
                }
            },
            pictured: {
                carousel: '.sidebar-view__panel .search-pictured-filter-view__carousel',
                item: '.sidebar-view__panel .filter-pictured-carousel-item',
                selectedItem: '.sidebar-view__panel .filter-pictured-carousel-item._selected',
                nthItem: '.sidebar-view__panel .search-pictured-filter-view__carousel .carousel__item:nth-child(%i)',
                lastItem: '.sidebar-view__panel .search-pictured-filter-view__carousel .carousel__item:last-of-type',
                firstItem: '.sidebar-view__panel .carousel__item:first-of-type',
                image: '.sidebar-view__panel .filter-pictured-carousel-item__img',
                text: '.sidebar-view__panel .filter-pictured-carousel-item__text',
                link: '.search-pictured-filter-view__link'
            },
            datepicker: {
                view: '.react-datepicker',
                today: '.react-datepicker__day._today',
                day10: '.react-datepicker__day[aria-label="day-10"]',
                day15: '.react-datepicker__day[aria-label="day-15"]'
            },
            worktime: {
                desktopPopup: '.work-time-filter-view__dropdown .popup__content',
                desktopDialog: '.work-time-filter-view__dialog',
                desktopClose: '.work-time-filter-view__dialog .close-button',
                mobileShutter: '.work-time-filter-view__shutter',
                mobileClose: '.work-time-filter-view__shutter .close-button',
                option: '.work-time-filter-view__control',
                checkedOption: '.work-time-filter-view__control ._checked',
                notCheckedOption: '.work-time-filter-view__control > :not(._checked)'
            },
            price: {
                priceFromInput: '.search-price-filter__input:first-child input',
                priceToInput: '.search-price-filter__input:last-child input',
                priceFromHandler: '.search-price-filter__slider .slider__handler',
                priceToHandler: '.search-price-filter__slider .slider__handler:nth-of-type(even)'
            },
            timeCalendar: {
                view: '.time-calendar-filter-view',
                calendar: '.time-calendar-filter-view__calendar',
                calendarInput: '.time-calendar-filter-view__calendar_control',
                timeInput: '.time-calendar-filter-view__time-control .time-input input',
                nthTimeInput: '.time-calendar-filter-view__time-control:nth-child(%i) .time-input input',
                button: '.time-calendar-filter-view__button'
            },
            bookingTooltip: '.search-filters-bar__booking-tooltip'
        },
        tabs: {
            view: '.sidebar-view__panel .tabs-select-view',
            places: '.sidebar-view__panel .tabs-select-view__title._name_places',
            selected: '.tabs-select-view__title _selected',
            collections: '.sidebar-view__panel .tabs-select-view__title._name_collections',
            filtersButton: '.search-tabs-filter-button .button'
        },
        menuView: {
            view: '.card-related-products-view',
            title: '.card-related-products-view__title',
            carousel: '.card-related-products-view__top-items',
            transitionItem: '.card-related-products-view__transition',
            rightContent: '.card-related-products-view__right-content'
        },
        list: {
            view: '.sidebar-view._name_search .scroll',
            collapsed: '.search-collapsed-card-view__list',
            micro: '.micro-search-list-view',
            content: '.search-list-view__content',
            list: '.search-list-view__list',
            addBusinessView: '.search-list-view .add-business-view',
            warning: '.search-list-view__warning',
            quarantine: '.search-list-view__quarantine-warning',
            disclaimer: '.search-list-view__disclaimer',
            snippet: {
                view: '.search-snippet-view',
                viewFirst: '.search-list-view__list .search-snippet-view:first-child',
                viewLast: '.search-snippet-view:last-of-type',
                viewFirstHovered: '.search-snippet-view:first-of-type > .search-snippet-view__body._hovered',
                photo: '.search-list-view .photos-thumbnail-view',
                businessStatus: '.search-list-view .business-working-status-view',
                businessStatusHours:
                    '.search-snippet-view__body .search-business-snippet-view .business-working-status-view__text',
                session: {
                    view: '.search-snippet-movie-sessions-view',
                    button: '.search-snippet-movie-sessions-view__button',
                    more: '.search-snippet-movie-sessions-view__button:last-child',
                    iframe: 'iframe[src^="https://widget.afisha.yandex.ru/"]'
                },
                title: '.search-business-snippet-view__title',
                subtitle: '.search-business-snippet-subtitle-view',
                linkOverlay: '.search-snippet-view__link-overlay',
                business: '.search-snippet-view__body._type_business',
                galleryPhoto: '.search-snippet-gallery-view__photo',
                galleryMoreButton: '.search-snippet-gallery-view__photo._more',
                specificMenuLink: '.search-snippet-gallery-view__button a[href$="/maps/org/kimchi/1097028035/menu/"]',
                specificPriceLink:
                    '.search-snippet-gallery-view__button a[href$="/maps/org/persona_lab/169848245116/prices/"]'
            },
            businessSnippet: {
                view: '.search-snippet-view__body .search-business-snippet-view',
                title:
                    '.search-snippet-view__body .search-business-snippet-view' +
                    ' .search-business-snippet-view__title',
                category:
                    '.search-snippet-view__body .search-business-snippet-view ' +
                    '.search-business-snippet-view__categories',
                address: '.search-business-snippet-view__address',
                rating: '.search-business-snippet-view .business-rating-with-text-view',
                photoPreview: '.search-business-snippet-view .photos-thumbnail-view',
                averageBill: '.search-business-snippet-view__average-bill',
                drug: '.search-business-snippet-view__drug',
                badge: '.business-verified-badge',
                badgePopup: '.business-verified-badge__order',
                link: '.search-business-snippet-view__title .link-wrapper',
                gallery: '.search-business-snippet-view__gallery',
                relatedItems: {
                    view: '.search-business-related-items-view',
                    button: '.search-business-related-items-view__button',
                    item: '.search-business-related-items-view__item'
                },
                direct: '.search-business-snippet-view__direct'
            },
            collectionSnippet: {
                view: '.sidebar-view__panel .search-collection-snippet-view',
                image: '.sidebar-view__panel .search-collection-snippet-view__image'
            },
            directSnippet: {
                view: '.search-direct-snippet-view'
            },
            meta: {
                view: '.search-list-meta-view'
            },
            branding: '.search-list-view__branding-banner',
            gasBanner: '.card-banner-view._view_black'
        },
        page: {
            separator: '.search-list-view__separator'
        },
        placemark: {
            view: '.search-placemark-view',
            icon: '.search-placemark-icon',
            poiIcon: '.search-placemark-icon .poi-icon-provider',
            poiTitle: '.search-placemark-title._position_right',
            active: '.search-placemark-icon._mode_active',
            dot: '.search-placemark-icon._mode_dot',
            hover: '.search-placemark-icon._state_hover',
            title: '.search-placemark-title',
            lowRelevant: '.search-placemark-icon._low-relevance',
            bookmarkView: '.search-placemarks-bookmark-view'
        },
        miniCatalogFirstItem: '.catalog-grid-view__item:nth-child(1)',
        nothingFound: {
            view: '.nothing-found-view',
            link: '.nothing-found-view__link'
        },
        similarCategoriesCarousel: '.categories-carousel-view',
        businessCard: {
            view: '.business-card-view',
            overview: '.business-card-view__overview',
            title: '.business-card-view .card-title-view__title',
            category: '.business-card-view .business-card-title-view__category',
            header: '.business-card-view .card-header-media-view',
            photos: '.business-card-view .card-header-media-view__photo',
            photosTab: '.business-card-view .business-photo-list',
            nthCarouselPhoto: '.business-card-view .carousel__item:nth-child(%i) .card-header-media-view__photo',
            addPhoto: '.business-card-view__add-photo',
            route: '.business-card-view .action-button-view._type_route',
            routePessimized: '.business-card-view .action-button-view._type_route-pessimized',
            typo: '.business-card-view .search-typo-view',
            ratingSection: '.business-card-view__rating',
            ratingSummarySection: '.business-summary-rating-badge-view',
            verifiedBadge: '.business-verified-badge',
            verifiedBadgePopup: '.business-verified-badge__order',
            plusBadge: '.cta-plus-offer-badge',
            plusBadgeText: '.cta-plus-offer-badge__plus-offer-text',
            awards: {
                view: '.business-awards-view',
                award: '.business-awards-view__award',
                dialog: '.business-awards-view__dialog',
                link: '.business-awards-view__link'
            },
            headerAwards: {
                view: '.business-header-awards-view',
                award: '.business-header-awards-view__award'
            },
            goodPlaceView: '.good-place-view',
            goodPlaceViewDialog: '.good-place-view__dialog',
            goodPlaceViewShutter: '.good-place-view__shutter',
            mobileAdvertButton: {
                view: '.business-card-view .action-button-view._type_action_button',
                anchor: '.business-card-view .action-button-view._type_action_button a'
            },
            brandingBanner: '.business-card-view__branding-banner',
            panorama: '.business-card-view .panorama-thumbnail-view',
            panoramaLoaded: '.business-card-view .panorama-thumbnail-view._loaded',
            links: {
                view: '.business-urls-view',
                url: '.business-urls-view__link',
                control: '.business-urls-view .card-feature-view__arrow',
                content: '.business-urls-view .card-dropdown-view__content',
                contentUrl: '.business-urls-view .card-dropdown-view__content .business-urls-view__url'
            },
            businessLinks: {
                view: '.business-links-view',
                link: '.business-links-view__link'
            },
            // TODO: add type to selector
            cardFeatureView: '.card-feature-view',
            afishaLink: '.afisha-link-view',
            menuLink: '.business-card-view__menu-link',
            socialLink: '.business-contacts-view__social-button',
            event: {
                snippet: {
                    item: '.card-business-events-view__item',
                    list: '.card-business-events-view',
                    seeAll: '.card-business-events-view__see-all',
                    loadMore: '.card-business-events-view__more'
                },
                tab: {
                    view: '.business-events-list',
                    item: '.business-event-view__title',
                    photo: '.business-event-view__gallery-photo'
                }
            },
            afisha: {
                carousel: {
                    view: '.afisha-movies-view',
                    search: {
                        view: '.afisha-movies-view__search',
                        button: '.afisha-movies-view__loupe'
                    },
                    day: '.afisha-movies-view__day',
                    datepicker: {
                        view: '.afisha-movies-view__calendar'
                    },
                    controls: {
                        toggle: '.afisha-movies-view__day-toggle'
                    },
                    item: '.afisha-movie-item',
                    itemText: '.afisha-movie-item__text',
                    errorMessage: '.afisha-movies-view__message'
                },
                movie: {
                    view: '.afisha-poster-view',
                    back: '.afisha-poster-view__back',
                    title: '.afisha-poster-view__title',
                    schedules: {
                        view: '.afisha-poster-view__schedules',
                        title: '.afisha-poster-view__schedule-title',
                        session: '.afisha-poster-view__session',
                        fade: '.afisha-poster-view__fade',
                        notFound: '.afisha-poster-view__not-found'
                    },
                    datepicker: {
                        view: '.afisha-poster-view__calendar'
                    },
                    controls: {
                        view: '.afisha-poster-view__controls',
                        toggle: '.afisha-poster-view__day-toggle'
                    },
                    event: {
                        view: '.afisha-poster-view__movie',
                        rating: '.afisha-poster-view__rating',
                        description: '.afisha-poster-view__description',
                        poster: '.afisha-poster-view__img'
                    }
                },
                error: '.afisha-error-view',
                loading: {
                    view: '.afisha-loading-view',
                    spinner: '.afisha-loading-view__spinner'
                }
            },
            address: {
                view: '.business-contacts-view__address',
                entrances: '.business-contacts-view__link._entrances',
                buildRoute: '.business-contacts-view__link._route',
                serviceZone: '.business-contacts-view__service-zone',
                toponymLink: '.business-contacts-view__address-link'
            },
            afishaSnippet: {
                view: '.business-card-view__afisha',
                item: '.card-afisha-view__item',
                description: '.card-afisha-view__item-description',
                iframe: 'iframe[src^="https://widget.afisha.yandex.ru/"]'
            },
            encyclopedia: {
                view: '.card-encyclopedia-view',
                expandButton: '.card-encyclopedia-item__expand',
                source: '.card-encyclopedia-item__source',
                showMoreButton: '.card-encyclopedia-view__right-button',
                tabView: '.card-encyclopedia-view._view_tab',
                tabButton: '.tabs-select-view__title._name_encyclopedia'
            },
            phones: {
                view: '.card-phones-view',
                number: '.card-phones-view__phone-number',
                control: '.card-phones-view .card-feature-view__arrow',
                content: '.card-phones-view .card-dropdown-view__content',
                show: '.card-phones-view__more',
                call: '.card-phones-view__phone-number-link'
            },
            metro: {
                view: '.masstransit-stops-view._type_metro',
                text: '.masstransit-stops-view._type_metro .masstransit-stops-view__stop',
                control: '.masstransit-stops-view._type_metro .card-feature-view__arrow',
                distance: '.masstransit-stops-view._type_metro .masstransit-stops-view__stop-distance',
                content: '.masstransit-stops-view._type_metro .card-dropdown-view__content',
                contentDistance:
                    '.masstransit-stops-view._type_metro .card-dropdown-view__content ' +
                    '.masstransit-stops-view__stop-distance'
            },
            masstransit: {
                view: '.masstransit-stops-view._type_masstransit',
                control: '.masstransit-stops-view._type_masstransit .card-feature-view__arrow',
                content: '.masstransit-stops-view._type_masstransit .card-dropdown-view__content',
                distance: '.masstransit-stops-view._type_masstransit .masstransit-stops-view__stop-distance'
            },
            workingStatus: {
                view: '.business-card-working-status-view'
            },
            hours: {
                block: '.business-card-title-view__bottom',
                text: '.business-card-title-view__bottom-item .business-working-status-flip-view',
                control: '.business-card-title-view .business-working-status-flip-view__control',
                content: '.business-working-intervals-view',
                closeButton: '.close-button._color_black._circle'
            },
            moved: {
                view: '.notification-banner',
                button: '.business-moved__button',
                routeShutter: '.business-moved-route-shutter'
            },
            advert: '.business-advert-view',
            advertBadge: '.advert-badge',
            edit: '.business-card-view__edit',
            chain: '.business-card-view__chain',
            features: {
                view: '.business-features-view',
                wide: '.business-features-view._wide',
                tab: '.business-tab-wrapper._materialized .business-features-view._view_tab',
                moreInfoButton: '.business-features-view__more-info',
                featuresCutView: '.features-cut-view',
                featuresCutExpand: '.features-cut-view__expand',
                valuedList: '.business-features-view__valued-list',
                valuedItem: '.business-features-view__valued',
                a11y: {
                    warning: '.business-feature-a11y-group-view__warning',
                    description: '.business-feature-a11y-group-view__description',
                    view: '.business-feature-a11y-additional-view',
                    photoItem: '.business-feature-a11y-additional-view__photo',
                    lastPhotoDate: '.business-feature-a11y-additional-view__last-photo-date'
                }
            },
            like: '.business-user-rating-view__button._type_like',
            histogram: {
                view: '.business-attendance-view',
                default: {
                    day: '.business-attendance-view__day:first-child',
                    hour: '.business-attendance-view__bar:nth-child(16)'
                },
                day: '.business-attendance-view__day:nth-child(3)',
                hour: '.business-attendance-view__bar:nth-child(13)'
            },
            specialOffers: {
                view: '.business-card-view__offers',
                carousel: '.card-special-offers-view',
                item: '.card-special-offers-view__item',
                itemImg: '.card-special-offers-view__item-img',
                itemLink: '.card-special-offers-view__item-name._link',
                disclaimers: '.card-special-offers-view__disclaimers',
                secondElement: '.business-card-view__offers .carousel__item:nth-child(2)',
                nextButton: '.business-card-view__offers .carousel__arrow-wrapper:not(._prev) .carousel__arrow',
                prevButton: '.business-card-view__offers .carousel__arrow-wrapper._prev .carousel__arrow',
                lastItemInCarousel: '.business-card-view__offers .carousel__item:last-child',
                arrow: '.carousel__arrow'
            },
            drugs: {
                view: '.card-related-products-view',
                openIframe: '.card-feature-view._interactive',
                iframe: '.iframe-dialog',
                iframeContent: '.iframe-dialog__frame',
                closeIframe: '.close-button._color_black._circle'
            },
            similarOrgs: {
                carousel: '.card-similar-carousel',
                extra: '.card-similar-carousel__extra',
                carouselItem: '.business-similar-places .carousel__item',
                firstCarouselItem: '.business-similar-places .carousel__item:nth-child(1)',
                secondCarouselItem: '.business-similar-places .carousel__item:nth-child(2)',
                prioritizedItemBadge: '.card-similar-carousel__item .business-verified-badge._prioritized',
                tab: '.card-similar-organizations-list-view'
            },
            similarOrgsList: {
                view: '.card-similar-organizations-list-view',
                snippet: '.card-similar-organizations-list-view__snippet'
            },
            fuelInfo: {
                view: '.search-fuel-info-view',
                control: '.search-fuel-info-view .card-feature-view__arrow'
            },
            hotelsBooking: {
                iframe: '.business-iframe iframe[src^="https://travel.yandex.ru/"]'
            },
            edadeal: {
                view: '.business-iframe',
                iframe: '.business-iframe iframe[src^="https://edadeal.ru/"]'
            },
            realty: {
                snippet: '.card-realty-snippet-view',
                snippetPriceTag: '.card-realty-snippet-view__price',
                desktopIframe: '.business-iframe iframe[src^="https://realty.yandex.ru/"]',
                mobileIframe: '.business-iframe iframe[src^="https://m.realty.yandex.ru/"]'
            },
            infrastructure: {
                categories: '.infrastructure-categories-view'
            },
            summary: {
                view: '.business-card-view .business-summary',
                expand: '.business-summary__expand-button'
            },
            footer: {
                view: '.business-card-view .business-card-view__gray-section',
                advertLink: '.business-card-view__footer-link:not([data-id="become-owner"])',
                becomeOwnerLink: '.business-card-view__footer-link[data-id="become-owner"]'
            },
            sources: {
                view: '.search-sources-view',
                link: '.search-sources-view__source',
                lastLink: '.search-sources-view__source:last-child',
                toggleExpandButton: '.search-sources-view__control'
            },
            openAgain: {
                view: '.yes-no-poll',
                gratitude: '.yes-no-poll__gratitude'
            },
            dialogs: {
                content: '.business-dialog-view__content',
                mobileCloseButton: '.shutter .close-button'
            },
            transit: {
                view: '.card-transit-view',
                taxi: '.card-transit-view .card-feature-view._type_taxi'
            },
            menu: {
                tab: '.business-full-items-grouped-view',
                item: '.business-full-items-grouped-view .related-item-photo-view',
                image: '.business-full-items-grouped-view .related-item-photo-view__image',
                imageWithAlt:
                    '.business-full-items-grouped-view .related-item-photo-view__image img[alt="Балтийский, фото — Знатный дворъ (Россия, Рязань, улица Дзержинского, 65)"]',
                info: '.business-full-items-grouped-view__info',
                disclaimer: '.business-full-items-grouped-view__disclaimer'
            },
            placesInside: {
                tab: '.card-businesses-list',
                searchInput: '.card-businesses-list input',
                tabFirstResult: '.card-businesses-list .search-snippet-view',
                snippet: '.card-places-inside-view'
            },
            fullItems: {
                controls: '.business-full-items-view__controls'
            },
            ownerBadge: '.business-owner-badge',
            quarantine: '.business-card-view .notification-banner',
            dialogButton: {
                view: '.booking-action-button-dialog',
                bookingButton: '.booking-action-button-dialog__button:first-child',
                advertButton: '.booking-action-button-dialog__button:last-child',
                bookingButtonMobile: '.booking-action-button-dialog__button:nth-child(2)',
                advertButtonMobile: '.booking-action-button-dialog__button:first-child',
                cancel: '.booking-action-button-dialog__cancel'
            },
            bookingMiniWidget: {
                view: '.card-booking-widget-view',
                button: '.card-feature-view__main',
                nthService: '.card-booking-widget-view__service:nth-child(%i)',
                nearestDate: '.card-booking-widget-view__service-nearest-date',
                tab: '.card-booking-widget-view__tab',
                resource: '.card-booking-widget-view__resource'
            }
        },
        toponymCard: {
            view: '.toponym-card-view',
            suggestedResults: '.additional-results-view',
            firstSuggestedResult: '.additional-results-view__list .search-snippet-view:nth-child(1)',
            addObject: '.toponym-card-view__add-object',
            title: '.toponym-card-view .card-title-view__title',
            buildRoute: '.toponym-card-view .action-button-view._type_route',
            panorama: '.toponym-card-view .panorama-thumbnail-view',
            advert: '.toponym-card-view__advert',
            edit: '.toponym-card-view__edit',
            catalogEntry: {
                view: '.catalog-entry-point',
                icon: '.catalog-entry-point__icon'
            },
            titleView: {
                coords: '.toponym-card-title-view__coords',
                carparksFree: '.toponym-card-title-view__carparks-price'
            },
            catalog: {
                view: '.toponym-card-view .catalog-grid-view',
                item: '.toponym-card-view .catalog-grid-view__item'
            },
            nearbyFood: '.toponym-card-view .catalog-grid-view__item:nth-child(1)',
            businesses: {
                view: '.toponym-businesses-list-snippet',
                showAll: '.toponym-businesses-list-snippet__show-all'
            },
            businessesTab: {
                view: '.card-businesses-list',
                select: '.card-businesses-list .select',
                firstSnippet: '.card-businesses-list .search-snippet-view',
                firstSnippetCategory:
                    '.card-businesses-list .search-snippet-view .search-business-snippet-view__category'
            },
            building: {
                view: '.card-building-view'
            },
            serviceOrgs: {
                view: '.toponym-service-orgs',
                org: '.toponym-service-orgs .search-snippet-view',
                showAll: '.toponym-service-orgs__show-all',
                lavka: '.toponym-service-orgs .search-lavka-view'
            },
            attractions: {
                view: '.toponym-attractions',
                showAll: '.toponym-attractions__show-all',
                item: '.toponym-attractions .search-snippet-view'
            },
            typo: '.toponym-card-view .search-typo-view__query',
            zeroSuggest: '.full-screen-suggest__drop .suggest__content._full-height',
            providers: {
                view: '.card-providers-view',
                item: '.card-providers-view__item',
                smallItem: '.card-providers-view__item:not(._big)',
                bigItem: '.card-providers-view__item._big',
                expandButton: '.card-providers-view__expand',
                icon: '.card-providers-view__icon'
            },
            hotWaterShutOffBanner: {
                view: '.toponym-hot-water-shut-off-banner',
                link: '.toponym-hot-water-shut-off-banner__link'
            },
            header: {
                stickyWrapper: '.toponym-card-view .sticky-wrapper._header'
            },
            footer: {
                view: '.toponym-card-view .card-footer-view'
            }
        },
        directCard: {
            view: '.direct-card-view'
        },
        showParkings: {
            view: '.card-parking-view',
            enabled: '.card-parking-view:not(._disabled)'
        },
        statusWarning: {
            view: '.business-status-warning-view',
            action: '.business-status-warning-view__button',
            thanks: '.business-status-warning-view._thanks',
            moved: '.business-status-warning-view__moved'
        },
        homePin: '.small-search-form-view__pin',
        gallery: {
            view: '.search-snippet-gallery-view',
            photo: '.search-snippet-gallery-view._type_carousel .carousel__item'
        }
    },
    businessMapPreview: {
        view: '.business-map-preview-view',
        route: '.business-map-preview-view .action-button-view._type_route',
        showPhone: '.business-map-preview-view .action-button-view._type_show-phone',
        photo: '.business-map-preview-view .card-header-media-view__photo',
        siteLink: '.business-map-preview-view .action-button-view._type_web',
        geoproductCta: '.business-map-preview-view__button._type_cta',
        menu: '.business-map-preview-view .action-button-view._type_menu',
        prices: '.business-map-preview-view .action-button-view._type_prices',
        booking: '.business-map-preview-view .action-button-view._type_booking'
    },
    realtyPreview: {
        view: '.realty-preview-view',
        price: '.realty-preview-view__offer-price'
    },
    poi: {
        panel: '.sidebar-view._name_search-result',
        panelNarrow: '.sidebar-view._name_search-result:not(._view_wide)',
        panelWide: '.sidebar-view._name_search-result._view_wide'
    },
    header: {
        view: '.header-view'
    },
    serpHeader: {
        view: '.serp-header-layout',
        input: '.serp-header-layout .input__control',
        submitButton: '.small-search-form-view__serp-submit',
        clearButton: '.serp-header-layout .input__clear',
        logo: '.serp-header-layout__logo-link',
        suggest: {
            view: '.suggest__content._in-popup',
            firstSuggestItem: '.suggest-item-view:first-of-type',
            catalog: '.suggest__content .shortcuts-carousel',
            firstCatalogItem: '.suggest__content .shortcuts-carousel .carousel__item:first-of-type',
            lastCatalogItem: '.suggest__content .shortcuts-carousel .carousel__item:last-of-type',
            group: '.suggest__group._titled',
            groupTitle: '.suggest__group-title',
            firstGroupItem: '.suggest-item-view:nth-child(2)',
            secondGroupItem: '.suggest-item-view:nth-child(3)',
            thirdGroupItem: '.suggest-item-view:nth-child(4)'
        },
        tabBar: {
            view: '.tab-bar__container',
            firstTab: '.tab-bar__container .tab-bar__tab:first-of-type',
            lastTab: '.tab-bar__container .tab-bar__tab:last-of-type'
        }
    },
    buttons: {
        air: '.button._view_air',
        airalt: '.button._view_air-alt'
    },
    geolocationPlacemark: '.geolocation-pin__placemark',
    geolocationError: {
        dialog: '.geolocation-error-dialog',
        closeButton: '.geolocation-error-dialog .close-button'
    },
    mapControls: {
        container: '.map-controls',
        hiddenControl: '.layout-control-group._hidden',
        visibleControl: '.layout-control-group:not(._hidden)',
        controlGroup: '.layout-control-group',
        bottomExtension: ' .map-controls__bottom-extension-slot',
        logo: '.logo-view',
        geolocation: '.map-geolocation-control',
        routes: '.route-control',
        print: '.print-control',
        additionalButton: '.map-controls__additional-button',
        additionalButtonInPopup: '.popup__content .map-controls__additional-button',
        panoramaPhoto: {
            mainControl: '.map-panorama-photo-control',
            mainControlEnabled: '.map-panorama-photo-control._checked',
            mainControlDisabled: '.map-panorama-photo-control:not(._checked)',
            switcher: '.mode-switcher-view',
            panoramaSwitcher: {
                default: '.mode-switcher-view__item._key_panorama',
                disabled: '.mode-switcher-view__item._key_panorama._disabled'
            },
            photoSwitcher: '.mode-switcher-view__item._key_photo',
            photoSwitcherDisabled: '.mode-switcher-view__item._key_photo:not(._active)',
            photoSwitcherEnabled: '.mode-switcher-view__item._key_photo._active'
        },
        ruler: {
            control: '.map-ruler-control .button',
            checkedControl: '.map-ruler-control .button._checked',
            notCheckedControl: '.map-ruler-control .button:not(._checked)',
            warningView: '.ruler-warning-view',
            rulerPoint: '.ruler-view__point',
            balloon: '.ruler-balloon',
            tooltip: '.ruler-balloon__tooltip-wrapper',
            closeButton: '.ruler-balloon__close',
            trashButton: '.ruler-balloon__trash',
            popup: '.ruler-balloon__popup',
            popupConfirmation: '.ruler-balloon__confirm',
            popupCancel: '.ruler-balloon__cancel'
        },
        zoom: {
            view: '.zoom-control',
            in: '.zoom-control__zoom-in button:not(._disabled)',
            disabledIn: '.zoom-control__zoom-in button._disabled',
            out: '.zoom-control__zoom-out button:not(._disabled)',
            disabledOut: '.zoom-control__zoom-out button._disabled'
        },
        rotate: {
            view: '.map-geolocation-rotate-control'
        },
        tilt: {
            view: '.map-tilt-control',
            button: '.map-tilt-control button'
        },
        tiltRotate: {
            view: '.map-tilt-rotate-control',
            tilt: '.map-tilt-rotate-control__tilt',
            rotate: '.map-tilt-rotate-control__ring',
            tilted: '.map-tilt-rotate-control__tilt._tilted',
            tiltDisabled: '.map-tilt-rotate-control__tilt._disabled'
        },
        mrc: {
            view: '.mrc-control'
        },
        parking: {
            view: '.parking-control'
        },
        traffic: {
            mainControl: '.traffic-control',
            mainControlEnabled: '.traffic-control._checked',
            mainControlDisabled: '.traffic-control:not(._checked)'
        },
        share: {
            view: '.map-share-view',
            closeButton: '.map-share-view .close-button',
            common: {
                view: '.map-share-view__block._type_common',
                input: '.map-share-view__block._type_common .share-line-view input'
            },
            rulerWarning: '.ruler-share-warning-view',
            mapFrame: {
                block: '.map-share-view__block._type_map',
                previewButton: '.map-share-view__block._type_map .share-line-view .button',
                preview: '.map-share-view__block._type_map .share-frame-view',
                previewInfo: '.map-share-view__block._type_map .map-share-view__preview-info',
                input: '.map-share-view__block._type_map .share-line-view input',
                frame: '.map-share-view__block._type_map iframe'
            },
            orgWidget: {
                block: '.map-share-view__block._type_org',
                previewButton: '.map-share-view__block._type_org .share-line-view .button',
                preview: '.map-share-view__block._type_org .share-frame-view',
                input: '.map-share-view__block._type_org .share-line-view input',
                frame: '.map-share-view__block._type_org iframe'
            },
            panorama: {
                input: '.panorama-share-control .share-line-view input'
            },
            mapsapiBanner: '.mapsapi-banner-view'
        },
        menu: {
            control: '.user-menu-control',
            ownerTooltip: '.business-owner-tooltip'
        },
        tools: {
            control: '.map-tools-control',
            list: '.map-tools-list'
        }
    },
    profile: {
        sidebar: '.sidebar-view._name_user-profile',
        panel: '.user-menu-view',
        item: '.user-menu-item-view',
        plusBalance: '.user-plus-balance-view',
        account: {
            control: '.user-settings-view__control',
            settings: '.user-settings-list-view',
            name: '.user-account-view__name',
            avatar: '.user-account-view__avatar',
            secondary: '.user-account-view._secondary',
            actionCarousel: '.user-action-carousel-view'
        },
        clearHistoryDialog: '.clear-history-dialog',
        addAccountAction: '.add-account-action-view',
        multiAccounts: {
            view: '.multi-accounts-view',
            buttonBack: '.panel-header-view__back-icon',
            name: '.multi-accounts-view__name'
        },
        ownerBadge: '.business-owner-badge'
    },
    traffic: {
        layer: 'img[src*="l=trf&"]',
        layerWithEvents: 'img[src*="l=trfe&"]',
        panel: '.sidebar-view._name_traffic',
        trafficPanel: '.traffic-panel-view',
        trafficTime: '.traffic-road-event-content-view__time',
        content: '.traffic-panel-view__content',
        panelTitle: '.traffic-title-view',
        titleButton: '.traffic-title-view__button',
        homeWorkRoute: {
            button: '.home-work-route-view__button',
            view: '.home-work-route-view',
            text: '.home-work-route-view__button-text',
            switch: '.home-work-route-view__switch'
        },
        titleTodayButton: '.traffic-title-view__menu .menu-item:nth-child(1)',
        titleTomorrowButton: '.traffic-title-view__menu .menu-item:nth-child(2)',
        titleCalendarButton: '.traffic-title-view__menu .menu-item:nth-child(3)',
        greenTrafficIcon: '.traffic-icon._color_green',
        yellowTrafficIcon: '.traffic-icon._color_yellow',
        roadEvents: '.traffic-road-events-view__wrapper',
        roadEventItem: '.traffic-road-events-view__event',
        roadEventPanel: '.traffic-road-event-panel-view',
        roadEventBalloon: '.traffic-road-event-balloon',
        roadEventsDropDown: {
            title: '.traffic-road-events-view__dropdown-title',
            description: '.traffic-road-events-view__dropdown-description',
            content: '.traffic-road-events-view__wrapper .card-dropdown-view__content'
        },
        settings: {
            panel: '.traffic-archive-view',
            timeline: '.traffic-archive-view__timeline',
            infoLayer: '.traffic-info-layer-view',
            eventsCheckbox: {
                empty: '.traffic-info-layer-view .checkbox:not(._checked)',
                checked: '.traffic-info-layer-view .checkbox._checked'
            },
            calendar: {
                nextMonth: '.react-datepicker__navigation--next',
                nextMonthSecondWeekDay: '.react-datepicker__week:nth-child(2) .react-datepicker__day'
            }
        },
        addPlaceRouteButton: {
            active: '.add-place-route-view__button:not(._disabled)',
            firstButton: '.add-place-route-view__button:nth-child(2)',
            lastButton: '.add-place-route-view__button:last-child'
        },
        close: '.sidebar-view._name_traffic .close-button',
        forecast: {
            view: '.traffic-forecast-view',
            spinner: '.traffic-forecast-view__spinner'
        }
    },
    routes: {
        scooterButton: '.route-panel-form-view .route-travel-modes-view__mode._mode_scooter',
        comparisonButton: '.route-panel-form-view .route-travel-modes-view__mode._mode_comparison',
        autoButton: '.route-panel-form-view .route-travel-modes-view__mode._mode_auto',
        taxiButton: '.route-panel-form-view .route-travel-modes-view__mode._mode_taxi',
        bicycleButton: '.route-panel-form-view .route-travel-modes-view__mode._mode_bicycle',
        autoButtonChecked: '.route-panel-form-view .route-travel-modes-view__mode._mode_auto._checked',
        bicycleButtonChecked: '.route-panel-form-view .route-travel-modes-view__mode._mode_bicycle._checked',
        comparisonButtonChecked: '.route-panel-form-view .route-travel-modes-view__mode._mode_comparison._checked',
        scooterButtonChecked: '.route-panel-form-view .route-travel-modes-view__mode._mode_scooter._checked',
        masstransitButton: '.route-panel-form-view .route-travel-modes-view__mode._mode_masstransit',
        masstransitButtonChecked: '.route-panel-form-view .route-travel-modes-view__mode._mode_masstransit._checked',
        pedestrianButton: '.route-panel-form-view .route-travel-modes-view__mode._mode_pedestrian',
        pedestrianButtonChecked: '.route-panel-form-view .route-travel-modes-view__mode._mode_pedestrian._checked',
        title: '.sidebar-panel-header-view__title',
        spinner: '.route-list-view__spinner',
        collapsedSpinnerView: '.collapsed-route-panel-form-view__spinner',
        b2b: {
            view: '.route-form-view__beta'
        },
        travelModes: {
            view: '.route-travel-modes-view',
            comparison: '.route-travel-modes-view__mode._mode_comparison',
            auto: '.route-travel-modes-view__mode._mode_auto',
            autoChecked: '.route-travel-modes-view__mode._mode_auto._checked',
            masstransit: '.route-travel-modes-view__mode._mode_masstransit',
            pedestrian: '.route-travel-modes-view__mode._mode_pedestrian',
            pedestrianChecked: '.route-travel-modes-view__mode._mode_pedestrian._checked',
            bicycle: '.route-travel-modes-view__mode._mode_bicycle'
        },
        routePointFields: {
            view: '.route-point-fields-view'
        },
        informers: {
            view: '.route-informers-view',
            firstItem: '.route-informers-view__item:first-child',
            secondItem: '.route-informers-view__item:nth-child(2)',
            hasAutoRoad: '.route-informers-view__item:nth-child(1)',
            hasBicycleRoad: '.route-informers-view__item:nth-child(2)',
            hasStairs: '.route-informers-view__item:nth-child(3)'
        },
        fullScreenSuggest: {
            view: '.full-screen-suggest',
            cancel: '.route-field-view__full-screen-cancel',
            input: '.full-screen-suggest input',
            firstGroupItem: '.full-screen-suggest .suggest-item-view:nth-child(1)'
        },
        firstInput: {
            view: '.route-field-view._index_0',
            input: '.route-field-view._index_0 input',
            field: '.route-field-view._index_0 .input .input__control',
            dragControl: '.route-field-view._index_0 .drag-handler-view',
            suggest: '.suggest__content',
            suggestItems: '.suggest__group',
            firstSuggestItem: '.suggest-item-view:first-of-type',
            firstSuggestItemSelected: '.suggest-item-view:first-of-type._selected',
            deleteButton: '.route-field-view._index_0 .input .input__clear',
            focusedInput: '.route-field-view._index_0 .input._focused',
            showOnMap: '.route-quick-access-item:first-child',
            suggestHomeIcon: '.route-quick-access-item__icon._type_home',
            suggestWorkIcon: '.route-quick-access-item__icon._type_work',
            suggestGeolocationIcon: '.route-quick-access-item__icon._type_geolocation'
        },
        secondInput: {
            view: '.route-field-view._index_1',
            input: '.route-field-view._index_1 input',
            field: '.route-field-view._index_1 .input .input__control',
            dragControl: '.route-field-view._index_1 .drag-handler-view',
            suggest: '.suggest__content',
            firstSuggestItem: '.suggest-item-view:first-of-type',
            suggestWorkIcon: '.route-quick-access-item__icon._type_work',
            deleteButton: '.route-field-view._index_1 .input .input__clear',
            focusedInput: '.route-field-view._index_1 .input._focused',
            showOnMap: '.route-quick-access-item:first-child'
        },
        thirdInput: {
            field: '.route-field-view._index_2 .input .input__control',
            deleteButton: '.route-field-view._index_2 .input .input__clear',
            suggest: '.suggest__content',
            firstSuggestItem: '.suggest-item-view:first-of-type'
        },
        disabledInput: '.route-field-view._disabled',
        invalidInputError: '.route-field-view__route-input-error',
        searchIcon: '.route-field-view__search',
        map3: {
            geolocationOpacityRoutePoint: '.waypoint-pin-view._geolocation',
            pointFirst: '.waypoint-pin-view._first',
            pointFirstIcon: '.waypoint-pin-view._first .waypoint-pin-view__pin-icon',
            pointFirstText: '.waypoint-pin-view._first .waypoint-pin-view__text',

            pointIntermediate: '.waypoint-pin-view._intermediate',
            pointIntermediateDisabled: '.waypoint-pin-view._intermediate._disabled',
            pointIntermediateIcon: '.waypoint-pin-view._intermediate .waypoint-pin-view__content',
            pointIntermediateText: '.waypoint-pin-view._intermediate .waypoint-pin-view__text',

            pointLast: '.waypoint-pin-view._last',
            pointLastIcon: '.waypoint-pin-view._last .waypoint-pin-view__pin-icon',
            pointLastText: '.waypoint-pin-view._last .waypoint-pin-view__text',

            pointMobileChoose: '.waypoint-pin-view._mobile-choose .waypoint-pin-view__pin-icon',

            rubricIcon: '.waypoint-pin-view__rubric-icon',
            rubricHomeIcon: '.waypoint-pin-view__rubric-icon._type_home',
            rubricWorkIcon: '.waypoint-pin-view__rubric-icon._type_work',
            rubricGeolocationIcon: '.waypoint-pin-view._geolocation',

            route: '.ymaps3x0--layer.ymaps3x0--graphics-layer svg g[stroke-linejoin="round"]'
        },
        sidebarPanel: '.sidebar-view._name_routes .sidebar-view__panel',
        panelForm: {
            lastIntermediatePoint: '.route-waypoint-icon-view._index_48:not(._disabled)',
            geolocationIcon: '.route-waypoint-icon-view._geolocation',
            error: '.route-panel-form-view__error',
            view: '.route-panel-form-view',
            categories: '.route-panel-form-view__categories',
            snippet: '.route-snippet-view',
            collapsedSnippet: '.route-snippet-view._collapsed',
            pedestrianCollapsedSnippet: '.collapsed-route-panel-form-view .pedestrian-route-snippet-view',
            autoCollapsedSnippet: '.collapsed-route-panel-form-view .auto-route-snippet-view',
            masstransitCollapsedSnippet: '.collapsed-route-panel-form-view .masstransit-route-snippet-view',
            comparisonMasstransitCollapsedSnippet:
                '.collapsed-route-panel-form-view ' +
                '.route-snippet-view._comparison._type_masstransit .comparison-route-snippet-view',
            bicycleCollapsedSnippet: '.collapsed-route-panel-form-view .bicycle-route-snippet-view',
            taxiCollapsedSnippet: '.collapsed-route-panel-form-view .taxi-route-snippet-view',
            firstSnippet: '.route-snippet-view:first-child',
            activeSnippet: '.route-snippet-view._active',
            collapsedView: '.collapsed-route-panel-form-view',
            small: {
                view: '.route-small-form-view',
                button: '.route-small-form-view__button',
                close: '.route-small-form-view .close-button',
                title: '.route-small-form-view__title'
            },
            distribution: {
                view: '.collapsed-route-panel-form-view__distribution',
                paranja: '.collapsed-route-panel-form-view__paranja',
                button: '.collapsed-route-panel-form-view__entry-distribution .button'
            },
            businessMoved: '.route-form-view__business-moved'
        },
        tip: {
            view: '.route-tip-view',
            image: '.route-tip-view__image',
            closeButton: '.route-tip-view__close-button'
        },
        form: {
            view: '.route-form-view',
            addPoint: '.route-form-view__add',
            limit: '.route-form-view__limit',
            resetPoints: '.route-form-view__reset',
            optimizeControl: '.route-form-view__optimize',
            reverseControl: '.route-form-view__reverse-icon'
        },
        pointsHistory: {
            view: '.route-points-history-view',
            distance: '.route-points-history-view__distance',
            item: '.route-points-history-view__item',
            more: '.route-points-history-view__more'
        },
        settings: {
            view: '.route-list-view__settings',
            masstransitView: '.masstransit-route-settings-view',
            countChangedSettings: '.route-list-view__settings-count',
            masstransitSettingsItemFirst: '.masstransit-route-settings-view__item:first-child',
            masstransitSettingsItemSecond: '.masstransit-route-settings-view__item:nth-child(2)',
            masstransitSettingsItemChecked: '.masstransit-route-settings-view__item ._checked',
            autoRouteSettings: '.auto-route-settings-view',
            autoRouteSettingsItemChecked: '.auto-route-settings-view__item ._checked',
            avoidTollsSettingsItemChecked: '.auto-route-settings-view__item:nth-child(2) ._checked',
            avoidUnpavedAndPoorConditionsSettingsItemChecked: '.auto-route-settings-view__item:nth-child(3) ._checked',
            avoidTolls: '.auto-route-settings-view__item:nth-child(2)',
            avoidUnpavedAndPoorConditions: '.auto-route-settings-view__item:nth-child(3)',
            ignoreTrafficJams: '.auto-route-settings-view__item:first-child',
            disabledIgnoreTrafficJams: '.auto-route-settings-view__item:first-child .checkbox._disabled',
            timeDependent: {
                view: '.time-dependent-view',
                button: '.time-dependent-view__button',
                reset: '.time-dependent-view__reset-to-now',
                menuItemFirst: '.time-dependent-view__menu .menu-item:first-child',
                inputTime: '.time-input input',
                inputTimeArrowRight: '.time-input__time-arrow:not(._left)',
                inputTimeArrowLeft: '.time-input__time-arrow._left',
                controlDate: '.calendar-input__input',
                calendarArrowRight: '.calendar-input__calendar-arrow:not(._left)',
                calendarArrowLeft: '.calendar-input__calendar-arrow._left',
                calendarText: '.calendar-input__text',
                calendar: '.calendar-input__calendar',
                lastDay:
                    '.calendar-input__calendar .react-datepicker__week:last-child' +
                    ' .react-datepicker__day:last-child'
            },
            cargo: {
                view: '.cargo-route-settings-view',
                items: '.cargo-route-settings-view__items',
                firstInput: '.cargo-route-settings-view__item:first-child .input .input__control',
                secondInput: '.cargo-route-settings-view__item:nth-child(2) .input .input__control',
                warning: {
                    view: '.route-list-view .route-snippet-view:first-child .route-warning-cargo-view',
                    items: '.route-snippet-view:first-child .route-warning-cargo-view__items'
                }
            }
        },
        warning: {
            view: '.route-list-view__warning',
            button: '.route-list-view__warning .notification-banner span'
        },
        routeList: {
            view: '.route-list-view',
            comparisonView: '.route-list-view._travel-mode_comparison',
            masstransitView: '.route-list-view._travel-mode_masstransit',
            bicycleView: '.route-list-view._travel-mode_bicycle',
            scooterView: '.route-list-view._travel-mode_scooter',
            autoView: '.route-list-view._travel-mode_auto',
            incut: '.route-list-view__incut',
            timeDependentIncut: '.route-list-view__time-dependent-incut',
            detailedSnippet: '.route-snippet-view._detailed',
            activeSnippet: '.route-snippet-view._active',
            secondSnippet: '.route-snippet-view:nth-child(2)',
            secondActiveSnippet: '.route-snippet-view:nth-child(2)._active',
            moreRoutesButton: '.route-list-view__more-routes-button',
            mobileShare: '.route-list-view__mobile-share',
            warning: {
                view: '.route-warning-view',
                tolls: '.route-warning-view._type_tolls',
                ferries: '.route-warning-view._type_ferries',
                barrier: '.route-warning-view._type_barrier',
                blocked: '.route-warning-view._type_blocked',
                ford: '.route-warning-view._type_ford',
                ruggedRoad: '.route-warning-view._type_rugged-road',
                frontier: '.route-warning-view._type_frontier'
            },
            popup: '.popup__content',
            pedestrianSnippet: '.route-list-view .pedestrian-route-snippet-view',
            bicycleSnippet: '.route-list-view .bicycle-route-snippet-view',
            autoSnippet: {
                view: '.auto-route-snippet-view',
                step: '.route-auto-step-view',
                detailedButton: '.auto-route-snippet-view__show-details',
                title: '.auto-route-snippet-view__route-title-primary',
                arrival: '.auto-route-snippet-view__arrival',
                excludingTraffic: '.auto-route-snippet-view__excluding-traffic'
            },
            masstransitRouteDuration: '.route-snippet-view._active .masstransit-route-snippet-view__route-duration',
            masstransitRouteHint: '.route-snippet-view._active .masstransit-route-snippet-view__route-hint',
            masstransitDetailedButton: '.masstransit-route-snippet-view__show-details',
            recommendedTransportFirst:
                '.route-snippet-view._active .masstransit-route-snippet-view__name._type_bus div',
            masstransitSnippet: {
                view: '.masstransit-route-snippet-view',
                waypoint: '.masstransit-route-snippet-view__waypoint',
                active: '.masstransit-route-snippet-view._active',
                detailsView: '.masstransit-route-snippet-view__show-details',
                pedestrian: '.pedestrian-route-snippet-view',
                water: '.masstransit-route-snippet-view__name._type_water',
                cable: '.masstransit-route-snippet-view__name._type_cable',
                funicular: '.masstransit-route-snippet-view__name._type_funicular',
                stepIcon: '.route-masstransit-step-view__icon._stop',
                undergroundIcon: '.masstransit-route-snippet-view__icon._type_underground',
                alert: '.masstransit-route-snippet-view__alert',
                aeroexpress: '.masstransit-route-snippet-view__name._type_aeroexpress'
            },
            comparison: {
                view: '.comparison-route-snippet-view',
                autoSnippet: '.route-snippet-view._comparison._type_auto',
                pedestrianSnippet: '.route-snippet-view._comparison._type_pedestrian',
                masstransitSnippet: '.route-snippet-view._comparison._type_masstransit',
                bicycleSnippet: '.route-snippet-view._comparison._type_bicycle',
                scooterSnippet: '.route-snippet-view._comparison._type_scooter',
                taxiSnippet: '.route-snippet-view._comparison._type_taxi',

                activeAutoSnippet: '.route-snippet-view._comparison._type_auto._active',
                activePedestrianSnippet: '.route-snippet-view._comparison._type_pedestrian._active',
                activeMasstransitSnippet: '.route-snippet-view._comparison._type_masstransit._active',
                activeBicycleSnippet: '.route-snippet-view._comparison._type_bicycle._active',
                activeTaxiSnippet: '.route-snippet-view._comparison._type_taxi._active',

                openTaxiLink: '.comparison-route-snippet-view__open-taxi-link',

                detailedButton: '.comparison-route-snippet-view__show-details',
                subTitle: '.comparison-route-snippet-view__route-subtitle'
            },
            b2bInfo: '.route-list-view__b2b-info'
        },
        detailedRoute: {
            view: '.route-panel-detailed-view',
            viewEdit: '.route-detailed-view__edit',
            routeThread: '.route-detailed-view__steps-layout:before',
            panel: '.route-panel-detailed-view__body',
            edit: '.route-detailed-view__edit',
            waypointFirst: '.route-waypoint-step-view._index_0',
            waypointThird: '.route-waypoint-step-view._index_2',
            waypointTitle: '.route-waypoint-step-view__title',
            waypointTime: '.route-waypoint-step-view__time-date-text',
            waypointTimeText: '.route-waypoint-step-view__time-text',
            waypointDateText: '.route-waypoint-step-view__date-text',
            waypointWaitingDuration: '.route-waypoint-step-view__waiting-duration',
            stopList: '.route-masstransit-step-view__stops._visible',
            transportBadge: '.route-masstransit-step-view__transport-number._clickable',
            transferText: '.route-masstransit-step-view__transfer-text',
            metroExit: '.route-masstransit-step-view__metro-exit',
            metroExitNumber: '.route-masstransit-step-view__metro-exit-number',
            metroExitText: '.route-masstransit-step-view__metro-exit-text',
            recommendedSuburban: '.route-masstransit-step-view__transport-priority._type_suburban',
            recommendedTrain: '.route-masstransit-step-view__transport-priority._type_train',
            recommendedAero: '.route-masstransit-step-view__transport-priority._type_aero',
            recommendedTransportNumber:
                '.route-masstransit-step-view__transport-priority' +
                ' .route-masstransit-step-view__transport-number._clickable',
            recommendedTransportTitle:
                '.route-masstransit-step-view__transport-priority .route-masstransit-step-view__transport-title',
            showTimetable: '.route-masstransit-step-view__link',
            departureTime: '.route-masstransit-step-view__timetable-departure',
            arrivalTime: '.route-masstransit-step-view__timetable-arrival',
            additionaltransports: '.route-masstransit-step-view__transports-other',
            essentialStops: '.route-masstransit-step-view__transport-essential-stops',
            boardingPositions: '.route-step-boarding-positions__boarding',
            moreTrips: '.route-masstransit-step-view__more-trips'
        },
        masstransitDetailedView: {
            busStep: '.route-masstransit-step-view._transport._type_bus',
            waterStep: '.route-masstransit-step-view._transport._type_water',
            funicularStep: '.route-masstransit-step-view._transport._type_funicular',
            metroStep: '.route-masstransit-step-view._transport._type_underground',
            trainStep: '.route-masstransit-step-view._transport._type_train',
            cableStep: '.route-masstransit-step-view._transport._type_cable',
            aeroStep: '.route-masstransit-step-view._transport._type_aero',
            aeroexpressStep: '.route-masstransit-step-view._transport._type_aeroexpress'
        },
        taxi: {
            view: '.route-panel-form-view .taxi-route-snippet-view',
            button: '.route-panel-form-view .taxi-route-snippet-view__button a',
            price: '.route-panel-form-view .taxi-route-snippet-view__price',
            duration: '.route-panel-form-view .taxi-route-snippet-view__duration'
        },
        editPedestrian: '.pedestrian-route-snippet-view__feedback',
        editBicycle: '.bicycle-route-snippet-view__feedback',
        errorMessage: '.route-error-view',
        mobileErrorMessage: '.route-panel-form-view .route-error-view',
        viaPoint: '.map-routes-auto-view__via-point',
        homeWork: {
            view: '.route-home-work-view'
        },
        pins: {
            specialRoad: {
                type: {
                    ruggedRoad: '.map-placemark .route-pins-view__special-road._type_RUGGED_ROAD',
                    tollRoad: '.map-placemark .route-pins-view__special-road._type_TOLL_ROAD',
                    fordCrossing: '.map-placemark .route-pins-view__special-road._type_FORD_CROSSING',
                    speedBump: '.map-placemark .route-pins-view__special-road._type_SPEED_BUMP',
                    railwayCrossing: '.map-placemark .route-pins-view__special-road._type_RAILWAY_CROSSING'
                },
                position: {
                    begin: '._position_begin',
                    end: '._position_end'
                }
            }
        }
    },
    whatshere: {
        panel: '.sidebar-view._name_search-result',
        preview: {
            view: '.whats-here-preview',
            title: '.whats-here-preview__title',
            routeButton: '.whats-here-preview__control-route',
            searchButton: '.whats-here-preview__control-search',
            panorama: '.whats-here-preview__panorama'
        }
    },
    feedback: {
        main: {
            dialog: {
                dialog: '.feedback-dialog',
                title: '.feedback-dialog__title',
                closeButton: '.close-button._color_black._circle',
                notification: '.feedback-dialog__notification',
                list: {
                    view: '.feedback-dialog .feedback-selector-view'
                }
            }
        },
        sidebar: {
            view: '.sidebar-view._name_feedback',
            panel: '.feedback-panel-content-view',
            label: '.feedback-panel-content-view .sidebar-panel-header-view__title',
            nmapsLink: '.card-tip-view__button > a',
            footer: '.feedback-footer-container-view'
        },
        form: {
            form: '.feedback-form',
            comment: '.feedback-textarea-field._name_comment .textarea__control',
            name: '.feedback-input-field._name_username .input .input__control',
            email: '.feedback-input-field._name_email .input .input__control',
            fileInput: '.feedback-field-view input[type="file"]',
            confidentialCheckbox: {
                view: '.feedback-comment-form__checkbox',
                checkbox: {
                    view: '.feedback-comment-form__checkbox .checkbox__box',
                    checked: '.feedback-comment-form__checkbox .checkbox._checked',
                    notChecked: '.feedback-comment-form__checkbox .checkbox:not(._checked)'
                }
            },
            attachedPhotos: {
                attachedPhotos: '.add-photos-view',
                photo: '.add-photos-view__photo',
                errorMessage: '.add-photos-view__error-message',
                removePhoto: '.add-photos-view__remove'
            },
            buttons: {
                cancel: '.feedback-form__cancel .button',
                submit: {
                    disabled: '.feedback-form__submit .button._disabled',
                    enabled: '.feedback-form__submit .button:not(._disabled)'
                },
                parking: {
                    disabled: '.feedback-add-toponym-view__accept .button._disabled',
                    enabled: '.feedback-add-toponym-view__accept .button:not(._disabled)'
                }
            },
            message: '.feedback-message-view',
            messageButton: '.feedback-message-view__button .button'
        },
        map: {
            placemark: {
                view: '.ugc-placemark-content-view',
                barrier: '.ugc-placemark-content-view._kind_barrier',
                gate: '.ugc-placemark-content-view._kind_gate',
                building: '.ugc-placemark-content-view._kind_building',
                location: '.ugc-placemark-content-view._kind_location',
                entrance: '.ugc-placemark-content-view._kind_entrance',
                entry: '.ugc-placemark-content-view._kind_entry',
                deleteButton: '.feedback-edit-location-view__close',
                transparent: '.ugc-placemark-content-view__wrapper._transparent',
                notTransparent: '.ugc-placemark-content-view__wrapper:not(._transparent)'
            },
            maneuverPoints: '.feedback-maneuver-map-view__icon'
        },
        photo: {
            button: '.photos-control-button._type_feedback',
            popup: '.feedback-selector-view',
            elements: {
                badQuality: '.feedback-selector-view ._name_organization-bad-quality',
                irrelevant: '.feedback-selector-view ._name_organization-irrelevant',
                complain: '.feedback-selector-view ._name_organization-support-complain'
            }
        },
        menu: {
            view: '.feedback-menu-view',
            item: '.feedback-menu-view__item'
        },
        header: {
            view: '.feedback-header-view',
            title: '.feedback-header-view__title',
            subtitle: '.feedback-header-view__subtitle',
            helper: '.feedback-header-view__helper',
            close: '.feedback-header-view__close',
            back: '.feedback-header-view__back'
        },
        finish: {
            view: '.feedback-finish-view',
            message: '.feedback-finish-view__message',
            button: '.feedback-finish-view__button .button',
            ugcOrganizationStatusFeed: '.feedback-finish-view__edit-status-carousel',
            link: '.feedback-finish-view__link'
        }
    },
    flyover: {
        disclaimer: '.map-flyover-disclaimer',
        flyoverControl: '.map-flyover-control',
        card: {
            view: '.business-card-view',
            tabContent: '.business-card-view__infrastructure',
            info: {
                view: '.infrastructure-info-view'
            },
            categories: {
                view: '.infrastructure-categories-view',
                item: '.infrastructure-categories-view__item',
                selectedItem: '.infrastructure-categories-view__item._selected'
            }
        }
    },
    addObjectFeedback: {
        view: '.feedback-add-object-view'
    },
    selectObjectFeedback: {
        view: '.feedback-select-object-view'
    },
    addToponymFeedback: {
        view: '.feedback-add-toponym-view',
        items: {
            road: '.feedback-field-view._name_road input',
            house: '.feedback-field-view._name_house input',
            comment: '.feedback-field-view._name_comment [placeholder]',
            email: '.feedback-field-view._name_email input',
            name: '.feedback-field-view._name_name input',
            photo: '.feedback-field-view._name_photos',
            site: '.feedback-field-view._name_site input',
            provider: '.feedback-field-view._name_provider input'
        },
        buttons: {
            submit: {
                view: '.feedback-footer-container-view button[type="submit"]',
                active: '.feedback-footer-container-view button[type="submit"]:not(._disabled)',
                disabled: '.feedback-footer-container-view button._disabled[type="submit"]'
            },
            secondary: {
                active: '.feedback-footer-container-view button[type="button"]:not(._disabled)',
                disabled: '.feedback-footer-container-view button._disabled[type="button"]'
            },
            parkingAccept: '.feedback-add-toponym-view__accept button[type="button"]:not(._disabled)'
        }
    },
    editObjectFeedback: {
        view: '.feedback-edit-object-view',
        views: {
            base: '.feedback-edit-toponym-view'
        },
        items: {
            comment: {
                optional: '.feedback-field-view._name_comment:not(._required) textarea',
                required: '.feedback-field-view._name_comment._required textarea'
            },
            email: '.feedback-field-view._name_email input',
            name: '.feedback-field-view._name_name input',
            photo: '.feedback-field-view._name_photos',
            street: '.feedback-field-view._name_street input',
            house: '.feedback-field-view._name_house input'
        },
        buttons: {
            submit: {
                active: '.feedback-footer-container-view button[type="submit"]:not(._disabled)',
                disabled: '.feedback-footer-container-view button._disabled[type="submit"]'
            },
            secondary: {
                active: '.feedback-footer-container-view button[type="button"]:not(._disabled)',
                disabled: '.feedback-footer-container-view button._disabled[type="button"]'
            },
            mapButton: {
                entrance: '.feedback-edit-entrances-view__accept',
                enabled: '.feedback-edit-entrances-view__accept .button:not(._disabled)'
            }
        }
    },
    organizationFeedback: {
        menu: '.feedback-business-menu-view',
        card: '.feedback-business-view',
        header: '.feedback-business-view__header',
        menuHeader: '.feedback-business-menu-view__header',
        menuTitle: '.feedback-business-menu-view__title',
        menuSubtitle: '.feedback-business-menu-view__subtitle',
        closeButton: '.close-button._color_black._circle',
        cancelButton: '.feedback-business-view__cancel button',
        name: '.feedback-card-name-view .input .input__control',
        nameSuggest: '.feedback-card-name-view .suggest__content',
        pointSelection: '.feedback-point-select-button-view__button',
        addressSuggest: '.suggest__drop',
        phoneNumber: '.feedback-business-card-phones-view .input .input__control',
        phoneNumberNotEmpty: '.feedback-business-card-phones-view .input:not(._empty) .input__control',
        phoneNumberAdd: '.feedback-business-card-phones-view [type="button"]',
        workingTime: {
            view: '.feedback-business-card-working-time-view',
            break: {
                view: '.feedback-business-card-break-view',
                firstFrom: '.feedback-business-card-break-view__break-container:first-child .input._stick_left input',
                firstTo: '.feedback-business-card-break-view__break-container:first-child .input._stick_right input'
            },
            hours: {
                view: {
                    active: '.feedback-business-card-hour-view [type="button"]:not(._disabled)',
                    disabled: '.feedback-business-card-hour-view [type="button"]._disabled'
                },
                content: {
                    view: '.feedback-business-card-hour-view__content',
                    aroundTheClock: '.feedback-business-card-hour-view__around-the-clock'
                }
            },
            days: {
                select: '.feedback-business-card-days-view__days .select button',
                dropdown: '.feedback-business-card-days-view__days .select__dropdown',
                buttons: {
                    addNewDay: '.feedback-business-card-working-time-view__add-new-day',
                    removeDays: '.feedback-business-card-days-view__remove-icon'
                },
                items: {
                    selectAll:
                        '.feedback-business-card-days-view__days .select._shown .select__dropdown .menu__group .menu__select-all-item',
                    defaultItem:
                        '.feedback-business-card-days-view__days .select._shown .select__dropdown .menu__group .menu-item:not(._disabled):not(:first-child)',
                    checkedItem:
                        '.feedback-business-card-days-view__days .select._shown .select__dropdown .menu__group .menu-item._checked'
                }
            }
        },
        comment: '.textarea__control',
        spravLink: '.feedback-business-view__info-link',
        sendButton: {
            view: '.feedback-footer-container-view button[type="submit"]',
            active: '.feedback-footer-container-view button[type="submit"]:not(._disabled)',
            disabled: '.feedback-footer-container-view button[type="submit"]._disabled'
        },
        address: {
            view: '.feedback-address-view .input__control',
            suggestFirstSnippet: 'li.feedback-suggest-drop-item:nth-child(1)',
            confirm: '.feedback-address-view__confirm',
            buttons: {
                save: '.feedback-address-view__save .button',
                clear: '.feedback-address-view .input__clear',
                accept: '.feedback-address-view__accept .button:not(._disabled)'
            }
        },
        errors: {
            namePopup: '.feedback-card-name-view__popup-content',
            categoryPopup: '.feedback-card-categories-view__popup-content',
            pointSelectionPopup: '.feedback-address-view__popup-content',
            daysPopup: '.feedback-business-card-days-view__popup-content'
        },
        tags: {
            view: '.tag-collection-view',
            first: '.tag-collection-view__text'
        },
        duplicates: {
            suggest: '.feedback-business-card-duplicates-view__suggest',
            firstElement: '.feedback-suggest-drop-item:first-child',
            checkbox: '.feedback-business-card-duplicates-view__checkboxes'
        },
        locationEdit: {
            view: '.feedback-business-location-view',
            helper: '.feedback-business-location-view .feedback-header-view__helper',
            entrancePlacemark: 'ymaps+ymaps+ymaps+ymaps+ymaps+ymaps+ymaps .ugc-placemark-content-view__wrapper',
            placemarkCloseButton: '.feedback-edit-location-view__close',
            buttons: {
                mapButton: '.feedback-business-location-view__accept button[type="button"]:not(._disabled)',
                submit: {
                    active: '.feedback-footer-container-view button[type="submit"]:not(._disabled)',
                    disabled: '.feedback-footer-container-view button._disabled[type="submit"]'
                },
                secondary: {
                    active: '.feedback-footer-container-view button[type="button"]:not(._disabled)',
                    disabled: '.feedback-footer-container-view button._disabled[type="button"]'
                }
            }
        },
        contacts: {
            view: '.feedback-business-view__contacts',
            phones: {
                view: '.card-dropdown-view__content .feedback-field-view._name_phone',
                inputs: {
                    new: {
                        input: '.feedback-field-view._name_phone .input-collection-view__new input',
                        addButton: '.feedback-field-view._name_phone .input-collection-view__new button',
                        clearButton: '.feedback-field-view._name_phone .input-collection-view__new .input__clear'
                    },
                    default: {
                        input:
                            '.feedback-field-view._name_phone .input-collection-view > .input-collection-view__input input',
                        clearButton:
                            '.feedback-field-view._name_phone .input-collection-view > .input-collection-view__input .input__clear'
                    }
                }
            },
            urls: {
                view: '.card-dropdown-view__content .feedback-field-view._name_urls',
                inputs: {
                    new: {
                        input: '.feedback-field-view._name_urls .input-collection-view__new input',
                        addButton: '.feedback-field-view._name_urls .input-collection-view__new button',
                        clearButton: '.feedback-field-view._name_urls .input-collection-view__new .input__clear'
                    },
                    default: {
                        input:
                            '.feedback-field-view._name_urls .input-collection-view > .input-collection-view__input input',
                        clearButton:
                            '.feedback-field-view._name_urls .input-collection-view > .input-collection-view__input .input__clear'
                    }
                }
            }
        },
        urls: {
            view: '.card-dropdown-view__content .feedback-field-view._name_urls'
        },
        categories: '.feedback-card-categories-view',
        category: {
            view: '.feedback-field-view._name_categories',
            input: '.feedback-card-categories-view__suggest .input .input__control',
            suggestFirstSnippet: 'li.feedback-suggest-drop-item:nth-child(1)',
            tags: {
                view: '.tag-collection-view',
                item: {
                    name: '.tag-collection-view__tag',
                    delete: '.tag-collection-view__close'
                }
            }
        },
        features: {
            view: '.feedback-card-features-view',
            dropdown: '.feedback-card-features-view .card-dropdown-view__content',
            name: '.feedback-card-features-view__feature .feedback-card-features-view__feature-name',
            items: {
                view: '.feedback-card-features-view__feature',
                boolean: {
                    button: '.feedback-card-features-view__feature .select button',
                    dropdown: {
                        view: '.feedback-card-features-view__feature .select._shown .select__dropdown',
                        item: '.feedback-card-features-view__feature .select._shown .select__dropdown .menu-item'
                    }
                },
                multiselect: {
                    view: '.feedback-card-features-view__enum-feature-group',
                    dropdown: {
                        view: '.feedback-card-features-view__feature .card-dropdown-view__content',
                        item: {
                            checked:
                                '.feedback-card-features-view__feature .card-dropdown-view__content .feedback-card-features-view__enum-feature .checkbox._checked',
                            unchecked:
                                '.feedback-card-features-view__feature .card-dropdown-view__content .feedback-card-features-view__enum-feature .checkbox:not(._checked)'
                        }
                    }
                },
                range: {
                    from: '.feedback-card-features-view__number-input:nth-child(2) input',
                    to: '.feedback-card-features-view__number-input:nth-child(3) input'
                }
            }
        }
    },
    routeFeedback: {
        view: '.feedback-route-view',
        form: '.feedback-incorrect-route-view',
        routePoint: '.feedback-map-line-view__point',
        menu: {
            better: '.feedback-route-view__menu-item._type_route-edit-better',
            incorrect: '.feedback-route-view__menu-item._type_route-incorrect',
            obstruction: '.feedback-route-view__menu-item._type_route-incorrect-obstruction',
            prohibitingSign: '.feedback-route-view__menu-item._type_route-incorrect-prohibiting-sign',
            poorCondition: '.feedback-route-view__menu-item._type_route-incorrect-poor-condition',
            roadClosed: '.feedback-route-view__menu-item._type_route-incorrect-road-closed',
            incorrectPedestrian: '.feedback-route-view__menu-item._type_route-incorrect-other',
            incorrectBicycle: '.feedback-route-view__menu-item._type_route-incorrect-other',
            incorrectMasstransit: '.feedback-route-view__menu-item._type_route-incorrect-other',
            other: '.feedback-route-view__menu-item._type_route-other',
            addObject: '.feedback-route-view__menu-item._type_add-object'
        },
        items: {
            comment: {
                default: '.feedback-field-view._name_comment [placeholder]',
                optional: '.feedback-field-view._name_comment:not(._required) [placeholder]',
                required: '.feedback-field-view._name_comment._required [placeholder]'
            },
            email: '.feedback-field-view._name_email input',
            photo: '.feedback-field-view._name_photos'
        },
        buttons: {
            cancel: '.feedback-footer-container-view .button+.button',
            submit: {
                view: '.feedback-footer-container-view .button',
                disabled: '.feedback-footer-container-view .button._disabled',
                enabled: '.feedback-footer-container-view .button:not(._disabled)'
            }
        }
    },
    spotFeedback: {
        card: '.feedback-spot-view',
        sendButton: {
            disabled: '.feedback-footer-container-view button:first-child._disabled',
            enabled: '.feedback-footer-container-view button:first-child:not(._disabled)'
        },
        sidebarToggleButton: '.feedback-footer-container-view button:last-child',
        photo: '.feedback-field-view._name_photos',
        comment: '.textarea__control',
        finishScreen: '.feedback-finish-view'
    },
    editMapFeedback: {
        view: '.feedback-map-edit-view'
    },
    carparks: {
        layer: 'img[src*="carparks"]',
        control: '.parking-control'
    },
    panoramas: {
        backControl: '.panorama-back-control-view',
        routes: {
            backControl: '.panorama-route-back-control-view',
            stair: '.panorama-routes-stair-view',
            waypoint: {
                firstLast: '.panorama-routes-waypoint-view__first-last-icon',
                intermediate: '.panorama-routes-waypoint-view__intermediate-icon'
            }
        },
        routesPreview: {
            view: '.panorama-route-preview-view',
            close: '.panorama-route-preview-view__close'
        },
        player: '.panorama-player-view',
        control: '.map-panorama-photo-control',
        closeButton: '.panorama-controls-view__close > button',
        historicalControlButton: '.panorama-controls-view__history',
        historicalView: '.panorama-history-control__selector',
        checkedControl: '.map-panorama-photo-control._checked',
        layer: 'img[src*="l=stv,sta"]',
        feedback: {
            control: '.panorama-feedback-control',
            selector: '.panorama-feedback-control__selector'
        },
        shareControl: '.panorama-controls-view__group .rounded-controls__child:first-child button',
        logo: '.panorama-player-view__logo a',
        zoom: {
            in: '.panorama-player-view .zoom-control__zoom-in button:not(._disabled)',
            disabledIn: '.panorama-player-view .zoom-control__zoom-in button._disabled',
            out: '.panorama-player-view .zoom-control__zoom-out button:not(._disabled)',
            disabledOut: '.panorama-player-view .zoom-control__zoom-out button._disabled'
        },
        placemarks: {
            preview: {
                business: {
                    view: '.panorama-placemark-business-preview-view',
                    closeMobile: '.panorama-placemark-business-preview-view .close-button'
                },
                panoramaDefault: {
                    view: '.panorama-placemark-preview-view',
                    closeMobile: '.panorama-placemark-preview-view .close-button'
                }
            },
            business: {
                view: '.panorama-placemark-business-view',
                highlighted: '.panorama-placemark-business-view._highlighted',
                text: '.panorama-placemark-business-view__text'
            },
            airship: '.panorama-placemark-airship-view',
            poi: {
                view: '.panorama-placemark-poi-view',
                highlighted: '.panorama-placemark-poi-view._highlighted'
            },
            marker: {
                view: '.panorama-placemark-marker-view',
                highlighted: '.panorama-placemark-marker-view._highlighted'
            },
            metro: {
                view: '.panorama-placemark-metro-view',
                msk: '.panorama-placemark-metro-view._style_mskMetroStyle',
                spb: '.panorama-placemark-metro-view._style_spbMetroStyle',
                kiev: '.panorama-placemark-metro-view._style_kieMetroStyle',
                samara: '.panorama-placemark-metro-view._style_samaraMetroStyle',
                kazan: '.panorama-placemark-metro-view._style_kazanMetroStyle',
                nnovgorod: '.panorama-placemark-metro-view._style_nnovgorodMetroStyle',
                kharkov: '.panorama-placemark-metro-view._style_kharkovMetroStyle',
                ekb: '.panorama-placemark-metro-view._style_ekbMetroStyle',
                minsk: '.panorama-placemark-metro-view._style_minskMetroStyle',
                novosib: '.panorama-placemark-metro-view._style_novosibMetroStyle',
                almaty: '.panorama-placemark-metro-view._style_almatyMetroStyle'
            }
        },
        modeSwitcher: '.mode-switcher-view'
    },
    photo: {
        player: '.photos-player-view',
        tapeLayout: '.photos-player-view__tape-layout',
        tapeScroll: '.photos-player-view__tape-layout .scroll__container',
        list: '.photo-list',
        layer: 'img[src*="l=pht"]',
        footer: '.photos-player-view__footer',
        frames: {
            any: '.photo-list__frame-wrapper',
            withAlt:
                '.photo-list__frame-wrapper img[alt="Фото: внутри — Сиеста (Россия, Москва, Мантулинская улица, 5, стр. 5)"',
            first: '.photo-list__frame-wrapper:nth-child(1)',
            /**
             * Второй __frame-wrapper является "третьим ребенком", потому что __frame-wrapper'ы
             * расположены на нечетных местах в верстке, перемежаясь __gap'ами.
             */
            second: '.photo-list__frame-wrapper:nth-child(3)',
            last: '.photo-list__frame-wrapper:last-child'
        },
        gap: '.photo-list__gap:nth-child(2)',
        modes: {
            preview: '.photos-player-view._mode_preview',
            tape: '.photos-player-view._mode_tape'
        },
        copyright: '.photos-player-view__copyright',
        copyrightLink: '.photos-player-view__copyright .photos-player-view__link',
        arrows: {
            forward: '.photos-player-view__button._type_forward',
            forwardDisabled: '.photos-player-view__button._type_forward._disabled',
            backward: '.photos-player-view__button._type_backward',
            backwardDisabled: '.photos-player-view__button._type_backward._disabled'
        },
        counter: '.photos-player-view__index-text',
        closeButton: '.photos-player-view__button._type_close',
        share: {
            button: '.photos-control-button._type_share',
            popup: '.gallery-share-view',
            socialNetButton: '.social-share-view_gallery__link._service_telegram'
        },
        addPhotoButton: '.photos-control-button._type_add-photo',
        openBookingButton: '.photos-control-button._type_open-booking',
        addPhotoForm: {
            view: '.add-photos-form-view',
            sendButton: '.add-photos-form-view__controls-item:first-child',
            success: '.success-upload-view',
            addMoreButton: '.success-upload-view__buttons button:first-child'
        },
        addPhotos: {
            view: '.add-photos-view',
            input: '.add-photos-view input[type="file"]',
            remove: '.add-photos-view__remove',
            photos: '.add-photos-view__photos',
            photo: '.add-photos-view__photo'
        },
        tags: {
            view: '.photos-tags',
            checkedTag: '.photos-tags .button._checked',
            firstTag: '.photos-player-view__tags .carousel__item:nth-child(1) .button[type=button]',
            secondTag: '.photos-player-view__tags .carousel__item:nth-child(2) .button[type=button]'
        },
        feebackButton: '.photos-control-button._type_feedback',
        controlsView: '.photos-player-view__action-buttons-container',
        firstControl: '.photos-control-group:first-child',
        feedbackPopup: '.feedback-selector-view',
        relatedItem: '.photo-player-related-item-view'
    },
    masstransitLayer: {
        view: '.masstransit-layer-panel-view',
        typesContainer: '.masstransit-layer-panel-view__types-container',
        noDataContainer: '.masstransit-layer-panel-view__no-data',
        tooSmallZoom: '.masstransit-layer-panel-view__zoom-too-small',
        transportContainer: '.masstransit-layer-panel-view__type-pane-container',
        transportContainerDisabled: '.masstransit-layer-panel-view__type-pane-container._disabled',
        placemark: '.masstransit-animated-placemarks',
        highlightedPlacemark: '.masstransit-animated-placemarks__placemark-info._highlighted',
        placemarkSmall: '.masstransit-animated-placemarks._size_small',
        placemarkNormal: '.masstransit-animated-placemarks._size_normal',
        placemarkLarge: '.masstransit-animated-placemarks._size_large',
        icon: '.masstransit-animated-placemarks__icon',
        popup: '.popup._position_bottom .popup__content',
        busIcon: '.masstransit-layer-panel-view__icon',
        regionSubtitle: '.masstransit-layer-panel-view__subtitle',
        snippet: '.masstransit-layer-panel-view__type-pane-container'
    },
    masstransitTransportList: {
        view: '.masstransit-transport-list-panel-view',
        noInfo: '.masstransit-transport-list-panel-view__no-info',
        vehicles: '.masstransit-transport-list-panel-view__vehicles',
        firstVehicleNameIcon: '.link-wrapper:first-child .masstransit-transport-list-panel-view__vehicle-name',
        firstVehicleNameIconInRow:
            '.masstransit-transport-list-panel-view__row:first-child' +
            ' .link-wrapper:first-child .masstransit-transport-list-panel-view__vehicle-name',
        vehicleLinks: '.masstransit-transport-list-panel-view__vehicles-links .link-wrapper',
        badge: '.masstransit-transport-list-panel-view__vehicle-name',
        sendToPhone: '.masstransit-layer-panel-view__send-to-phone',
        provideCarrierData: {
            view: '.masstransit-layer-panel-view__provide-carrier-data',
            detailsButton: '.masstransit-layer-panel-view__provide-carrier-data .button'
        }
    },
    masstransitStopPreview: '.masstransit-stop-preview-view',
    masstransitRegionPlacemark: '.masstransit-region-placemark',
    masstransitConstructions: {
        view: '.map-placemark__wrapper .route-pins-view__construction',
        stairUnknown: '.map-placemark__wrapper .route-pins-view__construction._type_STAIRS_UNKNOWN',
        gates: '.map-placemark__wrapper .route-pins-view__construction._type_GATES'
    },
    masstransitStop: {
        stop: {
            tabs: {
                view: '.tabs-select-view',
                timetable: '.tabs-select-view__title._name_timetable',
                reviews: '.tabs-select-view__title._name_reviews'
            },
            timetableButton: '.masstransit-stop-panel-view__timetable',
            favoritePlacemark: '.masstransit-bookmark-placemark-view',
            collapsedView: '.collapsed-masstransit-urban-stop-view',
            collapsedViewTitle: '.collapsed-masstransit-urban-stop-view .card-title-view__title',
            container: '.masstransit-stop-panel-view',
            warningStatus: '.masstransit-stop-panel-view__warning-status',
            title: '.masstransit-stop-panel-view .card-title-view__title',
            subtitle: '.masstransit-stop-panel-view__subtitle',
            additionalInfo: '.masstransit-stop-panel-view__section._additional',
            media: '.masstransit-stop-panel-view__section._media',
            aeroexpressSnippet: '.masstransit-vehicle-snippet-view._type_aeroexpress',
            snippet: '.masstransit-vehicle-snippet-view',
            snippets: '.masstransit-brief-schedule-view__vehicles',
            snippetFirst: '.masstransit-vehicle-snippet-view:first-child',
            snippetSecond: '.masstransit-vehicle-snippet-view:nth-child(2)',
            snippetThird: '.masstransit-vehicle-snippet-view:nth-child(3)',
            snippetIcon: '.masstransit-vehicle-snippet-view__icon-inner',
            disabledThreads: '.dropdown-view .masstransit-brief-schedule-view__vehicles',
            routeButton: '.masstransit-stop-panel-view .action-button-view._type_route',
            timetableActionButton: '.action-button-view._type_timetable',
            mobileAppLink: '.card-tip-view button',
            briefSchedule: '.masstransit-stop-panel-view__brief-schedule',
            bus40: '.masstransit-vehicle-snippet-view._selected .masstransit-vehicle-snippet-view__name',
            pulse: '.masstransit-pulse-view',
            threadsButton: '.masstransit-vehicles-open-button-view:not(._wide)',
            disabledThreadsButton:
                '.masstransit-brief-schedule-view__vehicles + .masstransit-vehicles-open-button-view',
            alternateButton: '.masstransit-vehicle-snippet-view:first-child .masstransit-vehicles-open-button-view',
            threads: '.masstransit-brief-schedule-view__threads',
            threadsExpanded: '.masstransit-brief-schedule-view__threads-list._expanded',
            photosContent: '.masstransit-stop-panel-view .business-photo-list',
            breadcrumbs: '.masstransit-stop-panel-view__breadcrumbs'
        },
        line: {
            collapsedView: '.masstransit-line-title-view',
            container: '.masstransit-line-panel-view',
            title: '.masstransit-card-header-view__title',
            legend: '.masstransit-line-panel-view__legend',
            legendOpened: '.masstransit-legend-group-view._is-open',
            legendControl: '.masstransit-legend-group-view__open-button',
            legendLink: '.masstransit-legend-group-view__item-link',
            passedStops: '.masstransit-legend-group-view._is-passed',
            notPassedStops: '.masstransit-legend-group-view:not(._is-passed)',
            firstNotPassedStopEstimation:
                '.masstransit-legend-group-view:not(._is-passed)' +
                ' .masstransit-legend-group-view__item:first-child' +
                ' .masstransit-legend-group-view__item-estimation',
            list: '.masstransit-threads-view',
            type: '.masstransit-card-header-view__type',
            number: '.masstransit-card-header-view__number',
            listControl: '.masstransit-line-panel-view .masstransit-card-header-view__another-threads',
            threads: '.masstransit-card-header-view__threads',
            firstThread: '.masstransit-threads-view__item:first-child',
            secondThread: '.masstransit-threads-view__item:nth-child(2)',
            secondThreadActive: '.masstransit-threads-view__item:nth-child(2)._is-active',
            stopName: '.masstransit-legend-group-view__item:first-child .masstransit-legend-group-view__item-link',
            notPassedStopName:
                '.masstransit-legend-group-view._is-passed ~ .masstransit-legend-group-view ' +
                '.masstransit-legend-group-view__item:first-child .masstransit-legend-group-view__item-link',
            essentialStops: '.masstransit-card-header-view__essential-stops',
            mobileAppLink: '.card-tip-view button',
            bookmark: '.masstransit-line-bookmark-button-view',
            collapsedLineCardStopsSnippet: '.masstransit-line-collapsed-stops-snippet',
            tabs: {
                timetable: '.tabs-select-view__title._name_timetable'
            },
            stopSelector: {
                view: '.masstransit-stop-selector-view',
                firstSnippetSelected: '.masstransit-stop-selector-view__snippet:first-child._selected',
                secondSnippet: '.masstransit-stop-selector-view__snippet:nth-child(2)',
                secondSnippetSelected: '.masstransit-stop-selector-view__snippet:nth-child(2)._selected',
                change: '.masstransit-stop-selector-view__change'
            },
            blueLine: 'path[stroke="#7373E6"]',
            greenLine: 'path[stroke="#2F8C00"]'
        },
        timetable: {
            mobileFilter: {
                carousel: {
                    view: '.masstransit-timetable-filter-view__carousel',
                    item:
                        '.masstransit-timetable-filter-view__carousel .masstransit-timetable-filter-view__item._type_bus',
                    selectedItem:
                        '.masstransit-timetable-filter-view__carousel .masstransit-timetable-filter-view__item._selected',
                    dropDownItem:
                        '.masstransit-timetable-filter-view__carousel .masstransit-timetable-filter-view__item:first-child'
                },
                view: '.masstransit-timetable-filter-view__filter',
                item: '.masstransit-timetable-filter-view__filter .masstransit-timetable-filter-view__item',
                selectedItem:
                    '.masstransit-timetable-filter-view__filter .masstransit-timetable-filter-view__item._selected',
                applyChangesButton:
                    '.masstransit-timetable-filter-view__filter-footer .masstransit-timetable-filter-view__button',
                resetButton:
                    '.masstransit-timetable-filter-view__filter-footer .button._view_secondary-blue._size_medium'
            },
            filter: {
                item: '.masstransit-timetable-view__filter .masstransit-transport-list-view__type-transport._type_bus',
                selectedItem: '.masstransit-transport-list-view__type-transport._type_bus._highlighted'
            },
            view: '.masstransit-timetable-view',
            advert: '.masstransit-stop-panel-view .banner-view',
            actions: '.masstransit-timetable-view__settings-actions',
            spinner: '.masstransit-timetable-view__spinner',
            transports: '.masstransit-timetable-view__transports',
            delimeter: '.masstransit-timetable-view__border',
            calendarMonth: '.masstransit-timetable-view__calendar .react-datepicker__month',
            calendarDaySelected: '.masstransit-timetable-view__calendar .react-datepicker__day--selected',
            vehicleSnippets: '.masstransit-timetable-view__transports > .masstransit-vehicle-snippet-view',
            resetFilterButton: '.masstransit-timetable-view__reset-filter',
            firstSnippet: '.masstransit-vehicle-snippet-view:first-child',
            subTime: '.masstransit-timetable-view__sub-time',
            time: '.masstransit-timetable-view__time',
            settings: '.masstransit-timetable-view__settings',
            settingsSticked: '.masstransit-timetable-view__settings._sticked',
            snippetThird: '.masstransit-timetable-view__transports .masstransit-vehicle-snippet-view:nth-child(3)',
            stopName: '.masstransit-timetable-view__action-button._type_stop'
        },
        lineStopPlacemark: '.masstransit-line-view__icon',
        lineStopPlacemarkSmall: '.masstransit-line-view__icon._size_small',
        lineStopPlacemarkNormal: '.masstransit-line-view__icon._size_normal',
        lineStopPlacemarkLarge: '.masstransit-line-view__icon._size_large',
        placemarkIcon: '.masstransit-stop-placemark > * > *:last-child',
        stopPlacemarkIcon: {
            view: '.masstransit-stop-placemark .masstransit-stop-placemark__background-icon',
            bookmark: '.masstransit-stop-placemark._bookmark .masstransit-stop-placemark__background-icon'
        },
        dotStop: '.map-routes-masstransit-view__stop-dot',
        panel: '.sidebar-view._name_masstransit',
        panelNarrow: '.sidebar-view._name_masstransit:not(._view_wide)',
        panelWide: '.sidebar-view._name_masstransit._view_wide',
        transportList: '.masstransit-transport-list-view',
        close: '.sidebar-view._name_masstransit .sidebar-panel-view__close',
        edit: '.masstransit-stop-panel-view__edit',
        linkToOrgFeedback: '.feedback-selector-view__item._name_link-to-org-feedback',
        prognoses: '.masstransit-prognoses-view',
        typeSecondName:
            '.masstransit-transport-list-view__type:nth-child(2) .masstransit-transport-list-view__type-name',
        typeTransortLastItem: '.masstransit-transport-list-view__type-transport:last-child',
        clickTransportItem: '.masstransit-transport-list-view__type-transport:first-child',
        hoverTransportItem: '.masstransit-transport-list-view__type-transport:nth-child(2)',
        hoveredVehicleItem: '.masstransit-vehicle-snippet-view._hovered',
        unknownVehicleItem: '.masstransit-vehicle-snippet-view._unknown-schedule',
        transit: {
            view: '.masstransit-stop-panel-view .card-transit-view',
            distance: '.masstransit-stops-view__stop-distance'
        }
    },
    metroStop: {
        panel: '.masstransit-stop-panel-view._type_underground',
        lineName: '.masstransit-underground-line-name-view',
        metroJams: {
            full: '.metro-jams-view._view_full',
            details: '.metro-jams-view._view_details',
            metroDetails: '.metro-jams-view._view_metro-details',
            mini: '.metro-jams-view._view_mini'
        }
    },
    railwayStation: {
        panel: '.masstransit-stop-panel-view._type_railway',
        timetableLink: '.masstransit-stop-panel-view__timetable-link',
        briefSchedule: '.masstransit-brief-schedule-view'
    },
    loginDialog: {
        view: '.login-dialog-view',
        typeFavorites: '.login-dialog-view._type_favorites',
        typeFeedback: '.login-dialog-view._type_feedback',
        typeVerification: '.login-dialog-view._type_verification',
        typeCommentator: '.login-dialog-view._type_commentator',
        typeFavoritesMasstransit: '.login-dialog-view._type_favoritesMasstransit',
        typeUgcProfile: '.login-dialog-view._type_ugcProfile',
        buttonsContainer: '.login-dialog-view__button'
    },
    passportLoginDomik: {
        page: '.passp-page',
        anotherLink: '.passp-auth-header-link[href*=auth]',
        form: '.passp-login-form',
        loginInput: 'input#passp-field-login',
        passwordInput: 'input#passp-field-passwd',
        submitButton: '.passp-sign-in-button',
        ignorePhoneButton: '[data-t="phone_actual_submit"] button',
        ignoreSocialButton: '.request-social_back-button button',
        ignoreAvatarButton: '.registration__avatar-btn',
        ignoreEmailButton: '[data-t=email_skip]'
    },
    bookmarks: {
        panel: '.sidebar-view._name_bookmarks .scroll',
        panelView: '.bookmarks-panel-view',
        bookmarksLayersMenu: {
            view: '.bookmarks-layers-menu-view',
            back: '.bookmarks-layers-menu-view__back',
            close: '.bookmarks-layers-menu-view__header > .close-button'
        },
        actionsMenu: '.options-control__content',
        actionsMenuSwitchControl: '.options-control__content .switch-control',
        snippet: '.bookmark-snippet-view',
        snippetPhoto: '.bookmark-snippet-view .photos-thumbnail-view img',
        loadingSnippet: '.bookmark-snippet-view__loading',
        hoveredSnippet: '.bookmarks-place-snippet-view._hovered',
        removedSnippet: '.bookmarks-removed-snippet-view',
        firstSnippet: {
            view: '.bookmark-snippet-view:nth-child(1)',
            loader: '.bookmark-snippet-view:nth-child(1) .bookmark-snippet-view__loader',
            edit: '.bookmark-snippet-view:nth-child(1) .options-control',
            linkOverlay: '.bookmark-snippet-view:nth-child(1) .search-snippet-view__link-overlay',
            title:
                '.bookmark-snippet-view:nth-child(1) .search-snippet-view__title, ' +
                '.bookmark-snippet-view:nth-child(1) .search-business-snippet-view__title',
            body: {
                view: '.bookmark-snippet-view:nth-child(1) .search-snippet-view__body',
                hovered: '.bookmark-snippet-view:nth-child(1) .search-snippet-view__body._hovered',
                noFill: '.bookmark-snippet-view:nth-child(1) .search-snippet-view__body._no-fill'
            },
            comment: {
                view: '.bookmark-snippet-view:nth-child(1) .bookmark-snippet-view__comment',
                input: '.bookmark-snippet-view:nth-child(1) .bookmark-snippet-view__comment .textarea__control',
                inputEllipsis:
                    '.bookmark-snippet-view:nth-child(1) .bookmark-snippet-view__comment .textarea__control .textarea__text-clamp',
                submit: {
                    view: '.bookmark-snippet-view:nth-child(1) .bookmark-comment-form__submit-wrap',
                    button: '.bookmark-snippet-view:nth-child(1) .bookmark-comment-form__submit-button'
                }
            }
        },

        tabs: {
            places: '.tabs-select-view__title._name_places',
            stops: '.tabs-select-view__title._name_stops',
            lines: '.tabs-select-view__title._name_lines'
        },

        masstransitEmptyPanel: '.masstransit-bookmarks-empty-panel-view',

        stopsPanel: {
            view: '.masstransit-bookmarks-stops-panel-view',
            stopItem: {
                view: '.masstransit-bookmarks-stop-view',
                spinner: '.masstransit-bookmarks-stop-view__spinner',
                geolocation: '.masstransit-bookmarks-stop-view__title-distance',
                title: '.masstransit-bookmarks-stop-view__title-text',
                vehicle: '.masstransit-vehicle-snippet-view',
                edit: '.options-control',
                moreRoutesButton: '.masstransit-vehicles-open-button-view._wide',
                openedRoutes: '.dropdown-view .masstransit-bookmarks-stop-view__vehicles',
                recover: '.bookmarks-removed-snippet-view__recover-link'
            }
        },

        linesPanel: {
            view: '.masstransit-bookmarks-lines-panel-view',
            lineItem: {
                view: '.masstransit-bookmarks-lines-panel-view__item',
                secondChild: '.masstransit-bookmarks-lines-panel-view__item:nth-child(2)',
                edit: '.options-control',
                recover: '.bookmarks-removed-snippet-view__recover-link'
            }
        },

        folder: {
            view: '.bookmarks-folder-view',
            header: {
                viewMinifed: '.bookmarks-folder-header-view__fixed',
                view: '.bookmarks-folder-header-view',
                title: '.bookmarks-folder-header-view__title',
                description: '.bookmarks-folder-header-view__description',
                editButton: '.bookmarks-folder-header-view__fixed .options-control',
                collpased: {
                    editButton: '.sidebar-panel-header-view .options-control'
                }
            }
        },

        folderSnippet: {
            view: '.bookmarks-folder-snippet-view',
            first: '.bookmarks-folder-snippet-view:first-child',
            firstAfterFav: '.bookmarks-folder-snippet-view:nth-child(2)',
            last: '.bookmarks-folder-snippet-view:last-child',
            new: '.bookmarks-root-folder-view__new-folder-link',
            headerTitle: '.bookmarks-folder-header-view__title',
            editButton: '.options-control',
            info: '.bookmarks-folder-snippet-view__bookmarks-map-info'
        },
        firstFolderSnippet: {
            view: '.bookmarks-folder-snippet-view:nth-child(1)',
            edit:
                '.bookmarks-folder-snippet-view:nth-child(1) .bookmarks-folder-snippet-view__edit-overlay .options-control',
            title: '.bookmarks-folder-snippet-view:nth-child(1) .bookmarks-folder-snippet-view__title',
            count: '.bookmarks-folder-snippet-view:nth-child(1) .bookmarks-folder-snippet-view__bookmarks-count'
        },
        afterFavFolderSnippet: {
            view: '.bookmarks-folder-snippet-view:nth-child(2)',
            edit: '.bookmarks-folder-snippet-view:nth-child(2) .options-control',
            title: '.bookmarks-folder-snippet-view:nth-child(2) .bookmarks-folder-snippet-view__title',
            count: '.bookmarks-folder-snippet-view:nth-child(2) .bookmarks-folder-snippet-view__bookmarks-count'
        },
        secondFolderSnippet: {
            view: '.bookmarks-folder-snippet-view:nth-child(2)',
            count: '.bookmarks-folder-snippet-view:nth-child(2) .bookmarks-folder-snippet-view__bookmarks-count'
        },
        lastFolderSnippet: {
            edit: '.bookmarks-folder-snippet-view:last-child .options-control',
            title: '.bookmarks-folder-snippet-view:last-child .bookmarks-folder-snippet-view__title',
            count: '.bookmarks-folder-snippet-view:last-child .bookmarks-folder-snippet-view__bookmarks-count'
        },

        favorites: '.bookmarks-root-folder-view__favorites',

        confirmation: {
            view: '.bookmarks-confirmation-view',
            input: '.bookmarks-confirmation-view__content .input .input__control',
            clearInput: '.bookmarks-confirmation-view__content .input .input__clear',
            submitButton: '.bookmarks-confirmation-view__submit-button .button:not(._disabled)'
        },
        home: {
            view: '.bookmarks-place-snippet-view:nth-child(1)',
            address: '.bookmarks-place-snippet-view:nth-child(1) .bookmarks-place-snippet-view__address',
            add: '.bookmarks-place-snippet-view:nth-child(1) .bookmarks-place-snippet-view__add-button .button',
            edit: '.bookmarks-place-snippet-view:nth-child(1) .options-control'
        },
        work: {
            view: '.bookmarks-place-snippet-view:nth-child(2)',
            address: '.bookmarks-place-snippet-view:nth-child(2) .bookmarks-place-snippet-view__address',
            add: '.bookmarks-place-snippet-view:nth-child(2) .bookmarks-place-snippet-view__add-button .button',
            edit: '.bookmarks-place-snippet-view:nth-child(2) .options-control'
        },

        move: {
            view: '.bookmarks-move-view',
            toNew: '.bookmarks-move-view__create-and-move-button',
            toFavorite: '.bookmarks-move-view__menu .menu-item:nth-child(1)',
            firstList: '.bookmarks-move-view .menu-item:nth-child(2) .menu-item__tick',
            secondList: '.bookmarks-move-view__menu .menu-item:nth-child(3)'
        },

        placemark: {
            view: '.bookmark-placemark',
            place: '.bookmark-placemark__place-bookmark',
            icon: '.bookmark-placemark .bookmark-placemark__icon',
            hovered: '.bookmark-placemark._hover',
            active: '.bookmark-placemark__active-bookmark',
            favorite: {
                contentWrapper: '.bookmark-placemark__favorite-bookmark-content-wrapper',
                pin: '.bookmark-placemark__favorite-bookmark-pin',
                title: '.bookmark-placemark__favorite-bookmark-title',
                titleAlwaysShow: '.bookmark-placemark__favorite-bookmark-title._always-show-title',
                titleAlwaysShowBig: '.bookmark-placemark__favorite-bookmark-title._always-show-title._bigger-text'
            }
        },

        suggest: '.suggest__content',
        suggestItem: '.suggest-item-view:nth-child(1)',
        icon: {
            view: '.bookmark-icon'
        },
        folderForm: {
            view: '.bookmarks-folder-form-view',
            inputTitle: '.bookmarks-folder-form-view__wrap-input-title input',
            inputDescription: '.bookmarks-folder-form-view__add-description-form textarea',
            showInputDescription: '.bookmarks-folder-form-view__open-add-description',
            selectIconLink: '.bookmarks-folder-form-view__select-icon-link'
        },
        actionForm: {
            view: '.bookmarks-action-view',
            title: '.bookmarks-action-view__header-title',
            submitButton: '.bookmarks-action-view__wrap-button-submit',
            submitButtonDisabled: '.bookmarks-action-view__wrap-button-submit .button._disabled',
            backButton: '.panel-header-view__back-icon'
        },
        selectFolderIcon: {
            view: '.bookmarks-select-icon-view',
            colorItem: '.bookmarks-select-icon-view__color-wrap',
            color: '.bookmarks-select-icon-view__color',
            selectedColor: '.bookmarks-select-icon-view__color._selected',
            iconItem: '.bookmarks-select-icon-view__icon-wrap',
            icon: '.bookmarks-select-icon-view__icon',
            selectedIcon: '.bookmarks-select-icon-view__icon._selected'
        },
        search: {
            view: '.bookmarks-search-input',
            input: '.bookmarks-search-input input'
        },
        selectFolder: {
            view: '.bookmarks-select-folder-view'
        }
    },
    orgpage: {
        view: '.business-card-view._wide',
        breadcrumbs: '.breadcrumbs-view',
        spinner: '.content-panel-view__spinner',
        header: {
            wrapper: '.orgpage-header-view__header-wrapper',
            title: '.orgpage-header-view__header',
            workingStatus: '.orgpage-header-view__working-status .business-working-status-flip-view',
            workingStatusPopup: '.orgpage-working-status-view__table',
            phones: {
                view: '.orgpage-phones-view',
                moreButton: '.orgpage-phones-view__more',
                popup: '.orgpage-phones-view__phones'
            },
            phone: {
                view: '.orgpage-phone-view',
                call: '.orgpage-phone-view__link._call'
            },
            address: '.orgpage-header-view__address',
            addressNew: '.orgpage-header-view__moved',
            ratingAndAwards: {
                wrapper: '.orgpage-header-view__wrapper'
            }
        },
        businessStatus: {
            view: '.business-status-warning-view._big',
            item: '.orgpage-photos-view__item'
        },
        photo: {
            view: '.orgpage-photos-view',
            item: '.orgpage-photos-view__item',
            lastItem: '.orgpage-photos-view .carousel__item:last-of-type .orgpage-photos-view__item',
            nextButton: '.orgpage-photos-view .carousel__arrow-wrapper:not(._prev) .carousel__arrow',
            addButton: '.orgpage-photos-view__button'
        },
        panorama: '.business-card-view._wide .panorama-thumbnail-view',
        info: {
            editButton: '.orgpage-info-view__edit',
            expandButton: '.orgpage-info-view__expand',
            lastFeature: '.orgpage-info-view__feature:last-child'
        },
        summary: {
            view: '.business-card-view._wide .business-summary'
        },
        contacts: {
            link: '.business-urls-view__url',
            socialLink: '.orgpage-social-links-view__icon',
            panorama: '.business-card-view__panorama',
            masstransit: {
                metro: {
                    distance: '.masstransit-stops-view._type_metro .masstransit-stops-view__stop-distance'
                },
                aboveground: {
                    distance: '.masstransit-stops-view._type_masstransit .masstransit-stops-view__stop-distance'
                },
                mapRoute: '.ymaps3x0--graphics-layer svg',
                expandButton: '.orgpage-masstransit-stops-view__control',
                firstCollapsedStation: '.dropdown-view .orgpage-masstransit-stops-view__stop:nth-child(1)'
            },
            oldAdvert: '.orgpage-contacts-view__advert',
            showEntrances: '.business-contacts-view__link._entrances',
            workingStatusView: '.orgpage-working-status-view__working-status',
            workingStatusPopup: '.orgpage-working-status-view__table'
        },
        categoriesTags: {
            view: '.orgpage-categories-info-view',
            link: '.orgpage-categories-info-view__link'
        },
        faq: {
            view: '.orgpage-faq-view',
            listView: '.orgpage-faq-view__faq-list',
            showMore: '.orgpage-faq-view__show-more',
            listItem: '.orgpage-faq-view__list-item',
            idClosed: '.orgpage-faq-view__list-item._id_closed',
            idClosedMoved: '.orgpage-faq-view__list-item._id_closed-moved',
            idDirections: '.orgpage-faq-view__list-item._id_directions',
            idContacts: '.orgpage-faq-view__list-item._id_contacts',
            idLocation: '.orgpage-faq-view__list-item._id_location',
            idAppearance: '.orgpage-faq-view__list-item._id_appearance',
            idHours: '.orgpage-faq-view__list-item._id_hours',
            idCityChain: '.orgpage-faq-view__list-item._id_city-chain',
            linkNewAddress: '.orgpage-faq-view__link._link_new-address',
            linkSimilar: '.orgpage-faq-view__link._link_similar',
            linkPedestrian: '.orgpage-faq-view__link._link_pedestrian',
            linkAuto: '.orgpage-faq-view__link._link_auto',
            linkMasstransit: '.orgpage-faq-view__link._link_masstransit',
            linkTaxi: '.orgpage-faq-view__link._link_taxi',
            linkContacts: '.orgpage-faq-view__link._link_contacts',
            linkPanorama: '.orgpage-faq-view__link._link_panorama',
            linkGallery: '.orgpage-faq-view__link._link_gallery',
            linkHours: '.orgpage-faq-view__link._link_hours',
            linkCityChain: '.orgpage-faq-view__link._link_city-chain'
        },
        ownership: {
            view: '.business-ownership-view',
            title: '.business-ownership-view__title',
            link: '.business-ownership-view__link',
            titleLink: '.business-ownership-view__title-link'
        },
        similar: {
            image: '.orgpage-similar-view .orgpage-similar-item__image'
        },
        menu: {
            categories: {
                view: '.business-related-items-rubricator',
                selected: '.business-related-items-rubricator__selected-category'
            },
            actionButton: '.business-full-items-view .photos-control-button'
        },
        placesInside: {
            tab: '.business-full-items-grouped-view',
            searchInput: '.business-full-items-grouped-view__input input',
            categories: {
                view: '.business-related-items-rubricator',
                selected: '.business-related-items-rubricator__selected-category'
            }
        }
    },
    suggest: {
        panel: '.suggest__items',
        content: '.suggest__content',
        items: {
            basic: '.suggest-item-view',
            first: '.suggest-item-view:nth-child(1)'
        },
        itemTitle: '.suggest-item-view__title'
    },
    map: {
        anySelectedPlacemark:
            'div[class*=placemark]._mode_active, .bookmark-placemark._active, .masstransit-stop-placemark',
        anyUnselectedPlacemark: 'div[class*=placemark]:not(._mode_active)',
        container: '.map-container',
        entrances: '.map-entrances-view',
        copyrights: '.map-copyrights__item._type_copyrights',
        support: '.map-copyrights__item._type_support',
        layers: {
            control: '.map-layers-control',
            controlChecked: '.map-layers-control > button._checked',
            controlNotChecked: '.map-layers-control > button:not(._checked)',
            view: '.map-layers-view',
            item: {
                view: '.list-item-view',
                active: '.list-item-view._active'
            },
            menu: '.shutter'
        },
        circlePlacemark: {
            view: '.map-circle-placemark',
            blue: '.map-circle-placemark[style="color: rgb(23, 126, 230);"]',
            green: '.map-circle-placemark[style="color: rgb(59, 179, 0);"]',
            red: '.map-circle-placemark[style="color: rgb(255, 68, 51);"]'
        },
        blackoutLayout: '.blackout-layout'
    },
    headerControls: {
        traffic: '.traffic-control',
        route: '.route-control',
        trafficChecked: '.traffic-control._checked',
        masstransit: '.masstransit-control',
        masstransitChecked: '.masstransit-control._checked',
        masstransitFilter: '.masstransit-filter-layer-control__text'
    },
    printPage: {
        body: '.body',
        view: '.print-view',
        firstPage: '.print-view__first-page',
        toggler: '.print-route-list-view__toggler-page',
        map: '.print-map-view',
        topControls: {
            size: '.print-view__header-control-layout .print-controls-view__select:nth-child(1) .select',
            orientation: '.print-view__header-control-layout .print-controls-view__select:nth-child(2) .select'
        },
        sizes: {
            letter: '.print-controls-view__select:nth-child(1) .menu-item:nth-child(5)'
        },
        orientations: {
            portrait: '.print-controls-view__select:nth-child(2) .menu-item:nth-child(1)',
            landscape: '.print-controls-view__select:nth-child(2) .menu-item:nth-child(2)'
        },
        masstransit: {
            vehiclesList: '.print-masstransit-stop-view__vehicles-list',
            routeLegend: '.print-masstransit-stop-view__legend',
            stopName: '.print-masstransit-stop-view__name'
        },
        routes: {
            auto: '.print-route-list-view._type_auto',
            masstransit: '.print-route-list-view._type_masstransit',
            short: '.print-view__route-info',
            waypointFirst: '.print-route-list-view__item._waypoint._index_0',
            waypointSecond: '.print-route-list-view__item._waypoint._index_1',
            waypointThird: '.print-route-list-view__item._waypoint._index_2',
            infoTitle: '.print-view__route-info-title'
        },
        cargoWarning: '.print-route-list-view__toggler-page .route-warning-cargo-view',
        searchList: '.print-search-list-view',
        descriptionList: '.print-description-list',
        singleSearch: '.print-single-search-view',
        placemark: '.map-circle-placemark',
        share: {
            sendToPhoneButton: '.print-promo-banner-view__actions-confirm button'
        }
    },
    discovery: {
        view: '.discovery-view',
        title: '.discovery-view__title',
        partnerTitle: '.discovery-view__partner-title',
        info: '.discovery-view__info',
        cover: '.discovery-view__photo',
        promo: '.discovery-promo-view',
        headerShare: '.social-share-view_discovery-small',
        footerShare: '.social-share-view_discovery-large',
        partner: {
            view: '.discovery-view__partner',
            title: '.discovery-view__partner-title'
        },
        partnerLinks: {
            view: '.discovery-partner-links-view',
            firstItem: '.carousel__item:first-of-type .discovery-partner-links-view__item'
        },
        organization: {
            view: '.discovery-organization-view',
            viewFirst: '.discovery-organization-view._is-first',
            photos: '.discovery-organization-view__photos',
            title: '.discovery-organization-view__title-link',
            feature: '.discovery-organization-view__feature',
            firstCollapsedSentence: '.discovery-organization-view._is-first .spoiler-view__text._collapsed'
        },
        balloon: {
            view: '.discovery-balloon-view',
            title: '.discovery-balloon-view__title',
            buttons: {
                route: '.discovery-balloon-view__control:first-child',
                close: '.discovery-balloon-view__close'
            }
        },
        seeAlso: {
            view: '.discovery-links-view',
            title: '.discovery-links-view .card-collection-view__title'
        },
        placemark: {
            view: '.discovery-placemark',
            default: '.discovery-placemark__placemark',
            hover: '.discovery-placemark__placemark._hover',
            title: '.search-placemark-title'
        },
        related: {
            view: '.discovery-related-view',
            item: '.discovery-related-view__item'
        }
    },
    maillist: {
        view: '.maillist-form-view',
        button: '.maillist-form-view .maillist-form-view__submit',
        input: '.maillist-form-view input'
    },
    usermaps: {
        panel: {
            view: '.user-maps-panel-view',
            title: '.user-maps-panel-view__title',
            description: '.user-maps-panel-view__description',
            createMap: '.user-maps-panel-view__controls .card-banner-view__banner',
            status: '.user-maps-panel-view__status'
        },
        share: '.user-maps-share-view',
        balloon: '.user-maps-balloon-view',
        features: '.user-maps-features-view',
        feature: '.user-maps-features-view__feature',
        activeFeature: '.user-maps-features-view__feature._active',
        feature3: '.user-maps-features-view__feature:nth-child(3)',
        feature4: '.user-maps-features-view__feature:nth-child(4)',
        reportDialog: {
            iframe: 'iframe[src^="https://forms.yandex.ru/"]'
        }
    },
    sidebarToggleButton: {
        collapse: '.sidebar-toggle-button:not(._collapsed)',
        expand: '.sidebar-toggle-button._collapsed'
    },
    pinPlacemark: {
        view: '.pin-placemark-view',
        icon: '.pin-placemark-view__icon'
    },
    masstransitRoutePin: {
        view: '.masstransit-route-pin-view',
        text: '.masstransit-route-pin-view__text',
        arrow: '.masstransit-route-pin-view__arrow',
        balloonText: '.masstransit-route-pin-view__balloon-text',
        zeroSizeDots: '.masstransit-route-pin-view__dot._size_none',
        balloon: {
            default: '.masstransit-route-pin-view__balloon-item',
            water: '.masstransit-route-pin-view__balloon-item._type_water',
            cable: '.masstransit-route-pin-view__balloon-item._type_cable',
            funicular: '.masstransit-route-pin-view__balloon-item._type_funicular',
            metro: '.masstransit-route-pin-view__balloon-item._type_underground',
            metroExit: '.masstransit-route-pin-view__balloon-item._type_metro-exit',
            aeroexpress: '.masstransit-route-pin-view__balloon-item._type_aeroexpress'
        }
    },
    nearbyPinView: '.search-nearby-pin-view__pin-icon',
    hoverHint: '.hover-hint',
    contentPanelView: {
        error: '.content-panel-error-view',
        errorButton: '.content-panel-error-view__button button',
        header: '.content-panel-header'
    },
    contentPanelHeader: {
        view: '.content-panel-header',
        headerForm: {
            view: '.content-panel-header__search-placeholder .search-form-view',
            input: '.content-panel-header__search-placeholder .search-form-view__input input',
            submit: '.content-panel-header__search-placeholder .big-search-form-view__search-button'
        },
        headerButtons: {
            pin: '.content-panel-header__icon',
            close: '.content-panel-header__close',
            search: '.content-panel-header-menu-item._name_search',
            route: '.content-panel-header-menu-item._name_build_route'
        }
    },
    cardMedia: {
        view: '.card-media-view',
        photoBlock: '.card-media-preview._type_photo',
        panoramaBlock: '.card-media-preview._type_panorama',
        addPhoto: '.card-add-photos-view'
    },
    cardFooter: {
        view: '.card-footer-view'
    },
    bookingPhotos: {
        view: '.photos-booking-view',
        title: '.photos-booking-view__title',
        operator: '.photos-booking-view__operator',
        firstLink: '.photos-booking-view__link:nth-child(1)'
    },
    hotelsBooking: {
        cardView: '.card-hotels-booking-view',
        list: '.hotels-booking-partner-prices',
        firstLink: '.hotels-booking-partner-prices__item:nth-child(1) .hotels-booking-partner-prices__link',
        controls: {
            datepicker: '.hotels-booking-header__control:nth-child(1) .button',
            datepickerText: '.hotels-booking-header__control:nth-child(1) .button__text',
            calendar: '.react-datepicker',
            dayFrom: '.react-datepicker__week:nth-child(2) .react-datepicker__day:nth-child(2)',
            dayFromAlt: '.react-datepicker__week:nth-child(3) .react-datepicker__day:nth-child(3)',
            dayTo: '.react-datepicker__week:nth-child(3) .react-datepicker__day:nth-child(4)',
            personsSelect: '.hotels-booking-header__control:nth-child(2) .button',
            personsDropdown: '.hotels-booking-header__controls .select__dropdown',
            onePersonOption: '.hotels-booking-header__controls .select__dropdown .menu-item:nth-child(1)',
            threePersonsOption: '.hotels-booking-header__controls .select__dropdown .menu-item:nth-child(3)'
        },
        moreButton: '.card-hotels-booking-view__show-more',
        closeCalendarButton: '.card-hotels-booking-view__calendar-close-button'
    },
    ugc: {
        businessHeader: {
            view: '.business-header-rating-view',
            reviewsLink: '.business-header-rating-view__text._clickable'
        },
        reviews: {
            view: '.card-reviews-view',
            summary: '.card-reviews-view__summary',
            actions: '.card-reviews-view__actions',
            badgeView: '.business-summary-rating-badge-view',
            badgeCount: '.business-summary-rating-badge-view__rating-count',
            review: '.ugc-review-view',
            impression: '.ugc-impression-review-question'
        },
        badge: {
            view: '.business-rating-badge-view',
            star: '.business-rating-badge-view__star',
            fullStar: '.business-rating-badge-view__star:not(._half):not(._empty)',
            halfStar: '.business-rating-badge-view__star._half',
            emptyStar: '.business-rating-badge-view__star._empty',
            wrapper: '.business-rating-with-text-view',
            count: '.business-rating-with-text-view__count'
        },
        ratingEdit: {
            view: '.business-rating-edit-view',
            nthStar: '.business-rating-edit-view__star:nth-child(%i)',
            nthSelectedStar: '.business-rating-edit-view__star:nth-child(%i)._selected'
        },
        ranking: {
            view: '.rating-ranking-view',
            popup: '.rating-ranking-view__popup'
        },
        reactions: {
            view: '.business-reactions-view',
            like: '.business-reactions-view__icon:not(._dislike)',
            dislike: '.business-reactions-view__icon._dislike',
            likeCounter: '.business-reactions-view__icon:not(._dislike) + .business-reactions-view__counter',
            dislikeCounter: '.business-reactions-view__icon._dislike + .business-reactions-view__counter'
        },
        amount: {
            view: '.business-rating-amount-view'
        },
        suggestion: {
            view: '.business-review-suggestion-view',
            gratitude: '.business-review-suggestion-view__extra',
            rate: '.business-review-suggestion-view__rate'
        },
        impressions: {
            view: '.ugc-impressions-view__impression',
            title: '.ugc-impression-review-question-view__title'
        },
        reviewForm: {
            view: '.business-review-form',
            title: '.business-review-form__title',
            comment: {
                view: '.business-review-form-comment-view',
                textarea: '.business-review-form-comment-view__textarea textarea'
            },
            sendButton: '.business-review-form__controls-item:first-child',
            cancelButton: '.business-review-form__controls-item:last-child',
            gratitude: '.business-review-dialog-form__gratitude',
            gratitudeCloseButton: '.business-review-dialog-form__gratitude button',
            spinner: '.business-review-form__spinner',
            stars: {
                view: '.business-rating-edit-view'
            }
        },
        review: {
            view: '.business-review-view',
            userView: '.business-user-review-card-view',
            editButton: '.business-review-view__edit',
            deleteButton: '.business-review-view__delete',
            restoreButton: '.business-review-view__restore',
            writeAgainButton: '.business-review-view__write-again',
            userIcon: '.business-review-view__user-icon',
            date: '.business-review-view__date',
            text: '.business-review-view__body',
            toggleBusinessComment: '.business-review-view__comment-expand',
            businessComment: '.business-review-comment__comment',
            photos: {
                carousel: '.business-review-view__carousel',
                view: '.business-review-photos',
                item: '.business-review-photos__item'
            },
            moderationStatus: '.business-review-view__moderation-status',
            moderationRules: '.business-review-view__moderation-rules',
            reactionsContainer: '.business-review-view__reactions-container',
            controls: '.business-review-view__controls',
            commentator: '.business-review-view__commentator',
            card: {
                view: '.business-reviews-card-view',
                tagsContainer: '.business-reviews-card-view__tags-container',
                showMore: '.business-reviews-card-view__more',
                item: '.business-reviews-card-view__review',
                nthItem: '.business-reviews-card-view__review:nth-child(%i)'
            },
            toggleQuoteMode: '.business-review-view__toggle-mode'
        },
        aspects: {
            view: '.business-review-aspects',
            item: '.business-aspect-view',
            selectedItem: '.button._view_accent .business-aspect-view',
            nthItem: '.business-review-aspects .carousel__item:nth-child(%i) .business-aspect-view',
            selectedNthItem:
                '.business-review-aspects .carousel__item:nth-child(%i) .button._view_accent .business-aspect-view',
            photos: {
                view: '.review-aspect-photos',
                nthItem: '.review-aspect-photos .carousel__item:nth-child(%i)',
                lastItem: '.review-aspect-photos .carousel__item:last-of-type',
                firstChild: '.review-aspect-photos .carousel__item:first-child',
                nextArrow: '.review-aspect-photos ._next',
                prevArrow: '.review-aspect-photos ._prev'
            }
        },
        commentator: {
            view: '.cmnt-main',
            form: '.cmnt-form',
            themes: {
                light: '.cmnt-theme_maps',
                dark: '.cmnt-theme_dark'
            },
            item: '.cmnt-item'
        },
        common: {
            feed: {
                view: '.ugc-feed-view',
                items: '.ugc-feed-view__items',
                footer: '.ugc-feed-view__footer',
                footerLink: '.ugc-feed-view__footer a.button',
                item: '.ugc-feed-item-view',
                secondItem: '.ugc-feed-item-view:nth-child(2)',
                itemSelected: '.ugc-feed-item-view._selected'
            },
            placeholder: {
                view: '.ugc-placeholder-view',
                link: '.ugc-placeholder-view a.button',
                button: '.ugc-placeholder-view .button'
            },
            options: {
                control: '.ugc-feed-item-view__control',
                menu: '.options-control__content'
            },
            tabs: {
                view: '.tabs-select-view',
                items: {
                    panoramas: '.tabs-select-view__title ._name_panoramas'
                }
            }
        },
        contributions: {
            view: 'ugc-feedback-view',
            firstFeedback: '.ugc-feedback-snippet-view._bordered .ugc-feedback-header-view__title'
        },
        feedback: {
            item: {
                header: '.ugc-feedback-header-view'
            },
            modeSwitcher: {
                draftMode: '.mode-switcher-view__item._key_drafts'
            }
        },
        feedbackDraft: {
            draftWrapper: '.feedback-draft-wrapper',
            confirm: {
                view: '.ugc-feedback-draft-confirm-view'
            },
            error: {
                view: '.ugc-feedback-drafts-view__error'
            },
            draft: {
                view: '.ugc-feedback-draft-view',
                menu: {
                    view: '.ugc-feedback-draft-view__option-control',
                    content: '.options-control__content'
                }
            }
        },
        mrc: {
            view: '.ugc-mrc-list-view',
            editor: {
                view: '.ugc-mrc-ride-view',
                button: '.ugc-mrc-ride-view__button',
                buttonDisabled: '.ugc-mrc-ride-view__button button[disabled=true]',
                slider: {
                    start: '.ugc-ride-edit-slider__handler._type_start',
                    end: '.ugc-ride-edit-slider__handler._type_end'
                }
            },
            image: {
                view: '.ugc-mrc-image-view',
                close: '.ugc-mrc-image-view__close',
                play: '.ugc-ride-mini-timeline__playback',
                slider: '.ugc-ride-mini-slider'
            },
            rideTimeline: {
                play: '.ugc-ride-timeline__playback'
            }
        },
        assignments: {
            feed: {
                view: '.ugc-assignments-feed-view',
                item: '.ugc-feed-view__items .ugc-collapsable-view',
                organization: '.ugc-feed-view__items .ugc-collapsable-view:first-child',
                lastItem: '.ugc-feed-view__items .ugc-collapsable-view:last-child'
            },
            objectAssignment: {
                view: '.ugc-object-assignment-form-view',
                content: '.ugc-object-assignment-form-view__content',
                footer: '.feedback-footer-container-view',
                buttons: {
                    first: '.feedback-footer-container-view .button:first-child',
                    second: '.feedback-footer-container-view .button:nth-child(2)',
                    third: '.feedback-footer-container-view .button:nth-child(3)'
                }
            },
            entrancesAssignment: {
                desktop: {
                    lastNameInput: '.feedback-edit-entrances-view__name:last-child input'
                },
                mobile: {
                    entranceCard: '.ugc-entrances-assignment-form-view__entrance',
                    secondEntranceCard:
                        '.carousel__item + .carousel__item .ugc-entrances-assignment-form-view__entrance',
                    inputs: {
                        name: '.ugc-entrances-assignment-form-view__entrance-inputs input',
                        apartmentRange:
                            '.ugc-entrances-assignment-form-view__entrance-inputs .feedback-field-view._name_apartments-range input'
                    },
                    buttons: {
                        saveEntrance: '.ugc-entrances-assignment-form-view__submit-button button',
                        savePosition: '.feedback-footer-container-view button._view_primary:not(._disabled)',
                        editPosition: '.ugc-entrances-assignment-form-view__link-button',
                        deleteEntrance:
                            '.ugc-entrances-assignment-form-view__link-button + .ugc-entrances-assignment-form-view__link-button'
                    },
                    placemark: {
                        secondEntrance: '.ymaps3x0--marker-layer>ymaps+ymaps .ugc-placemark-content-view._kind_entrance'
                    },
                    carousel: '.carousel__scrollable',
                    completedEntrance: {
                        message: '.ugc-entrances-assignment-form-view__complete-message',
                        nextButton: '.ugc-entrances-assignment-form-view__next-button button'
                    }
                }
            },
            organizationAssignment: {
                form: {
                    view: '.ugc-organization-assignment-form-view'
                }
            },
            common: {
                inputs: {
                    street: '.feedback-field-view._name_road input',
                    house: '.feedback-field-view._name_house input',
                    name: '.feedback-field-view._name_name input',
                    comment: {
                        input: '.feedback-field-view._name_comment input',
                        textarea: '.feedback-field-view._name_comment textarea'
                    },
                    photos: '.feedback-field-view._name_photos input[type="file"]'
                },
                buttons: {
                    firstButton: {
                        enabled: '.feedback-footer-container-view .button:first-child:not(._disabled)',
                        disabled: '.feedback-footer-container-view .button._disabled:first-child'
                    },
                    secondButton: '.feedback-footer-container-view .button:nth-child(2)'
                }
            }
        },
        photos: {
            tab: {
                buttonActive: '.ugc-photos-view__tab .button._view_accent'
            },
            feed: {
                item: '.ugc-map-photos-group-view',
                view: '.ugc-feed-view__items',
                group: {
                    view: '.ugc-photos-group-view',
                    photo: '.ugc-photos-group-view__photo',
                    button: '.ugc-photos-group-view__button',
                    control: '.ugc-photo-group-header__control'
                },
                photo: '.ugc-map-photo-view',
                photoOptions: '.ugc-map-photo-view__control',
                photoFirstOptions: '.ugc-map-photo-view:nth-child(1) .ugc-map-photo-view__control',
                photoSecondOptions: '.ugc-map-photo-view:nth-child(2) .ugc-map-photo-view__control',
                photoThirdOptions: '.ugc-map-photo-view:nth-child(3) .ugc-map-photo-view__control',
                photoLink: '.ugc-map-photo-view__link',
                photoDate: '.ugc-map-photo-view__date',
                authorshipSwitchControl: {
                    on: '.options-control__content .switch-control._selected',
                    off: '.options-control__content .switch-control:not(._selected)'
                },
                spinner: '.ugc-spinner-view',
                info: {
                    photo: '.ugc-photo-info-view__photo',
                    control: '.ugc-photo-info-view__control'
                }
            },
            player: '.photos-player-view',
            editor: {
                view: '.ugc-photo-edit-view',
                saveButton: '.ugc-photo-edit-view__button:first-child .button:not(._disabled)'
            }
        }
    },
    commentatorEntry: {
        expand: '.commentator-entry-view__expand'
    },
    cardRelatedProducts: {
        view: '.card-related-products-view',
        title: '.card-related-products-view__title',
        carousel: '.card-related-products-view__top-items',
        transitionItem: '.card-related-products-view__transition',
        nextButton: '.card-related-products-view .carousel__arrow-wrapper:not(._prev) .carousel__arrow',
        prevButton: '.card-related-products-view .carousel__arrow-wrapper._prev .carousel__arrow',
        relatedProduct: '.card-related-products-view .related-product-view',
        imageWithAlt:
            '.card-related-products-view .related-product-view img[alt="Салат Цезарь с курицей, фото — Знатный дворъ (Россия, Рязань, улица Дзержинского, 65)"]',
        singleItem: '.related-item-photo-view._size_small._clickable'
    },
    tabs: {
        container: '.tabs-select-view',
        titles: '.tabs-select-view__titles',
        counter: '.tabs-select-view__counter',
        overviewTabTitle: '.tabs-select-view__title._name_overview',
        menuTabTitle: '.tabs-select-view__title._name_menu',
        pricesTabTitle: '.tabs-select-view__title._name_prices',
        photosTabTitle: '.tabs-select-view__title._name_gallery',
        placesTabTitle: '.tabs-select-view__title._name_inside',
        reviewsTabTitle: '.tabs-select-view__title._name_reviews',
        selectedReviewsTab: '.tabs-select-view__title._name_reviews._selected',
        edadealTabTitle: '.tabs-select-view__title._name_edadeal',
        bookingTabTitle: '.tabs-select-view__title._name_hotels-booking',
        relatedTabTitle: '.tabs-select-view__title._name_related',
        postsTabTitle: '.tabs-select-view__title._name_posts',
        realtyTabTitle: '.tabs-select-view__title._name_realty',
        servicesTabTitle: '.tabs-select-view__title._name_services',
        attractionsTabTitle: '.tabs-select-view__title._name_attractions',
        featuresTabTitle: '.tabs-select-view__title._name_features',
        infrastructureTabTitle: '.tabs-select-view__title._name_infrastructure',
        eventsTabTitle: '.tabs-select-view__title._name_events',
        timetableTabTitle: '.tabs-select-view__title._name_timetable',
        chainTabTitle: '.tabs-select-view__title._name_chain',
        visibleTab: '.business-tab-wrapper._materialized',
        visibleNthTabContent: '.business-tab-wrapper:nth-child(%i)._materialized'
    },
    posts: {
        card: {
            post: {
                view: '.business-post-card-view',
                content: '.business-post-card-view__content'
            },
            posts: {
                view: '.business-posts-card-view',
                item: '.business-posts-card-view__item',
                seeAllLink: '.business-posts-card-view__see-all',
                nextButton: '.business-posts-card-view .carousel__arrow-wrapper:not(._prev) .carousel__arrow'
            }
        },
        posts: {
            view: '.business-posts-list',
            moderationBanner: '.business-posts-list__moderation-banner',
            post: {
                view: '.business-posts-list-post-view',
                published: '.business-posts-list-post-view:not(._unpublished)',
                unpublished: '.business-posts-list-post-view._unpublished',
                photos: '.business-posts-list-post-view__photos',
                text: '.business-posts-list-post-view__text',
                readMore: '.business-posts-list-post-view__read-more',
                moderataionStatus: '.business-posts-list-post-view__moderation-status'
            },
            firstPost: '.business-posts-list-post-view:nth-child(2)',
            fourthPost: '.business-posts-list-post-view:nth-child(5)'
        },
        post: {
            view: '.business-post-view',
            published: '.business-post-view:not(._unpublished)',
            unpublished: '.business-post-view._unpublished',
            rejectionReasons: '.moderation-info__banner .notification-banner',
            title: '.business-post-view__title',
            backButton: '.business-post-view__back',
            header: '.business-post-view__title',
            actions: '.business-post-view__actions'
        },
        carousel: {
            view: '.business-post-carousel-view'
        },
        photo: {
            view: '.business-post-photo-view'
        },
        editor: {
            entryButtons: {
                edit: '.post-editor-entry-buttons__button:first-child .button',
                delete: '.post-editor-entry-buttons__button:nth-child(2) .button'
            },
            form: {
                moderationStatus: '.post-editor-form-view__moderation-status',
                textArea: '.textarea__control'
            }
        }
    },
    common: {
        businessesChooser: {
            title: '.businesses-chooser__title',
            checkedItem: '.businesses-chooser__item._checked',
            nthBusinessCheckbox: '.businesses-chooser__item:nth-child(%s) .checkbox',
            allBusinessCheckbox: '.businesses-chooser__item:nth-child(2) .checkbox',
            firstBusinessCheckbox: '.businesses-chooser__item:nth-child(3) .checkbox',
            secondBusinessCheckbox: '.businesses-chooser__item:nth-child(4) .checkbox'
        },
        button: {
            primary: '.button._view_primary',
            secondaryBlue: '.button._view_secondary-blue',
            delete: '.delete-button',
            active: '.button:not(._disabled)'
        },
        statusScreen: '.status-screen',
        finalStatusScreenLink: '.final-status-screen__link',
        overlay: '.overlay',
        topOverlay: '.overlay:last-child',
        panelOverlay: '.panel-overlay',
        shutter: '.shutter',
        topShutter: '.shutter:last-child',
        formBlock: '.form-block',
        fileInput: 'input[type="file"]',
        draggableView: '.draggable-view',
        confirmation: {
            view: '.popup-confirm-view',
            submitButton: '.popup-confirm-view .button:nth-child(1)',
            cancelButton: '.popup-confirm-view .button:nth-child(2)',
            secondButton: '.popup-confirm-view__second-button'
        },
        tags: {
            tagsStatic: '.tags-view__static',
            tagsCarousel: '.tags-view__carousel',
            tagsCarouselVisible: '.tags-view__carousel._visible',
            searchButton: '.tags-view__tag-text._code_search',
            tag: '.tags-view__tag',
            nthCheckedCarouselTag:
                '.tags-view__carousel .carousel__item:nth-child(%i) .tags-view__tag .button._checked',

            lastStaticTag: '.tags-view__static .tags-view__tag:last-child'
        }
    },
    confirm: {
        delete: '.confirm-delete',
        exit: '.confirm-exit'
    },
    stories: {
        view: '.stories-view',
        item: '.stories-view__item',
        itemSelected: '.stories-view__item._selected',
        nthItem: '.stories-view__item:nth-child(%i)',
        selectedNthItem: '.stories-view__item._selected:nth-child(%i)',
        mobileSelectedItem: '.stories-view .Cube__Pane[aria-hidden=false]',
        close: '.stories-view__close',
        player: '.stories-view__player',
        header: '.stories-view__header',
        storiesEditorContainer: '.stories-editor-layout-component__container',
        story: {
            view: '.story-view',
            close: '.story-view__close',
            pause: '.story-view__pause',
            more: '.story-view__more',
            shareHintContent: '.popup__content',
            desktopMoreContent: '.popup__content .story-view__more-content',
            mobileMoreContent: '.shutter .story-view__more-content',
            title: '.story-view__title',
            isPlaying: '.story-view__pause:not(._paused)',
            isPaused: '.story-view__pause._paused',
            progressBar: '.story-view__progress .segmented-progress-bar__progress'
        },
        screens: {
            view: '.story-screens'
        },
        screen: {
            arrowPrevious: '.story-screen-view__previous',
            arrowNext: '.story-screen-view__next',
            content: '.story-screen-view__content',
            loader: '.story-screen-view__loader'
        },
        screenEditor: {
            view: '.story-screen-editor',
            carousel: '.story-screen-editor__screens > .carousel',
            image: '.story-screen-editor__image',
            prevSlideButton: '.story-screen-editor__prev',
            nextSlideButton: '.story-screen-editor__next',
            screens: {
                nthButton:
                    '.story-screen-editor__screens > .carousel .carousel__item:nth-child(%n) .story-cover-preview'
            },
            removeScreenButton: '.story-screen-editor__remove',
            closeButtonMobile: '.story-screen-editor__close',
            closeButtonDesktop: '.story-screen-editor .close-button',
            ok: '.story-screen-editor__button._type_ok',
            cancel: '.story-screen-editor__button._type_cancel',
            button: {
                nameInput: '.form-block:nth-child(1) input',
                linkInput: '.form-block:nth-child(2) input'
            },
            buttons: {
                add: '.story-screen-editor__button._type_add button',
                edit: '.story-screen-editor__button._type_edit button'
            },
            addPhoto: '.story-screen-editor__add-photo'
        },
        infoEditor: {
            view: '.story-info-editor',
            screenPreview: '.form-block > .carousel .story-cover-preview',
            coverPreview: '.story-cover-preview:not(._clickable) .story-cover-preview__inner',
            carousel: {
                view: '.story-info-editor__carousel',
                item: '.story-info-editor__carousel .carousel__item',
                nthItem: '.story-info-editor__carousel .carousel__item:nth-child(%i)'
            },
            link: '.story-info-editor__branches-link',
            titleInput: '.story-info-editor__name input',
            branches: '.story-info-editor__branches',
            branch: '.story-info-editor__branches-item',
            block: '.story-info-editor__block',
            ok: '.story-info-editor__button._type_ok',
            delete: '.story-info-editor__delete'
        },
        moderation: {
            view: '.moderation-info',
            expandButon: '.moderation-info__expand-button'
        },
        coverPreview: {
            view: '.story-cover-preview',
            inner: '.story-cover-preview__inner',
            clickable: '.story-cover-preview._clickable',
            faded: '.story-cover-preview._faded',
            selected: '.story-cover-preview._selected'
        },
        orderSnippet: {
            view: '.story-order-snippet',
            title: '.story-order-snippet__title',
            draggableHandler: '.story-order-snippet__wrap-handler',
            deleteButton: '.story-order-snippet__delete-icon'
        },
        editStories: {
            view: '.stories-order-editor__content',
            moderationNotification: '.stories-order-editor__rejected-notification'
        },
        screenOrderEditor: {
            view: '.story-screen-order-editor',
            ok: '.story-screen-order-editor__button._type_ok',
            cancel: '.story-screen-order-editor__button._type_cancel'
        },
        buttonEditor: {
            view: '.story-button-editor',
            nameInput: '.story-button-editor__text input',
            nameInputHint: '.story-button-editor__text .input-with-error__error',
            urlInput: '.story-button-editor__url input',
            urlInputHint: '.story-button-editor__url .input-with-error__error',
            ok: '.story-button-editor__button._type_ok',
            cancel: '.story-button-editor__button._type_cancel',
            delete: '.story-button-editor__delete'
        },
        coverEditor: {
            view: '.story-cover-editor',
            cover: '.story-cover-editor__current .story-cover-preview__inner',
            errorPopup: '.story-cover-editor__error',
            ok: '.story-cover-editor__button._type_ok',
            cancel: '.story-cover-editor__button._type_cancel',
            carousel: {
                nthItem: '.story-cover-editor .carousel__item:nth-child(%i)'
            }
        },
        storyEditor: {
            view: '.story-editor',
            errorPopup: '.story-editor__error'
        },
        authorizedBusinessesLoader: {
            view: '.authorized-businesses-loader'
        }
    },
    mrc: {
        layer: 'img[src*="l=mrc"]',
        modeSwitcher: '.mode-switcher-view',
        feedbackButton: '.mrc-feedback-control',
        control: {
            view: '.mrc-control',
            unchecked: '.mrc-control .button:not(._checked)',
            checked: '.mrc-control .button._checked',
            disabled: '.mrc-control .button._disabled'
        },
        emptyPreview: '.mrc-empty-preview',
        emptyBalloon: '.mrc-empty-preview-balloon',
        placemark: '.mrc-map-icon-view',
        secondaryPlacemark: '.mrc-map-icon-view._type_additional',
        balloon: '.mrc-preview-balloon__balloon',
        miniplayer: {
            view: '.mrc-mini-player-view',
            play: '.mrc-mini-player-view .mrc-mini-player-view__play',
            close: '.mrc-mini-player-view .mrc-mini-player-view__close',
            fullscreen: '.mrc-mini-player-view .mrc-mini-player-view__fullscreen'
        },
        player: {
            view: '.mrc-player',
            authorView: '.author-view',
            mini: '.mrc-mini-player-view',
            picture: '.mrc-picture-view._ready',
            image: {
                view: '.mrc-player-image-view',
                prev: '.mrc-player-image-view[data-type=prev]',
                current: '.mrc-player-image-view[data-type=current]',
                next: '.mrc-player-image-view[data-type=next]'
            },
            playback: '.mrc-playback-control-view',
            playbackControl: '.mrc-playback-control-view__control',
            minimap: {
                view: '.mrc-minimap-view'
            },
            similar: {
                view: '.mrc-similar-view'
            },
            directionReverse: '.mrc-player__direction-button'
        }
    },
    interstitial: {
        view: '.interstitial-view',
        closeButton: '.interstitial-view .close-button'
    },
    mobileAppBanner: {
        view: '.mobile-app-banner-view'
    },
    distribution: {
        desktopBlock: '#Y-A-290757-1',
        mobileBlock: '#Y-A-666603-1'
    },
    advertMock: {
        view: '[data-advert-id]',
        closeButton: '[data-advert-id] [data-close]'
    },
    contextMenu: '.context-menu-component',
    nmapControl: '.map-nmap-control',
    panoramaMinimap: {
        view: '.panorama-minimap-view',
        expandedView: '.panorama-minimap-view._expanded',
        splitControl: '.panorama-minimap-view__split-view-control',
        markerView: '.panorama-minimap-marker-view',
        fovInMarker: '.field-of-view__fov'
    },
    actionBar: {
        prefix: {
            collapsedCard: '.collapsed-content-view',
            businessCard: '.business-card-view',
            toponymCard: '.toponym-card-view',
            masstransitStop: '.masstransit-stop-panel-view'
        },
        view: '.card-actions-view',
        routeButton: '.action-button-view._type_route',
        taxiButton: '.action-button-view._type_taxi .button',
        undergroundButton: '.action-button-view._type_underground',
        callPhoneButton: '.action-button-view._type_call-phone',
        callPhoneButtonLink: '.action-button-view._type_call-phone a',
        showPhoneButton: '.action-button-view._type_show-phone',
        showWebButton: '.action-button-view._type_show-web',
        webButton: '.action-button-view._type_web',
        webButtonLink: '.action-button-view._type_web a',
        bookmarkButton: '.action-button-view._type_bookmark',
        bookmarkButtonNotSaved: '.action-button-view._type_bookmark:not(._saved)',
        bookmarkButtonChecked: '.action-button-view._type_bookmark._saved',
        shareButton: '.action-button-view._type_share',
        sendToPhoneButton: '.action-button-view._type_send-to-phone',
        actionButton: '.action-button-view._type_action_button',
        actionButtonLink: '.action-button-view._type_action_button a',
        reviewButton: '.action-button-view._type_review',
        bookingButton: '.action-button-view._type_booking'
    },
    langSwitcher: {
        view: '.lang-switcher-view',
        languages: '.lang-switcher-view__languages',
        russian: '.lang-switcher-view__language:first-child',
        russianChecked: '.lang-switcher-view__language:first-child .checkbox._checked'
    },
    homeTabs: {
        view: '.home-tabs-view',
        item: '.home-tabs-view__item'
    },
    dialog: {
        view: '.dialog',
        title: '.dialog__title',
        content: '.dialog__content',
        closeButton: '.dialog .close-button._circle',
        mobileCloseButton: '.body > .close-button._color_black._circle'
    },
    iframeDialog: {
        view: '.iframe-dialog',
        frame: '.iframe-dialog__frame'
    },
    cardShare: {
        view: '.card-dropdown-view .card-share-view',
        mobileView: '.shutter .card-share-view',
        sendToPhone: '.card-push-view__service',
        coordinatesDropdownToggle: '.card-share-view .card-feature-view__additional',
        shortLinkClipboard: '.card-share-view__short-link .clipboard',
        shortLinkText: '.card-share-view__short-link .card-share-view__text',
        coordinatesClipboard: '.card-share-view__coordinates .clipboard'
    },
    shortcutsCarousel: {
        view: '.shortcuts-carousel',
        bottom: '.map-controls__bottom-extension-slot .shortcuts-carousel',
        item: {
            view: '.shortcuts-carousel__item',
            second: '.shortcuts-carousel .carousel__item:nth-child(2)',
            last: '.shortcuts-carousel .carousel__item:last-child',
            promo: '.shortcuts-carousel__item._id_branding_yandex-gasstation',
            bookmark: {
                home: '.shortcuts-carousel__item._id_bookmark_home',
                work: '.shortcuts-carousel__item._id_bookmark_work'
            },
            bookmarkToggle: '.shortcuts-carousel__item._id_masstransit-bookmarks',
            masstransitFilterLayer: '.shortcuts-carousel__item._id_masstransit-layer',
            catalog: '.shortcuts-carousel__item._id_catalog'
        }
    },
    covid: {
        panel: {
            view: '.covid-panel-view:not(._collapsed)',
            collapsed: '.covid-panel-view._collapsed',
            statSwitcher: '.covid-panel-view:not(._collapsed) .mode-switcher-view._filled'
        },
        mapSwitcher: '.covid-map-view__switcher .mode-switcher-view._filled-air'
    },
    actual: {
        panel: {
            view: '.actual-panel-view',
            feed: '.actual-panel-view__feed',
            selectedStory:
                '.actual-panel-view:not(._collapsed) .actual-panel-view__section .carousel__item:nth-child(1)',
            links: '.actual-panel-view__links'
        }
    },
    popup: {
        view: '.popup',
        hintView: '.popup._type_help'
    },
    metro: {
        view: '.metro-input-form',
        reset: '.metro-input-form__reset',
        switch: '.metro-input-form__switch-icon-container',
        citySelectButton: '.metro-header-view__city-select',
        clickedSchemeLabel: '.scheme-objects-view__scheme-svg._click-through',
        schemeStation: '.scheme-objects-view__station',
        schemeStationClosest: '.scheme-objects-view__station._closest',
        schemeLabel: '.scheme-objects-view__label',
        schemeLabelClosed: '.scheme-objects-view__label._closed',
        schemeLabelWarning: '.scheme-objects-view__label._warning circle',
        schemeDecorations: '.scheme-objects-view__decorations._visible',
        snippetsCarousel: '.metro-route-snippets-carousel__routes-container',
        routeView: '.scheme-objects-view__scheme-svg:nth-child(3) g',
        firstInput: '.metro-input-form__stop-suggest._type_from input',
        secondInput: '.metro-input-form__stop-suggest._type_to input',
        secondInputFocused: '.metro-input-form__stop-suggest._type_to input._focused',
        fullScreenInput: '.full-screen-suggest input',
        suggestDropItem: '.metro-input-form__drop-item',
        activeSnippet: '.route-snippet-view._active._type_metro',
        details: {
            block: '.route-metro-details-step-view__metro-details',
            interval: '.route-metro-details-step-view__metro-minutes',
            routePartDuration: '.route-masstransit-step-view__details-info-left-content'
        },
        events: {
            view: '.route-metro-events-step-view'
        },
        eventBalloon: '.metro-event-baloon-view__content',
        footer: '.metro-footer-view',
        citySelect: {
            title: '.metro-city-select-view__title',
            close: '.metro-city-select-view .close-button',
            linkFirst: '.metro-city-select-view__link:first-of-type',
            linkSecond: '.metro-city-select-view__link:nth-child(3)',
            checkboxChecked: '.metro-city-select-view__city-block > .checkbox._checked'
        },
        stationDialogMenu: '.metro-dialog-view__container'
    },
    promoButton: {
        view: '.promo-button'
    },
    tooltip: {
        view: '.popup._type_tooltip',
        content: '.tooltip-content-view',
        title: '.tooltip-content-view__title'
    },
    events: {
        card: {
            share: '.event-card-view .card-share-view',
            shareButton: '.event-card-view .action-button-view._type_share .button',
            view: '.event-card-view',
            photo: '.event-photos-view__photo',
            link: '.event-card-view .action-button-view._type_link .button',
            title: '.event-card-view .card-title-view__title'
        },
        poi: {
            view: '.event-placemark__placemark',
            title: '.event-placemark__title-content',
            activeTitleBalloon: '.search-placemark-title',
            activeTitle: '.search-placemark-title__title',
            activeSubtitle: '.search-placemark-title__subtitle-item'
        }
    },
    weather: {
        link: '.weather-view._link'
    },
    mobileAppShare: {
        view: '.mobile-app-share-view',
        input: '.mobile-app-share-view__input:nth-child(3) .input .input__control',
        inputInvalid: '.mobile-app-share-view__input:nth-child(3) .input._invalid .input__control',
        captcha: '.mobile-app-share-view__captcha',
        captchaInput: '.mobile-app-share-view__captcha-wrapper .mobile-app-share-view__input .input__control',
        captchaError: '.mobile-app-share-view__captcha-error',
        sendButtonDisabled: '.mobile-app-share-view__send .button._disabled',
        sendButtonActive: '.mobile-app-share-view__send .button:not(._disabled)'
    },
    mapWidget: {
        app: '.map-widget-app',
        iframeWithApp: '.map-widget-iframe',
        controls: {
            tiltRotateControl: {
                view: '.map-widget-layout-view .map-tilt-rotate-control'
            },
            rulerControl: {
                view: '.map-widget-ruler-control-view',
                checked: '.map-widget-ruler-control-view ._checked'
            },
            trafficControl: {
                view: '.map-widget-traffic-control-view',
                text: '.map-widget-traffic-control-view .button__text'
            },
            inMaps: '.map-widget-layout-view__maps-links a',
            panoramaInMaps: '.map-widget-panorama-player-app__open-in-maps-button',
            searchControl: {
                view: '.map-widget-search-control-view',
                text: '.map-widget-search-control-view > button > span.button__text'
            },
            rightControls: {
                view: '.map-widget-layout-view__right-controls',
                top: '.map-widget-layout-view__right-controls._position_top'
            },
            zoomControl: {
                view: '.map-widget-layout-view .zoom-control'
            },
            geolocationControl: {
                view: '.map-widget-layout-view .map-geolocation-control'
            }
        },
        routes: {
            balloon: {
                view: '.map-widget-routes-content-view',
                closeButton: '.map-widget-content-view__balloon .close-button'
            }
        },
        usermaps: {
            content: '.map-widget-user-maps-content-view'
        },
        traffic: {
            roadEventBalloon: '.traffic-road-event-content-view'
        },
        content: {
            ready: '.map-widget-content-view__container._ready',
            balloon: {
                view: '.map-widget-content-view__balloon',
                close: '.map-widget-content-view__balloon .close-button'
            },
            panel: {
                view: '.map-widget-content-view__panel',
                close: '.map-widget-content-view__panel .close-button'
            },
            nearestMetroDistance: '.map-widget-nearest-stops-view__distance',
            masstransit: {
                view: '.map-widget-masstransit-stop-content-view'
            },
            workingIntervals: '.map-widget-business-working-intervals-view__intervals',
            common: {
                title: '.map-widget-content-title-view__title',
                metroDistance: '.map-widget-nearest-stops-view__distance',
                taxi: '.map-widget-taxi-link-view__link'
            },
            business: {
                view: '.map-widget-business-content-view',
                rating: '.map-widget-business-rating-view',
                workingIntervals: '.map-widget-business-working-intervals-view__time-label',
                feedback: '.map-widget-business-content-view__feedback-link',
                directionsButton: '.map-widget-business-buttons-view__button-container._directions',
                contacts: {
                    flip: '.map-widget-business-contacts-view__flip',
                    extraPhone: '.map-widget-business-contacts-view__phone'
                }
            },
            toponym: {
                view: '.map-widget-toponym-content-view',
                feedback: '.map-widget-toponym-content-view__feedback-link'
            }
        },
        search: {
            control: {
                view: '.map-widget-search-control-view',
                checked: '.map-widget-search-control-view._checked'
            },
            resultList: {
                balloon: {
                    view: '.map-widget-search-result-list-view__balloon-container',
                    close: '.map-widget-search-result-list-view__fullscreen-container .close-button'
                },
                fullscreen: {
                    view: '.map-widget-search-result-list-view__fullscreen-container',
                    close: '.map-widget-search-result-list-view__fullscreen-container .close-button'
                },
                item: '.map-widget-search-result-list-item',
                header: '.map-widget-search-result-list-view__header',
                clarify: '.map-widget-search-result-list-view__caption .map-widget-search-result-list-view__link',
                more: '.map-widget-search-result-list-view__more',
                loading: '.map-widget-search-result-list-view__spinner-container'
            }
        },
        panorama: {
            fullscreenControl: '.map-widget-panorama-player-app__fullscreen-control',
            openInMaps: '.map-widget-panorama-player-app__open-in-maps-button',
            copyrights: '.map-widget-panorama-player-app__copyrights'
        }
    },
    somethingWrongView: '.something-wrong-view',
    socialShare: {
        vk: '.social-share-view__link._service_vkontakte',
        twitter: '.social-share-view__link._service_twitter',
        facebook: '.social-share-view__link._service_facebook',
        ok: '.social-share-view__link._service_ok'
    },
    businessDialogView: {
        content: '.business-dialog-view__content'
    },
    indoor: {
        control: {
            view: '.indoor-levels-control',
            wrapper: '.indoor-levels-control__wrapper',
            level: '.indoor-levels-control__level',
            activeLevel: '.indoor-levels-control__level._active',
            arrow: '.indoor-levels-control__arrow'
        }
    },
    userProfile: {
        mainUser: {
            view: '.main-user-view',
            settings: '.main-user-view__settings',
            name: '.main-user-view__name'
        },
        ugc: {
            tabs: {
                container: '.tabs-select-view__titles-container',
                impressionsTabTitleSelected: '.tabs-select-view__title._name_impressions._selected',
                assignmentsTabTitle: '.tabs-select-view__title._name_assignments',
                mrcTabTitle: '.tabs-select-view__title._name_mrc',
                reviewsTabTitle: '.tabs-select-view__title._name_reviews',
                photosTabTitle: '.tabs-select-view__title._name_photos',
                panoramsTabTitle: '.tabs-select-view__title._name_panoramas',
                feedbackTabTitle: '.tabs-select-view__title._name_feedback'
            },
            feed: '.ugc-feed-view'
        }
    },
    scaleLine: {
        view: '.map-scale-line',
        label: '.map-scale-line__label'
    },
    listItem: {
        view: '.list-item-view'
    },
    actionsMenu: {
        view: '.shutter'
    },
    addAction: {
        view: '.add-action-view',
        content: '.add-action-view__content',
        details: '.add-action-view__button',
        children: '.add-action-view__children'
    },
    companyLogo: {
        view: '.company-logo-view'
    },
    businessStories: {
        view: '.business-stories-view',
        preview: '.story-preview',
        previewLast: '.business-stories-view__carousel .carousel__item:last-of-type .story-preview',
        previewNth: '.business-stories-view__carousel .carousel__item:nth-child(%i) .story-preview',
        addStory: '.business-stories-view__add-story',
        spinner: '.business-stories-view__spinner',
        skeleton: '.business-stories-view__skeleton',
        edit: '.business-stories-view__link'
    },
    cardEntranceStory: {
        view: '.card-entrance-story-view',
        button: '.card-entrance-story-view__button'
    },
    cardBanner: {
        view: '.card-banner-view'
    },
    businessEntranceStory: {
        button: '.business-entrance-story-view__button'
    },
    businessFullItems: {
        view: '.business-full-items-grouped-view',
        title: '.business-full-items-grouped-view__title',
        category: '.business-full-items-grouped-view__category'
    },
    genericMapFeature: {
        marker: '.generic-map-feature._marker'
    },
    ingrad: {
        card: {
            tabContent: '.business-card-view__infrastructure',
            info: {
                view: '.infrastructure-info-view'
            },
            categories: {
                view: '.infrastructure-categories-view',
                item: '.infrastructure-categories-view__item',
                selectedItem: '.infrastructure-categories-view__item._selected'
            }
        }
    },
    meta: {
        ogImage: 'meta[property="og:image"]'
    },
    mapsBooking: {
        unavailableView: '.maps-booking-unavailable-view',
        errorView: '.maps-booking-error-view',
        mainView: '.maps-booking-view',
        header: '.maps-booking-header-view',
        closeButton: '.maps-booking-header-view .close-button',
        backButton: '.maps-booking-header-view .back-button',
        summaryPanel: '.maps-booking-summary-panel-view',
        fixedSummaryPanel: '.sticky-wrapper > .maps-booking-summary-panel-view',
        summaryPanelButton: '.maps-booking-summary-panel-view__button',
        summaryPanelButtonDisabled: '.maps-booking-summary-panel-view__button button._disabled',
        summaryPanelCount: '.maps-booking-summary-panel-view__count',
        summaryPanelPrice: '.maps-booking-summary-panel-view__price',
        partnerDisclaimer: '.maps-booking-partner-view',
        inlineDialog: '.maps-booking-view .maps-booking-dialog',
        webview: '.webview-maps-booking-component',
        dialog: '.maps-booking-dialog',
        dialogCloseButton: '.maps-booking-dialog__children .button',
        dialogTitle: '.maps-booking-dialog__title',
        dialogSubtitle: '.maps-booking-dialog__subtitle',
        dialogButton: '.maps-booking-action-dialog-view__button',
        services: {
            view: '.maps-booking-services-view',
            categories: '.maps-booking-services-view__categories',
            category: '.maps-booking-services-view__category',
            nthCategory: '.maps-booking-services-view__category:nth-child(%i)',
            categoriesButton: '.maps-booking-services-search-header-view__category-button-content',
            categoryListing: '.maps-booking-category-view__listing',
            snippet: '.maps-booking-services-view__snippet-container',
            serviceBlock: '.maps-booking-service-view',
            nthServiceBlock: '.maps-booking-service-view:nth-child(%i)',
            serviceDescription: '.maps-booking-service-view__description',
            checkbox: '.maps-booking-service-view__checkbox',
            tagsCarouselVisible: '.maps-booking-tags-view__carousel._visible',
            nthServiceSelected: '.maps-booking-service-view:nth-child(%i)._selected',
            nthServiceDisabled: '.maps-booking-service-view:nth-child(%i)._disabled',
            tag: '.maps-booking-tags-view__tag',
            firstCategoryServiceBlocks: '.maps-booking-services-view__category:first-child .maps-booking-service-view',
            moreButton: '.maps-booking-service-view__read-more',
            errorContainer: '.maps-booking-services-view > .maps-booking-error-view',

            secondCategory: '.maps-booking-services-view__category:nth-child(2)',
            firstCheckboxInSecondCategory:
                '.maps-booking-services-view__category:nth-child(2) .maps-booking-service-view:first-child .maps-booking-service-view__checkbox',
            secondCheckboxInSecondCategory:
                '.maps-booking-services-view__category:nth-child(2) .maps-booking-service-view:nth-child(2) .maps-booking-service-view__checkbox',

            secondCheckedCarouselTag:
                '.maps-booking-tags-view__carousel .carousel__item:nth-child(2) .maps-booking-tags-view__tag .button._checked',

            lastStaticTag: '.maps-booking-tags-view__static .maps-booking-tags-view__tag:last-child',

            searchView: '.maps-booking-services-view__categories._search',
            searchResults: '.maps-booking-services-search-view__results-container',
            searchViewCategory: '.maps-booking-services-search-view__category',
            servicesViewInput: '.maps-booking-services-search-header-view__input input[type="text"]',
            searchViewInput: '.maps-booking-services-search-header-view input[type="text"]',
            searchViewCancelButton: '.maps-booking-services-search-header-view__button',
            badge: '.maps-booking-category-view__title-badge'
        },
        resources: {
            list: '.maps-booking-resources-view__list',
            view: '.maps-booking-resources-view',
            resourceBlock: '.maps-booking-resource-view',
            anyResource: '.maps-booking-resources-view__resource:first-child',
            nthResource: '.maps-booking-resources-view__resource:nth-child(%i)',
            nthResourceTitle: '.maps-booking-resources-view__resource:nth-child(%i) .maps-booking-resource-view__name',
            nthDetailsIcon:
                '.maps-booking-resources-view__resource:nth-child(%i) .maps-booking-resource-view__details-icon'
        },
        resource: {
            view: '.maps-booking-resource-details-view',
            reviewsView: '.maps-booking-reviews-view',
            resourceDetailsBlock: '.maps-booking-resource-details-view__resource',
            chooseResourceButton: '.maps-booking-resource-details-view__footer > button',
            footer: '.maps-booking-resource-details-view__footer',
            nthReview: '.maps-booking-reviews-view__review:nth-child(%i)',
            readMore: '.maps-booking-resource-details-view__read-more',
            headerTransparent: '.maps-booking-header-view > .sidebar-panel-header-view._transparent',
            header: '.maps-booking-header-view',
            previousTitle: '.maps-booking-resource-view__previous-title'
        },
        schedule: {
            view: '.maps-booking-schedule-view',
            tooltip: '.maps-booking-schedule-view__tooltip-content',
            switcher: '.maps-booking-schedule-view__switcher',
            switcherAllActive: '.mode-switcher-view__item._active._key_all',
            toast: '.maps-booking-schedule-view__toast',
            error: '.maps-booking-schedule-view__error'
        },
        slots: {
            container: '.maps-booking-slots-view__container',
            noSlotsButton: '.maps-booking-slots-view__no-booking-button',
            slots: '.maps-booking-slots-view__slots',
            slot: '.maps-booking-slots-view__slot',
            disabledSlot: '.maps-booking-slots-view__slot._disabled'
        },
        calendar: {
            rightArrow: '.maps-booking-calendar-input-view__arrow-container:not(._left):not(._disabled)',
            leftArrow: '.maps-booking-calendar-input-view__arrow-container._left:not(._disabled)',
            rightArrowDisabled: '.maps-booking-calendar-input-view__arrow-container._disabled:not(._left)',
            leftArrowDisabled: '.maps-booking-calendar-input-view__arrow-container._left._disabled',
            activeDay: '.maps-booking-calendar-input-view__day._active',
            previousFromActive: '.maps-booking-calendar-input-view__day._previous-from-active',
            notBookableDay: '.maps-booking-calendar-input-view__day._no-booking:not(._weekend):not(._active)',
            bookableDay: '.maps-booking-calendar-input-view__day:not(._active):not(._no-booking):not(._weekend)',
            weekendDayNotBookable: '.maps-booking-calendar-input-view__day._no-booking._weekend',
            weekendDayBookable: '.maps-booking-calendar-input-view__day._weekend:not(._no-booking)',
            calendarView: '.maps-booking-calendar-input-view'
        },
        checkout: {
            description: '.maps-booking-checkout-view__description',
            container: '.maps-booking-checkout-view__container',
            nameInput: 'label[for="booking_name"] input',
            phoneInput: 'label[for="booking_phone"] input',
            nameInputDisabled: 'label[for="booking_name"] .input._disabled',
            phoneInputDisabled: 'label[for="booking_phone"] .input._disabled',
            continueButton: '.maps-booking-checkout-view__passport-auth-button-continue',
            continueButtonDisabled: '.maps-booking-checkout-view__passport-auth-button-continue ._disabled',
            loginButton: '.maps-booking-checkout-view__passport-auth-button-login',
            passportAuth: '.maps-booking-checkout-view__passport-auth',
            namePhoneCommentBlock: '.maps-booking-checkout-view__name-phone-comment-block',
            editDate: '.maps-booking-checkout-view__edit-icon',
            editResource: '.maps-booking-resource-view__edit-icon',
            plusBlock: '.maps-booking-checkout-view__block._plus'
        },
        phoneVerificationDialog: {
            view: '.maps-booking-phone-verification-dialog',
            closeButton: '.maps-booking-view .dialog .close-button',
            input: '.maps-booking-phone-verification-dialog__input input',
            inputInvalid: '.maps-booking-phone-verification-dialog__input .input._invalid',
            countDown: '.maps-booking-phone-verification-dialog__countdown',
            sendAgainButton: '.maps-booking-phone-verification-dialog__button'
        },
        success: {
            container: '.maps-booking-success-view__container',
            profileButton: '.maps-booking-success-view__success-all-records',
            icon: '.maps-booking-success-view__icon',
            exitButton: '.maps-booking-success-view__buttons-exit',
            moreButton: '.maps-booking-success-view__buttons-more',
            plus: '.maps-booking-success-view__plus'
        },
        record: {
            view: '.maps-booking-record-view',
            company: '.maps-booking-record-view__company',
            feedback: '.maps-booking-feedback-input-view',
            datetimeTitle: '.maps-booking-record-view__datetime-title',
            datetime: '.maps-booking-record-view__datetime'
        },
        list: {
            view: '.maps-booking-list-view',
            empty: '.maps-booking-list-view__empty',
            emptyButton: '.maps-booking-list-view__empty-button',
            error: '.maps-booking-list-view__error',
            spinner: '.maps-booking-list-view__spinner',
            hintIcon: '.maps-booking-list-view__hint-icon',
            hint: '.maps-booking-list-view__hint'
        },
        snippet: {
            title: '.maps-booking-view .maps-booking-snippet-view__services',
            view: '.maps-booking-snippet-view',
            button: '.maps-booking-snippet-view__button',
            dialogView: '.maps-booking-view .maps-booking-snippet-view'
        },
        menu: {
            view: '.maps-booking-menu-view',
            card: '.maps-booking-menu-view__card',
            services: '.maps-booking-menu-view__card:first-child',
            resource: '.maps-booking-menu-view__card:last-child',
            snippet: '.maps-booking-menu-view__snippet',
            plusCard: '.maps-booking-menu-view__card._promo',
            plusAmount: '.maps-booking-menu-view__promo-title > span:first-child'
        }
    },
    parkingPayment: {
        view: '.parking-payment-card-selection-view',
        loading: '.parking-payment-card-selection-view__loading',
        notifiction: '.webview-notification-view',
        paymentButton: '.parking-payment-button-view',
        addCardButton: {
            view: '.parking-payment-card-selection-view__add-button',
            spinner: '.parking-payment-card-selection-view__add-button-spinner'
        },
        emptyState: '.parking-payment-card-selection-view__empty',
        paymentMethod: {
            mastercard: {
                view: '.parking-payment-method-view._type_MASTERCARD',
                selected: '.parking-payment-method-view._selected._type_MASTERCARD'
            },
            terminated: '.parking-payment-method-view._type_UNKNOWN'
        },
        close: '.parking-payment-card-selection-view .close-button'
    },
    fullScreenInput: {
        view: '.full-screen-input-wrapper',
        input: '.full-screen-input-wrapper input, .full-screen-input-wrapper textarea',
        button: '.full-screen-input-wrapper__button',
        closeButton: '.full-screen-input-wrapper .close-button'
    },
    transportSpb: {
        routeTag: '.transport-spb-route-tag-view',
        changeTitle: '.transport-spb-route-list-view__change-title',
        routeGroup: '.transport-spb-route-groups-view__group',
        routeGroupButton: '.transport-spb-route-groups-view__button',
        mainPage: {
            top: '.transport-spb-main-view__main',
            title: '.transport-spb-main-view__title',
            description: '.transport-spb-main-view__description',
            routeGroups: '.transport-spb-main-view__route-groups',
            footer: '.transport-spb-main-view__footer',
            button: '.transport-spb-main-view__button',
            company: '.transport-spb-main-view__company',
            copyright: '.transport-spb-main-view__copyright',
            contacts: '.transport-spb-main-view__contacts',
            routeList: '.transport-spb-route-list-view'
        },
        contacts: {
            view: '.transport-spb-contacts-view',
            link: '.transport-spb-contacts-view__link'
        },
        mapPage: {
            view: '.transport-spb-map-view',
            zoomControl: '.transport-spb-map-view__zoom-control',
            logo: '.transport-spb-map-view__logo',
            mobileLogo: '.transport-spb-content-view__logo',
            routeView: '.transport-spb-route-view',
            mapRoutePlacemark: '.transport-spb-map-route-view__placemark',
            suggestView: '.transport-spb-suggest-view',
            inputFocused: '.transport-spb-suggest-view__input .input._focused'
        }
    }
} as const;

export default selectors;
