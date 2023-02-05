#include <yandex/maps/navikit/map/route/map_route_utils.h>

#include <boost/test/unit_test.hpp>

#include <memory>
#include <vector>

namespace yandex::maps::navikit::map::tests {

namespace {

const float TOLERANCE_PERCENT = 1.0f;

std::vector<double> accumulate(const std::vector<double>& v)
{
	std::vector<double> result;
	result.reserve(v.size());
	double sum = 0;

	for (const auto& elem: v) {
		sum += elem;
		result.push_back(sum);
	}

	return result;
}

void testSubpolylineLength(
	const std::vector<double>& lengths,
	const mapkit::geometry::Subpolyline& subpolyline,
	double expectedResult)
{
	BOOST_CHECK_CLOSE(
    	navikit::map::route::subpolylineLength(
    		accumulate(lengths),
    		subpolyline),
    	expectedResult,
    	TOLERANCE_PERCENT);
}

} // anonymous namespace

BOOST_AUTO_TEST_CASE(SubpolylineLengthTest)
{
    testSubpolylineLength({ 1.0f }, { {0, 0.0}, {0, 1.0} }, 1.0);
    testSubpolylineLength({ 1.0, 1.0 }, { {0, 0.7}, {1, 0.6} }, 0.9);
    testSubpolylineLength({ 1.0, 2.0 }, { {1, 0.0}, {1, 0.7} }, 1.4);
    testSubpolylineLength({ 1.0, 2.0 }, { {0, 0.1}, {0, 0.2} }, 0.1);
    testSubpolylineLength({ 1.0, 2.0, 3.0 }, { {0, 0.0}, {2, 1.0} }, 6.0);
    testSubpolylineLength({ 1.0, 2.0, 3.0, 4.0 }, { {0, 0.5}, {3, 0.5} }, 7.5);
    testSubpolylineLength({ 1.0f }, { {0, 0.2}, {0, 0.7} }, 0.5);
}

}
