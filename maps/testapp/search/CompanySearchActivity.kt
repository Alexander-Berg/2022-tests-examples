package com.yandex.maps.testapp.search

import com.yandex.mapkit.GeoObject
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.search.*
import com.yandex.runtime.KeyValuePair

open class CompanySearchActivity : CardSearchActivity() {
    override fun onStopImpl(){}
    override fun onStartImpl(){}

    override fun defaultSearchBoxText() = "1017052599"

    override fun createSession() = makeSession(getSearchOptions(this)
        .setSnippets(ALL_SNIPPETS)
        .setUserPosition(Point(55.7, 37.6))
    )

    private fun makeSession(searchOptions: SearchOptions): Session {
        return searchManager.resolveURI(
            "ymapsbm1://org?oid=$searchBoxText",
            searchOptions,
            searchListener)
    }

    override fun fillObjectCard(geoObject: GeoObject, searchResults: SectionedListView) {
        addBusinessInfo(geoObject, searchResults)
        addRating1xInfo(geoObject, searchResults)
        addMassTransitInfo(geoObject, searchResults)
        addPanoramasInfo(geoObject, searchResults)
        addPhotosInfo(geoObject, searchResults)
        addBusinessImagesInfo(geoObject, searchResults)
        addReferencesInfo(geoObject, searchResults)
        addFuelInfo(geoObject, searchResults)
        addExchangeInfo(geoObject, searchResults)
        addRouteDistancesInfo(geoObject, searchResults)
        addSubtitleInfo(geoObject, searchResults)
        addDirectInfo(geoObject, searchResults)
        addMetrikaInfo(geoObject, searchResults)
        addGoodsInfo(geoObject, searchResults)
        addGoods1xInfo(geoObject, searchResults)
        addShowtimesInfo(geoObject, searchResults)
        addRoutePoint(geoObject, searchResults)
    }

    private fun addGoodsInfo(geoObject: GeoObject,
                             searchResults: SectionedListView) {
        val metadata = geoObject.metadata<GoodsObjectMetadata>() ?: return
        searchResults.addSection("goods", metadata.goods.map { goodsItemWithDetails(it) })
    }

    private fun addGoods1xInfo(geoObject: GeoObject,
                             searchResults: SectionedListView) {
        val metadata = geoObject.metadata<Goods1xObjectMetadata>() ?: return
        searchResults.addSection("snippet | goods", metadata.goods.map { goodsItemWithDetails(it) })
    }

    private fun addDirectInfo(geoObject: GeoObject, searchResults: SectionedListView) {
        val metadata = geoObject.metadata<DirectObjectMetadata>() ?: return
        searchResults.addSection("direct | main",
            ItemWithDetails(metadata.title, "title"),
            ItemWithDetails(metadata.text, "text"),
            ItemWithDetails(metadata.extra, "extra"),
            ItemWithDetails(metadata.domain, "domain"),
            ItemWithDetails(metadata.url, "url")
        )
        searchResults.addSection("direct | disclaimers",
            metadata.disclaimers.map { ItemWithDetails(it, "") }
        )
        searchResults.addSection("direct | counters", metadata.counters.map {
            ItemWithDetails(it.url, it.type)
        })
        searchResults.addSection("direct | links", metadata.links.map {
            ItemWithDetails(it.href, it.type)
        })
        val contactInfo = metadata.contactInfo ?: return
        searchResults.addSection("direct | contact info",
            ItemWithDetails(contactInfo.companyName, "company name"),
            ItemWithDetails(contactInfo.address, "address"),
            ItemWithDetails(contactInfo.email, "email"),
            ItemWithDetails(contactInfo.hours, "address"),
            ItemWithDetails(contactInfo.phone, "phone")
        )
    }

