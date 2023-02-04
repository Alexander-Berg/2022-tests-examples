// Файл генерится автоматически.
// Ручные изменения будут утеряны при запуске 'make update-analytic-regexps'
// Оригинал: https://a.yandex-team.ru/arc/trunk/arcadia/analytics/geo/maps/common/clicks_description.json

const ANALYTIC_NAMES = {
    route: {
        regexp: '.*\\.(preview_card|orgpage|collapsed_card|org_poi_preview|org_pin_preview)\\..*(build_route)$'
    },
    metroRoute: {
        regexp: '.*\\.(preview_card|orgpage)\\..*(metro_route)$'
    },
    masstransitRoute: {
        regexp: '.*\\.(preview_card|orgpage)\\..*(masstransit_route)$'
    },
    showPhone: {
        regexp: '.*\\.(preview_card|orgpage|collapsed_card|org_poi_preview|org_pin_preview)\\..*show_phone$'
    },
    callPhone: {
        regexp: '.*\\.(preview_card|orgpage|collapsed_card)\\..*call_phone$'
    },
    openPhotoCard: {
        regexp: '.*\\.(preview_card|orgpage|collapsed_card)\\..*photos$'
    },
    openPhotoSerp: {
        regexp: '.*serp_panel\\.(collapsed_)?results\\.result_item\\..*photo$'
    },
    openPhotoOrg: {
        regexp: '.*\\.(orgpage|org_poi_preview|org_pin_preview)\\..*(photo|photos)$'
    },
    openPhotoTab: {
        regexp: '.*\\.(preview_card|orgpage|collapsed_card)\\..*tab$',
        params: {
            tab: 'gallery'
        }
    },
    siteLink: {
        regexp: '.*\\.(preview_card|orgpage|collapsed_card|org_poi_preview|org_pin_preview)\\..*(site_link)$'
    },
    socialLink: {
        regexp: '.*\\.(preview_card|orgpage|collapsed_card|org_poi_preview|org_pin_preview)\\..*(social_link)$'
    },
    panorama: {
        regexp: '.*\\.(preview_card|orgpage|collapsed_card)\\..*(panorama)$'
    },
    sendUgc: {
        regexp: '.*\\..*(review_form\\.send_review)$'
    },
    parkingControl: {
        regexp: '.*\\.(preview_card|orgpage|collapsed_card)\\..*(parking_control)$'
    },
    getTaxi: {
        regexp: '.*\\.(preview_card|orgpage|collapsed_card)\\..*(get_taxi_button)$'
    },
    openUgc: {
        regexp: '.*\\.(preview_card|orgpage)\\..*reviews\\..*$'
    },
    openUgcTab: {
        regexp: '.*\\.(preview_card|orgpage|collapsed_card)\\..*tab$',
        params: {
            tab: 'reviews'
        }
    },
    workingStatus: {
        regexp: '.*\\.(preview_card|orgpage)\\..*working_status(\\.control)?$'
    },
    showEntrances: {
        regexp: '.*\\.(preview_card|orgpage)\\..*(show_entrances)$'
    },
    cta: {
        regexp: '.*\\.(preview_card|orgpage)\\..*(business_link|delivery|table|appointment|exhibits)$'
    },
    eda_takeaway: {
        regexp: '.*\\.(preview_card|orgpage|collapsed_card)\\..*(eda_takeaway_button)$'
    },
    bookmark: {
        regexp: '.*\\.(preview_card|orgpage|collapsed_card)\\..*(bookmark)$'
    },
    chain: {
        regexp: '.*\\.(preview_card|orgpage)\\..*(chain)$'
    },
    details: {
        regexp: '.*\\.(preview_card)\\.(more_info)$'
    },
    share: {
        regexp: '.*\\.(preview_card|orgpage|collapsed_card)\\..*(share|send_to_phone)$'
    },
    shareBlock: {
        regexp: '.*\\.preview_card\\.share\\..*(service|clipboard)$'
    },
    post: {
        regexp: '.*\\.(preview_card|orgpage)\\..*(business_post)$'
    },
    postTab: {
        regexp: '.*\\.(preview_card|orgpage|collapsed_card)\\..*tab$',
        params: {
            tab: 'posts'
        }
    },
    postsSeeAll: {
        regexp: '.*\\.(preview_card|orgpage|collapsed_card|org_poi_preview|org_pin_preview)\\..*(posts_see_all)$'
    },
    storyOpen: {
        regexp: '.*\\.(preview_card|orgpage|collapsed_card|org_poi_preview|org_pin_preview)\\..*(story)$'
    },
    rate: {
        regexp: '.*\\.(preview_card|orgpage|dialog)\\..*(rate_stars|review_form)\\.(star)$'
    },
    bookingLink: {
        regexp: '.*\\.(preview_card|orgpage).*booking(_snippet)?\\..*(partner_item.link)$'
    },
    bookingControl: {
        regexp: '.*\\.(preview_card|orgpage).*booking(_snippet)?\\.(calendar_button|persons_select)$'
    },
    bookingButton: {
        regexp: '.*\\.(serp_panel|org_poi_preview|org_pin_preview)\\..*(booking_button)$'
    },
    pricesButton: {
        regexp: '.*\\.(serp_panel|org_poi_preview|org_pin_preview)\\..*(prices_button)$'
    },
    pricesTab: {
        regexp: '.*\\.(preview_card|orgpage|collapsed_card)\\..*tab$',
        params: {
            tab: 'prices'
        }
    },
    menuItem: {
        regexp:
            '.*\\.(preview_card|orgpage)\\..*(popular|related_products|orgpage_related_items)\\..*(offer.img|offer.url|product_card|category)$'
    },
    menuLook: {
        regexp:
            '.*\\.(preview_card|orgpage)\\..*(popular|related_products|orgpage_related_items)\\..*(next|prev|show_menu)$'
    },
    menuButton: {
        regexp: '.*\\.(serp_panel|org_poi_preview|org_pin_preview)\\..*(menu_button)$'
    },
    bookingWidget: {
        regexp: '.*\\.(preview_card|orgpage)\\..*booking_widget\\..*$'
    },
    geoproductOpen: {
        regexp: '.*\\.(preview_card|orgpage|collapsed_card)\\..*(promotion|promotion.control)$'
    },
    geoproductLink: {
        regexp: '.*\\.(preview_card|orgpage|collapsed_card)\\..*(promotion_link)$'
    },
    geoproductCta: {
        regexp: '.*\\.(preview_card|orgpage|collapsed_card|org_poi_preview|org_pin_preview)\\..*(cta_button)$'
    },
    lastMileStory: {
        regexp: '.*\\.(preview_card|orgpage|placemark)\\..*(entrance_story)$'
    }
};

export {ANALYTIC_NAMES};
