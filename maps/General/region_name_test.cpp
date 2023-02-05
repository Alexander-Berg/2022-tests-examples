#define BOOST_TEST_DYN_LINK
#define BOOST_TEST_MAIN

#include <yandex/maps/i18n-geobase/region_name.h>
#include <boost/test/unit_test.hpp>

#include <geobase3/lookup.hpp>

using namespace maps::i18n::geobase;
using namespace maps;

namespace {

const char* GEOBASE_PATH = "/var/cache/geobase/geodata3.bin";

maps::Locale ru_RU(maps::LANG_RU, maps::REGION_RU);
maps::Locale uk_UA(maps::LANG_UK, maps::REGION_UA);
maps::Locale tr_TR(maps::LANG_TR, maps::REGION_TR);
maps::Locale en_US(maps::LANG_EN, maps::REGION_US);

size_t moscow = 213;
size_t chelyabinsk = 56;
size_t kiev = 143;
size_t stambul = 11508;
}

BOOST_AUTO_TEST_CASE(TestRegionName)
{
    BOOST_CHECK_EQUAL(regionName(moscow, ru_RU), "Москва");
    BOOST_CHECK_EQUAL(regionName(moscow, uk_UA), "Москва");
    BOOST_CHECK_EQUAL(regionName(moscow, tr_TR), "Moskova");
    BOOST_CHECK_EQUAL(regionName(moscow, en_US), "Moscow");

    BOOST_CHECK_EQUAL(regionName(chelyabinsk, ru_RU), "Челябинск");
    BOOST_CHECK_EQUAL(regionName(chelyabinsk, uk_UA), "Челябінськ");
    BOOST_CHECK_EQUAL(regionName(chelyabinsk, tr_TR), "Çelyabinsk");
    BOOST_CHECK_EQUAL(regionName(chelyabinsk, en_US), "Chelyabinsk");

    BOOST_CHECK_EQUAL(regionName(kiev, ru_RU), "Киев");
    BOOST_CHECK_EQUAL(regionName(kiev, uk_UA), "Київ");
    BOOST_CHECK_EQUAL(regionName(kiev, tr_TR), "Kiev");
    BOOST_CHECK_EQUAL(regionName(kiev, en_US), "Kyiv");

    BOOST_CHECK_EQUAL(regionName(stambul, ru_RU), "Стамбул");
    BOOST_CHECK_EQUAL(regionName(stambul, uk_UA), "Стамбул");
    BOOST_CHECK_EQUAL(regionName(stambul, tr_TR), "İstanbul");
    BOOST_CHECK_EQUAL(regionName(stambul, en_US), "Istanbul");
}

BOOST_AUTO_TEST_CASE(TestLookup)
{
    geobase3::lookup lookup(GEOBASE_PATH);
    BOOST_CHECK_EQUAL(lookup.linguistics_for_region(moscow, "ru").nominative_case, "Москва");
    BOOST_CHECK_EQUAL(lookup.linguistics_for_region(moscow, "uk").nominative_case, "Москва");
    BOOST_CHECK_EQUAL(lookup.linguistics_for_region(moscow, "tr").nominative_case, "Moskova");
    BOOST_CHECK_EQUAL(lookup.linguistics_for_region(moscow, "en").nominative_case, "Moscow");

    BOOST_CHECK_EQUAL(lookup.linguistics_for_region(chelyabinsk, "ru").nominative_case, "Челябинск");
    BOOST_CHECK_EQUAL(lookup.linguistics_for_region(chelyabinsk, "uk").nominative_case, "Челябінськ");
    BOOST_CHECK_EQUAL(lookup.linguistics_for_region(chelyabinsk, "tr").nominative_case, "Çelyabinsk");
    BOOST_CHECK_EQUAL(lookup.linguistics_for_region(chelyabinsk, "en").nominative_case, "Chelyabinsk");

    BOOST_CHECK_EQUAL(lookup.linguistics_for_region(kiev, "ru").nominative_case, "Киев");
    BOOST_CHECK_EQUAL(lookup.linguistics_for_region(kiev, "uk").nominative_case, "Київ");
    BOOST_CHECK_EQUAL(lookup.linguistics_for_region(kiev, "tr").nominative_case, "Kiev");
    BOOST_CHECK_EQUAL(lookup.linguistics_for_region(kiev, "en").nominative_case, "Kyiv");

    BOOST_CHECK_EQUAL(lookup.linguistics_for_region(stambul, "ru").nominative_case, "Стамбул");
    BOOST_CHECK_EQUAL(lookup.linguistics_for_region(stambul, "uk").nominative_case, "Стамбул");
    BOOST_CHECK_EQUAL(lookup.linguistics_for_region(stambul, "tr").nominative_case, "İstanbul");
    BOOST_CHECK_EQUAL(lookup.linguistics_for_region(stambul, "en").nominative_case, "Istanbul");
}

BOOST_AUTO_TEST_CASE(TestUnsupportedLocale)
{
    BOOST_CHECK_EQUAL(regionName(kiev, maps::Locale(maps::LANG_BE, maps::REGION_BY)), "Киев");
    BOOST_CHECK_EQUAL(regionName(kiev, maps::Locale(maps::LANG_BE, maps::REGION_US)), "Киев");
    BOOST_CHECK_EQUAL(regionName(kiev, maps::Locale(maps::LANG_UK, maps::REGION_PL)), "Київ");
    BOOST_CHECK_EQUAL(regionName(kiev, maps::Locale(maps::LANG_EN, maps::REGION_GB)), "Kyiv");
    BOOST_CHECK_EQUAL(regionName(kiev, maps::Locale(maps::LANG_HE, maps::REGION_IL)), "Kyiv");
    BOOST_CHECK_EQUAL(regionName(kiev, maps::Locale(maps::LANG_RU, maps::REGION_US)), "Киев");
    BOOST_CHECK_EQUAL(regionName(kiev, boost::lexical_cast<maps::Locale>("no-Latn-NO-hognorsk_RU_LOCAL")), "Киев");
}