    private fun addMetrikaInfo(geoObject: GeoObject, searchResults: SectionedListView) {
        val metadata = geoObject.metadata<MetrikaObjectMetadata>() ?: return
        searchResults.addSection("metrika",
            ItemWithDetails(metadata.counter, "counter")
        )
        searchResults.addSection("metrika | goals",
            ItemWithDetails(metadata.goals?.call, "call") {
                SearchFactory.getInstance().searchLogger().logGeoObjectCardAction(CardActionEvent.MAKE_PHONE_CALL, geoObject)
            },
            ItemWithDetails(metadata.goals?.route, "route") {
                SearchFactory.getInstance().searchLogger().logGeoObjectCardAction(CardActionEvent.MAKE_ROUTE, geoObject)
            },
            ItemWithDetails(metadata.goals?.cta, "cta") {
                SearchFactory.getInstance().searchLogger().logGeoObjectCardAdvertAction("", geoObject);
            }
        )
    }

    private fun addBusinessInfo(geoObject: GeoObject, searchResults: SectionedListView) {
        val metadata = geoObject.metadata<BusinessObjectMetadata>() ?: return

        searchResults.addSection("short name", metadata.shortName)
        searchResults.addSection("address", metadata.address.formattedAddress)
        searchResults.addSection("seoname", metadata.seoname)
        searchResults.addSection("indoor level", metadata.indoorLevel)
        addLinksInfo(metadata, searchResults)
        addCategoryInfo(metadata, searchResults)
        addPropertiesInfo(metadata, searchResults)
        addChainInfo(metadata, searchResults)
        metadata.workingHours?.let { searchResults.addSection("Working hours",
            ItemWithDetails(it.text, "text"),
            ItemWithDetails(it.state?.text, "state.text"),
            ItemWithDetails(it.state?.shortText, "state.shortText"),
            ItemWithDetails(it.state?.isOpenNow?.toString(), "state.isOpenNow"),
            ItemWithDetails(it.state?.tags?.joinToString(prefix="[", postfix="]"), "state.tags"))
        }
        addFeaturesInfo(metadata, searchResults)
        metadata.closed?.let { searchResults.addSection("closed", it.toString()) }
        metadata.unreliable?.let { searchResults.addSection("unreliable", "true") }
        addAdvertInfo(metadata, searchResults)

        val experimentalMetadata = geoObject.metadata<ExperimentalMetadata>() ?: return

        addExperimentalInfo(experimentalMetadata, searchResults)
    }

    private fun addPhotosInfo(geoObject: GeoObject, searchResults: SectionedListView) {
        val metadata = geoObject.metadata<BusinessPhotoObjectMetadata>() ?: return
        searchResults.addSection("snippet | photos", metadata.photos.map {
            ItemWithDetails("Photo Id: " + it.id, it.links.mapNotNull{it.type}.joinToString())
        })
    }

    private fun addBusinessImagesInfo(geoObject: GeoObject, searchResults: SectionedListView) {
        val metadata = geoObject.metadata<BusinessImagesObjectMetadata>() ?: return
        metadata.logo?.let {
            searchResults.addSection("snippet | business images",
                ItemWithDetails(it.urlTemplate, "logo"))
        }
    }

    private fun addPanoramasInfo(geoObject: GeoObject, searchResults: SectionedListView) {
        val metadata = geoObject.metadata<PanoramasObjectMetadata>() ?: return
        searchResults.addSection("snippet | panoramas", metadata.panoramas.map {
            ItemWithDetails("Panorama Id: " + it.id)
        })
    }

    private fun addMassTransitInfo(geoObject: GeoObject, searchResults: SectionedListView) {
        val metadata = geoObject.metadata<MassTransit1xObjectMetadata>() ?: return
        searchResults.addSection("snippet | stops", metadata.stops.map{
            ItemWithDetails(it.name, "Distance: " + it.distance.text + "; Line: "
                + (it.line?.name ?: "no data"))
        })
    }

    private fun addLinksInfo(metadata: BusinessObjectMetadata, searchResults: SectionedListView) {
        searchResults.addSection("links", metadata.links.map {
            ItemWithDetails(it.link.href, it.tag)
        })
    }

