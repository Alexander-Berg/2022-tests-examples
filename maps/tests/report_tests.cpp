#include <maps/wikimap/mapspro/libs/stat_client/include/report.h>

#include <maps/libs/stringutils/include/split.h>

#include <library/cpp/testing/unittest/registar.h>

#include <sstream>

namespace maps::wiki::stat_client::tests {

Y_UNIT_TEST_SUITE(report_tests)
{
    using introspection::operator==;

    struct Dimensions {
        size_t value;

        Dimensions(size_t value): value(value) {}

        auto introspect() const { return value; }

        static void printHeader(csv::OutputStream& os) { os << "dimensions"; };
        void print(csv::OutputStream& os) const { os << value; };
    };

    struct Measures {
        size_t value = 0;

        Measures() = default;
        Measures(size_t value): value(value) {}

        auto introspect() const { return value; }

        static void printHeader(csv::OutputStream& os) { os << "measures"; };
        void print(csv::OutputStream& os) const { os << value; };
    };

    Y_UNIT_TEST(should_set_name)
    {
        Report<Dimensions, Measures, Scale::Daily> report("ReportName");
        UNIT_ASSERT_EQUAL(report.name, "ReportName");
    }

    Y_UNIT_TEST(should_set_scale)
    {
        Report<Dimensions, Measures, Scale::Daily> dailyReport("");
        UNIT_ASSERT_EQUAL(dailyReport.scale, Scale::Daily);

        Report<Dimensions, Measures, Scale::Weekly> weeklyReport("");
        UNIT_ASSERT_EQUAL(weeklyReport.scale, Scale::Weekly);
    }

    Y_UNIT_TEST(should_set_and_get_measures_by_dimensions)
    {
        Report<Dimensions, Measures, Scale::Daily> report("");
        report[Dimensions(3)] = Measures{30};
        report[Dimensions(1)] = Measures(10);
        report[Dimensions(2)] = Measures(20);

        UNIT_ASSERT_EQUAL(report[Dimensions(1)], Measures(10));
        UNIT_ASSERT_EQUAL(report[Dimensions(2)], Measures(20));
        UNIT_ASSERT_EQUAL(report[Dimensions(3)], Measures(30));
    }

    Y_UNIT_TEST(should_update_measures_by_dimensions)
    {
        Report<Dimensions, Measures, Scale::Daily> report("");
        report[Dimensions(42)] = Measures{24};
        UNIT_ASSERT_EQUAL(report[Dimensions(42)], Measures(24));
        report[Dimensions(42)] = Measures{42};
        UNIT_ASSERT_EQUAL(report[Dimensions(42)], Measures(42));
    }

    Y_UNIT_TEST(should_print_empty_csv)
    {
        std::ostringstream oss;
        csv::OutputStream csvOut(oss);

        Report<Dimensions, Measures, Scale::Quarterly> report("");
        report.print(csvOut);
        UNIT_ASSERT_EQUAL(
            oss.str(),
            "dimensions,measures"
        );
    }

    Y_UNIT_TEST(should_print_csv)
    {
        Report<Dimensions, Measures, Scale::Quarterly> report("");
        report[Dimensions(30)] = Measures{3};
        report[Dimensions(10)] = Measures(1);
        report[Dimensions(20)] = Measures(2);

        std::ostringstream oss;
        csv::OutputStream csvOut(oss);
        csvOut.setNewLineStyle(csv::NewLineStyle::LF);

        report.print(csvOut);
        auto csv = stringutils::fields(oss.str(), '\n'); // fields() drops empty lines, whereas split() does not.

        UNIT_ASSERT_EQUAL(csv.size(), 4);

        // Check header
        UNIT_ASSERT_EQUAL(csv[0], "dimensions,measures");

        // Erase header and check body
        csv.erase(csv.begin());
        std::sort(csv.begin(), csv.end());
        UNIT_ASSERT_EQUAL(csv[0], "10,1");
        UNIT_ASSERT_EQUAL(csv[1], "20,2");
        UNIT_ASSERT_EQUAL(csv[2], "30,3");
    }
}

} // namespace maps::wiki::stat_client::tests
