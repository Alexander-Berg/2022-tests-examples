#include <maps/libs/cmdline/include/cmdline.h>

#include <maps/libs/common/include/exception.h>

#include <library/cpp/testing/unittest/registar.h>

#include <climits> // for broken boost::test on hardy
#include <limits>
#include <stdexcept>


class UsageError: public maps::RuntimeError {
public:
    using RuntimeError::RuntimeError;
};

class Parser: public maps::cmdline::Parser {
public:
    Parser()
    {
        onFail([](const std::string& msg) { throw UsageError(msg); });
    }

    void parse(std::vector<std::string> args, const std::string& argv0 = "self")
    {
        args.insert(args.begin(), argv0);
        std::vector<char*> argv;
        for (std::string& s: args)
            argv.push_back(&s[0]);
        argv.push_back(0);

        maps::cmdline::Parser::parse(args.size(), argv.data());
    }

    std::string fail(std::vector<std::string> args, const std::string& argv0 = "self")
    {
        try {
            parse(std::move(args), argv0);
            throw maps::RuntimeError("parsed an invalid command line");
        }
        catch (const UsageError& err) {
            std::string msg = err.what();
            std::string hdr = argv0 + ": ";
            if (msg.substr(0, hdr.size()) == hdr)
                msg.erase(0, hdr.size());
            return msg;
        }
    }
};

using maps::cmdline::Option;