    private fun addCategoryInfo(metadata: BusinessObjectMetadata, searchResults: SectionedListView) {
        searchResults.addSection("categories", metadata.categories.map {
            ItemWithDetails(it.name, "class: " + (it.categoryClass ?: "") + "; tags: " + it.tags.joinToString())
        })
    }

    private fun addPropertiesInfo(metadata: BusinessObjectMetadata, searchResults: SectionedListView) {
        val items = metadata.properties?.items ?: return
        searchResults.addSection("properties", items.map {
            ItemWithDetails(it.key, it.value)
        })
    }

    private fun addChainInfo(metadata: BusinessObjectMetadata, searchResults: SectionedListView) {
        searchResults.addSection("chains", metadata.chains.map {
            ItemWithDetails(it.name, it.id)
        })
    }

    private fun addFeaturesInfo(metadata: BusinessObjectMetadata, searchResults: SectionedListView) {
        searchResults.addSection("features", metadata.features.map { feature ->
            ItemWithDetails(feature.name,
                    feature.value.enumValue?.let { enums -> enums.joinToString(transform = { it.name }) }
                            ?: feature.value.textValue?.let { it[0] }
                            ?: ""
            )
        })
    }

    private fun addAdvertInfo(metadata: BusinessObjectMetadata, searchResults: SectionedListView) {
        metadata.advertisement ?: return
        val items = ArrayList<ItemWithDetails>()

        fun addSimpleObject(obj: Any?, details: String) {
            obj ?: return
            items.add(ItemWithDetails(obj.toString(), details))
        }

        fun addImage(obj: AdvertImage?, details: String) {
            obj ?: return
            addSimpleObject(obj.url, "$details.url")
        }

        fun addPromo(obj: Advertisement.Promo?, details: String) {
            obj ?: return
            addSimpleObject(obj.title, "$details.title")
            addSimpleObject(obj.details, "$details.details")
            obj.disclaimers.map { addSimpleObject(it, "$details.disclaimer") }
            addSimpleObject(obj.url, "$details.url")
            addImage(obj.banner, "$details.banner")
            addSimpleObject(obj.fullDisclaimer, "$details.full_disclaimer")
        }

        fun addMoney(obj: com.yandex.mapkit.Money?, details: String) {
            obj ?: return
            addSimpleObject(obj.value, "$details.value")
            addSimpleObject(obj.text, "$details.text")
            addSimpleObject(obj.currency, "$details.currency")
        }

        fun addProduct(obj: Advertisement.Product?, details: String) {
            obj ?: return
            addSimpleObject(obj.title, "$details.title")
            addSimpleObject(obj.url, "$details.url")
            addImage(obj.photo, "$details.photo")
            addMoney(obj.price, "$details.price")
        }

        fun addTextData(obj: Advertisement.TextData?, details: String) {
            obj ?: return
            addSimpleObject(obj.title, "$details.title")
            addSimpleObject(obj.text, "$details.text")
            addSimpleObject(obj.url, "$details.url")
            obj.disclaimers.map { addSimpleObject(it, "$details.disclaimer") }
        }

        fun addKeyValuePair(obj: KeyValuePair?, details: String) {
            obj ?: return
            addSimpleObject(obj.key, "$details.key")
            addSimpleObject(obj.value, "$details.value")
        }

        fun addAction(obj: Action, details: String) {
            addSimpleObject(obj.type, "$details.type")
            obj.properties.map { addKeyValuePair(it, "$details.property") }
        }

        fun addAdvert(obj: Advertisement?, details: String) {
            obj ?: return
            addTextData(obj.textData, "$details.textData")
            addSimpleObject(obj.about, "$details.about")
            addImage(obj.logo, "$details.logo")
            addImage(obj.photo, "$details.photo")
            obj.actions.map { addAction(it, "$details.action") }
            addSimpleObject(obj.logId, "$details.log_id")
            addPromo(obj.promo, "$details.promo")
            obj.products.map { addProduct(it, "$details.product") }
            obj.properties.map { addKeyValuePair(it, "$details.property") }
        }

        addAdvert(metadata.advertisement, "advert")
        searchResults.addSection("advert", items)
    }

