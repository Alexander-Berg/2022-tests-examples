#include <maps/libs/sql_chemistry/include/gateway.h>

#include <maps/libs/cmdline/include/cmdline.h>
#include <maps/libs/common/include/hex.h>
#include <maps/libs/geolib/include/test_tools/comparison.h>
#include <maps/libs/local_postgres/include/instance.h>

#include <util/system/hp_timer.h>
#include <util/random/random.h>

#include <pqxx/pqxx>
#include <pqxx/tablereader>

#include <optional>
#include <iostream>

namespace {
using namespace maps;
using namespace sql_chemistry;
using namespace geolib3;

struct Server : public local_postgres::Database {
    static Server& instance() {
        static Server server;
        return server;
    }

    Server() {
        this->createExtension("postgis");
        const std::string createTable = R"(
CREATE SCHEMA signals;
CREATE TABLE signals.ride_track_point (
  id bigserial PRIMARY KEY,
  source_id text NOT NULL,
  pos geometry(Point,3395) NOT NULL,
  date timestamp with time zone NOT NULL,
  accuracy double precision,
  heading double precision
);
)";
        this->executeSql(createTable);
    }
};

using OptDouble = std::optional<double>;

bool approximateEqual(OptDouble a, OptDouble b, double tol = 1e-9) {
    if (a && b) { return std::abs(*a - *b) <= tol; }
    return a.has_value() == b.has_value();
}

struct TrackPoint {
    int64_t id{0};
    std::string sourceId{};
    Point2 pos{};
    chrono::TimePoint timestamp{};
    double accuracy{};
    OptDouble heading{};

    static auto introspect(TrackPoint& t) {
        return std::tie(t.id, t.sourceId, t.pos, t.timestamp, t.accuracy, t.heading);
    }

    bool operator==(const TrackPoint& rhs) const {
        return id == rhs.id && sourceId == rhs.sourceId
            && test_tools::approximateEqual(pos, rhs.pos, 1e-6)
            && timestamp == rhs.timestamp
            && approximateEqual(accuracy, rhs.accuracy) && approximateEqual(heading, rhs.heading);
    }

    bool operator!=(const TrackPoint& rhs) const { return !(rhs == *this); }
};

namespace table {
struct TrackPoint : Table<::TrackPoint> {
    static constexpr std::string_view name_() noexcept { return "signals.ride_track_point"; }

    const BigSerialKey id{"id", name_()};
    const StringColumn sourceId{"source_id", name_()};
    const MercatorColumn<Point2> pos{"pos", name_()};
    const TimePointColumn timestamp{"date", name_()};
    const NumericColumn<double> accuracy{"accuracy", name_()};
    const NullableNumericColumn<double> heading{"heading", name_()};

    auto columns_() const { return std::tie(id, sourceId, pos, timestamp, accuracy, heading); }
};
const TrackPoint trackPoint{};
} // namespace table

using TrackPointGateway = sql_chemistry::Gateway<table::TrackPoint>;

class Program {
public:
    Program(size_t size, std::string connStr) {
        ASSERT(size > 0);
        // Use limit only when connecting to external server, fetch all rows when use local instance.
        if (connStr.empty()) {
            connStr = Server::instance().connectionString();
            limit_ = 0;
            conn_ = std::make_unique<pqxx::connection>(connStr);
            insertTestData(size);
        } else {
            limit_ = size;
            conn_ = std::make_unique<pqxx::connection>(connStr);
        }
    }

    void insertTestData(size_t size) {
        pqxx::work work{*conn_};
        std::cout << "Inserting " << size << " test data rows.\n";
        original_.resize(size);
        for (size_t i = 0; i < size; ++i) {
            auto& p = original_[i];
            p.sourceId = "SourceIdTest" + std::to_string(RandomNumber<unsigned>());
            p.pos = Point2{RandomNumber<double>() * 1e6, RandomNumber<double>() * 1e6};
            p.timestamp = chrono::parseIsoDateTime(
                "2018-08-06 10:00:" + std::to_string(RandomNumber<unsigned>(60u)) + ".00+00:00");
            p.accuracy = RandomNumber<double>();
            if (RandomNumber<bool>()) { p.heading = RandomNumber<double>(); }
        }
        TrackPointGateway{work}.insert(original_);
        work.commit();
    }

    void warmup() {
        pqxx::work work{*conn_};
#pragma GCC diagnostic push
#pragma GCC diagnostic ignored "-Wdeprecated-declarations"
        pqxx::tablereader reader{work, "(" + query(1) + ")"};
#pragma GCC diagnostic pop
        std::string line;
        reader.get_raw_line(line);
        reader.complete();
        work.exec(query(1));
    }

    void runTableReaderWithoutParsing() {
        pqxx::work work{*conn_};
        THPTimer timer;
#pragma GCC diagnostic push
#pragma GCC diagnostic ignored "-Wdeprecated-declarations"
        pqxx::tablereader reader{work, "(" + query(limit_) + ")"};
#pragma GCC diagnostic pop
        std::vector<std::string> rows;

        while (true) {
            if (!reader) { break; }
            std::string line;
            if (!reader.get_raw_line(line)) { break; }
            rows.push_back(std::move(line));
        }

        std::cout << "Loaded  " << rows.size() << " in " << timer.Passed() << ".\n";
    }

