import maps.garden.modules.geocoder_tester.lib.geocoder_index_tester as gt
from maps.garden.libs.search_data_validation.geocoder_validator.lib import tester
import logging


def create_versioned_geocoder(url):
    version = tester.Geocoder(url).request('geocode=москва').metadata().version
    return tester.VersionedGeocoder(url, version)


def main():
    logging.getLogger().setLevel('DEBUG')
    testing_geocoder = create_versioned_geocoder("http://addrs-testing.search.yandex.net/geocoder/testing_content/maps")
    stable_geocoder = tester.Geocoder("http://addrs-testing.search.yandex.net/geocoder/stable/maps")

    success_flag, report, short_report = gt.run_tester(testing_geocoder, stable_geocoder, "local_call")
    print(report)