    private fun addRating1xInfo(geoObject: GeoObject, searchResults: SectionedListView) {
        val metadata = geoObject.metadata<BusinessRating1xObjectMetadata>() ?: return
        searchResults.addSection("snippet | rating_1x",
            ItemWithDetails(metadata.ratings.toString(), "number of ratings"),
            ItemWithDetails(metadata.reviews.toString(), "number of reviews"),
            ItemWithDetails(metadata.score?.toString(), "current rating"))
    }

    private fun addExperimentalInfo(metadata: ExperimentalMetadata, searchResults: SectionedListView) {
        searchResults.addSection("snippet | experimental", metadata.experimentalStorage!!.items.map {
            ItemWithDetails(it.key, it.value)
        })
    }

    private fun addReferencesInfo(geoObject: GeoObject, searchResults: SectionedListView) {
        val metadata = geoObject.metadata<ReferencesObjectMetadata>() ?: return
        searchResults.addSection("snippet | references", metadata.references.map {
            ItemWithDetails(it.id, it.scope)
        })
    }

    private fun addFuelInfo(geoObject: GeoObject, searchResults: SectionedListView) {
        val metadata = geoObject.metadata<FuelMetadata>() ?: return
        searchResults.addSection(
            "snippet | fuel | general",
            ItemWithDetails(metadata.timestamp?.toString(), "timestamp"),
            ItemWithDetails(metadata.attribution?.author?.name, "author"),
            ItemWithDetails(metadata.attribution?.link?.href, "link")
        )
        searchResults.addSection("snippet | fuel | fuels", metadata.fuels.map {
            ItemWithDetails(it.name, it.price?.text)
        })
    }

    private fun addExchangeInfo(geoObject: GeoObject, searchResults: SectionedListView) {
        val metadata = geoObject.metadata<CurrencyExchangeMetadata>() ?: return
        searchResults.addSection("snippet | exchange", metadata.currencies.map {
            ItemWithDetails(it.name, "buy: ${it.buy?.text ?: "null"}, sell: ${it.sell?.text ?: "null"}")
        })
    }

    private fun addRouteDistancesInfo(geoObject: GeoObject, searchResults: SectionedListView) {
        geoObject.metadata<RouteDistancesObjectMetadata>()?.absolute?.let {
            searchResults.addSection("snippet | distance/absolute", listOf(
                ItemWithDetails("driving time", it.driving?.duration?.text),
                ItemWithDetails("driving distance", it.driving?.distance?.text),
                ItemWithDetails("walking time", it.walking?.duration?.text),
                ItemWithDetails("walking distance", it.walking?.distance?.text),
                ItemWithDetails("transit time", it.transit?.duration?.text)
            ))
        }
    }

    private fun addSubtitleInfo(geoObject: GeoObject, searchResults: SectionedListView) {
        val metadata = geoObject.metadata<SubtitleMetadata>() ?: return
        searchResults.addSection("snippet | subtitle", metadata.subtitleItems.map {
            ItemWithDetails(
                    it.type + ": \"" + it.text + "\"",
                    it.properties.joinToString(transform = { it.key + ": " + it.value })
            )
        })
    }

    private fun addShowtimesInfo(geoObject: GeoObject, searchResults: SectionedListView) {
        val metadata = geoObject.metadata<ShowtimesObjectMetadata>() ?: return
        searchResults.addSection("snippet | showtimes",
            ItemWithDetails(
                metadata.title,
                metadata.showtimes.joinToString(transform = {
                    it.startTime.text + " – " + it.price?.text
                })
            )
        )
    }
}