    void runTableReader() {
        pqxx::work work{*conn_};
        THPTimer timer;
#pragma GCC diagnostic push
#pragma GCC diagnostic ignored "-Wdeprecated-declarations"
        pqxx::tablereader reader{work, "(" + query(limit_) + ")"};
#pragma GCC diagnostic pop
        std::vector<TrackPoint> all;
        std::vector<std::string> row;

        while (reader >> row) {
            all.emplace_back();
            auto& p = all.back();
            p.id = std::stoll(row[0]);
            p.sourceId = row[1];
            auto bin = hexDecode(std::string_view{row[2]}.substr(2)); // Skip "\x"
            auto data = reinterpret_cast<const char*>(bin.data());
            p.pos = EWKB::read<SpatialReference::Epsg3395, Point2>(
                std::string{data, data + bin.size()});
            p.timestamp = chrono::parseSqlDateTime(row[3]);
            p.accuracy = std::stod(row[4]);
            p.heading = row[5].empty() ? OptDouble{} : OptDouble{std::stod(row[5])};
            row.clear();
        }

        std::cout << "Loaded  " << all.size() << " in " << timer.Passed() << ".\n";
        checkResult(all);
    }

    void runGateway() {
        pqxx::work work{*conn_};
        THPTimer timer;
        TrackPointGateway gtw{work};
        auto all = limit_ > 0 ? gtw.load(orderBy(table::trackPoint.id).limit(limit_)) : gtw.load();
        std::cout << "Loaded  " << all.size() << " in " << timer.Passed() << ".\n";
        checkResult(all);
    }

    void runPqxxParsing() {
        pqxx::work work{*conn_};
        THPTimer timer;
        auto res = work.exec(query(limit_));
        auto execTime = timer.Passed();
        std::vector<TrackPoint> all(res.size());

        for (size_t i = 0; i < res.size(); ++i) {
            auto& p = all[i];
            auto& row = res[i];
            p.id = row[0].as<int64_t>();
            p.sourceId = row[1].as<std::string>();
            p.pos = EWKB::read<SpatialReference::Epsg3395, Point2>(
                pqxx::binarystring{row[2]}.str());
            p.timestamp = chrono::parseSqlDateTime(row[3].as<std::string>());
            p.accuracy = row[4].as<double>();
            p.heading = row[5].is_null() ? OptDouble{} : OptDouble{row[5].as<double>()};
        }

        std::cout << "Loaded  " << all.size() << " in " << timer.Passed()
            << "s (execute time " << execTime << "s).\n";
        checkResult(all);
    }

private:
    void checkResult(std::vector<TrackPoint>& result) {
        std::sort(result.begin(), result.end(), [](auto& p1, auto& p2) { return p1.id < p2.id; });
        if (original_.empty()) {
            original_ = std::move(result);
            return;
        }
        if (result.size() != original_.size()) {
            std::cout << "Size mismatch: " << result.size() << " != " << original_.size() << ".\n";
            return;
        }
        for (size_t i = 0; i < original_.size(); ++i) {
            if (result[i] != original_[i]) {
                std::cout << "Item " << i << " mismatch";
            }
        }
    }

    std::string query(size_t limit) {
        std::ostringstream s;
        s << " SELECT id, source_id, ST_AsEWKB(pos), date, accuracy, heading ";
        s << " FROM " << table::TrackPoint::name_() << " ";
        if (limit > 0) { s << " ORDER BY id LIMIT " << limit << " "; }
        return s.str();
    }

    std::unique_ptr<pqxx::connection> conn_{};
    size_t limit_{};
    std::vector<TrackPoint> original_{};
};

} // namespace

int main(int argc, char* argv[]) try {
    std::ios::sync_with_stdio(false);

    maps::cmdline::Parser cmd;
    cmd.section("Utility for testing gateway performance.");
    auto connStrOpt = cmd.string('c', "conn")
        .defaultValue("")
        .help("Connection string. Create local server if not set.");
    auto sizeOpt = cmd.size_t('s', "size")
        .defaultValue(100000ul)
        .help("Number of entities to fetch from database.");
    cmd.parse(argc, argv);

    Program program{sizeOpt, connStrOpt};

    for (int iter = 0; iter < 3; ++iter) {
        std::cout << std::endl << "Iteration " << iter << ".\n";
        program.warmup();
        std::cout << "--- Table reader without parsing.\n";
        program.runTableReaderWithoutParsing();
        std::cout << "--- Gateway.\n";
        program.runGateway();
        std::cout << "--- Table reader.\n";
        program.runTableReader();
        std::cout << "--- PQXX standard parsing.\n";
        program.runPqxxParsing();
    }

    return 0;
}
catch (const std::exception& e) {
    std::cerr << e.what() << std::endl;
    return 1;
}