Y_UNIT_TEST_SUITE(BasicTest) {

Y_UNIT_TEST(basic_test)
{
    struct Fixture {
        Parser p;
        Option<std::string> str;
        Option<int> num;
        Option<bool> flag;
        Option<double> real;

        Fixture():
            str(p, "str", 's'),
            num(p, "num", 'n'),
            flag(p, "bool", 'b'),
            real(p, "real", 'r')
        {}
    };

    Fixture f1;
    f1.p.parse({ "-s", "foo", "-n10", "-b", "arg1", "arg2", "-r", "5.2" });
    UNIT_ASSERT(f1.str.defined() && f1.str == "foo");
    UNIT_ASSERT(f1.num.defined() && f1.num == 10);
    UNIT_ASSERT(f1.flag.defined() && f1.flag);
    UNIT_ASSERT(f1.real.defined() && f1.real >= 5.19 && f1.real <= 5.21);
    UNIT_ASSERT_EQUAL(f1.p.argv().size(), 2);
    UNIT_ASSERT_EQUAL(f1.p.argv()[0], "arg1");
    UNIT_ASSERT_EQUAL(f1.p.argv()[1], "arg2");

    Fixture f2;
    f2.p.parse({ "--bool=no", "--num", "10", "-sxx" });
    UNIT_ASSERT(f2.str.defined() && f2.str == "xx");
    UNIT_ASSERT(f2.num.defined() && f2.num == 10);
    UNIT_ASSERT(f2.flag.defined() && !f2.flag);
    UNIT_ASSERT(!f2.real.defined());
    UNIT_ASSERT(f2.p.argv().empty());


    UNIT_ASSERT_EXCEPTION(Fixture().p.parse({ "--num=xxx" }), UsageError);
    UNIT_ASSERT_EQUAL(Fixture().p.fail({ "--foo" }), "unknown option `--foo'");
    UNIT_ASSERT_EQUAL(Fixture().p.fail({ "--num" }), "option `--num' requires an argument");
    UNIT_ASSERT_EQUAL(Fixture().p.fail({ "-zn10" }), "unknown option `-z'");
    UNIT_ASSERT_EQUAL(Fixture().p.fail({ "-bn" }), "option `-n' requires an argument");
}

Y_UNIT_TEST(container_test)
{
    struct Fixture {
        Parser p;
        Option<std::vector<int>> vint;
        Option<std::map<std::string, std::string>> mstr;
        Option<std::optional<std::string>> opt;

        Fixture():
            vint(p, "vector", 'v'),
            mstr(p, "map", 'm'),
            opt(p, "optional", 'o')
        {
            mstr.sep(',');
        }
    };
    auto getmstr = [](const Fixture& f, const std::string& k) -> std::string {
        auto i = f.mstr.find(k);
        return i != f.mstr.end() ? i->second : "";
    };

    Fixture f1;
    f1.p.parse({
        "-v10", "-mone=1", "--vector=20", "--map=two=2",
        "--vector", "30", "--map", "three=3", "-m", "four=4,five=5,six=6",
        "-m", "seven=Nw=="
    });
    UNIT_ASSERT(f1.vint.defined() && f1.vint.size() == 3);
    UNIT_ASSERT_EQUAL(f1.vint[0], 10);
    UNIT_ASSERT_EQUAL(f1.vint[1], 20);
    UNIT_ASSERT_EQUAL(f1.vint[2], 30);

    UNIT_ASSERT(f1.mstr.defined() && f1.mstr.size() == 7);
    UNIT_ASSERT_EQUAL(getmstr(f1, "one"),   "1");
    UNIT_ASSERT_EQUAL(getmstr(f1, "two"),   "2");
    UNIT_ASSERT_EQUAL(getmstr(f1, "three"), "3");
    UNIT_ASSERT_EQUAL(getmstr(f1, "four"),  "4");
    UNIT_ASSERT_EQUAL(getmstr(f1, "five"),  "5");
    UNIT_ASSERT_EQUAL(getmstr(f1, "six"),   "6");
    UNIT_ASSERT_EQUAL(getmstr(f1, "seven"), "Nw==");

    Fixture f2;
    f2.p.parse({ "-o" });
    UNIT_ASSERT(f2.opt.defined() && !f2.opt);

    Fixture f3;
    f3.p.parse({ "-ofoo" });
    UNIT_ASSERT(f3.opt.defined() && f3.opt && f3.opt.value() == "foo");
}

Y_UNIT_TEST(duration_test)
{
    struct Fixture {
        Parser p;
        Option<std::chrono::milliseconds> d;

        Fixture():
            d(p, 'd', "duration")
        {}
    };

    Fixture f1;
    f1.p.parse({ "--duration=10ms" });
    UNIT_ASSERT(f1.d.defined() && f1.d.count() == 10);

    Fixture f2;
    f2.p.parse({ "-d", "1min" });
    UNIT_ASSERT(f2.d.defined() && f2.d.count() == 60000);

    UNIT_ASSERT_EQUAL(
        Fixture().p.fail({ "--duration", "10us" }),
        "cannot parse `-d 10us': duration resolution too high"
    );
}

Y_UNIT_TEST(required_test)
{
    Parser p;
    auto opt = p.string("foo", 'f').required();
    UNIT_ASSERT_EQUAL(p.fail({}), "required option `-f' missing");
}

struct IstreamableOption {
    int intValue;
    double doubleValue;

    friend std::istream& operator>>(std::istream& is, IstreamableOption& option) {
        char c;
        is >> c;
        REQUIRE(c == '{', "Wrong format: expected '{'");
        is >> std::ws >> option.intValue >> std::ws >> c;
        REQUIRE(c == ';', "Wrong format: expected ';'");
        is >> std::ws >> option.doubleValue >> std::ws >> c;
        REQUIRE(c == '}', "Wrong format: expected '}'");

        return is;
    }
};

Y_UNIT_TEST(istreamable_option_test)
{
    struct Fixture {
        Parser p;
        Option<IstreamableOption> istreamableOption;
        Fixture(): istreamableOption(p, "istreamable", 'i') {}
    };

    Fixture f;
    UNIT_ASSERT(!f.istreamableOption.defined());
    f.p.parse({"--istreamable", "{10; 3.14\t}"});
    UNIT_ASSERT(f.istreamableOption.defined());
    UNIT_ASSERT(f.istreamableOption.intValue == 10);
    UNIT_ASSERT_DOUBLES_EQUAL(f.istreamableOption.doubleValue, 3.14, std::numeric_limits<double>::epsilon());
}

}
