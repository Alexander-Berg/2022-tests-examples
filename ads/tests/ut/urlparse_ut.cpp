#include "urlparse.h"

#include <library/cpp/testing/unittest/registar.h>

#include <util/generic/map.h>
#include <util/stream/output.h>

#include <Python.h>

using namespace NTsarTransport;

static inline IOutputStream& operator<<(IOutputStream& out, const TCgiParameters& parsedQuery) {
    if (parsedQuery.empty()) {
        out << "{}";
        return out;
    }
    out << "{";
    for (auto start = parsedQuery.begin(); start != parsedQuery.end();) {
        out << "\'" << start->first << "\': [";
        auto [range_start, range_end] = parsedQuery.equal_range(start->first);
        while (range_start != range_end) {
            out << "\'" << range_start->second << "\',";
            ++range_start;
        }
        out << "], ";
        start = range_start;
    }
    out << "}";
    return out;
}

class TURLParseTest : public TTestBase {
public:
    UNIT_TEST_SUITE(TURLParseTest);
    UNIT_TEST(NetLocParsedCorrectly);
    UNIT_TEST(PathParsedCorrectly);
    UNIT_TEST(QueryParsedCorrectly);
    UNIT_TEST_SUITE_END();

    void NetLocParsedCorrectly() {
        for (decltype(TestUrls.size()) i = 0; i < TestUrls.size(); ++i) {
            TParsedURL parsedUrl(TestUrls[i]);
            Cerr << "Test case: \'" << TestUrls[i] << "\' \'" << parsedUrl.NetLoc() << "\' == \'" << ExpectedNetLocs[i] << "\'" << Endl;
            UNIT_ASSERT_EQUAL(parsedUrl.NetLoc(), ExpectedNetLocs[i]);
        }
    }

    void PathParsedCorrectly() {
        for (decltype(TestUrls.size()) i = 0; i < TestUrls.size(); ++i) {
            TParsedURL parsedUrl(TestUrls[i]);
            Cerr << "Test case: \'" << TestUrls[i] << "\' \'" << parsedUrl.Path() << "\' == \'" << ExpectedPaths[i] << "\'" << Endl;
            UNIT_ASSERT_EQUAL(parsedUrl.Path(), ExpectedPaths[i]);
        }
    }

    void QueryParsedCorrectly() {
        for (decltype(TestUrls.size()) i = 0; i < TestUrls.size(); ++i) {
            TParsedURL parsedUrl(TestUrls[i]);
            Cerr << "Test case: \'" << TestUrls[i] << "\' \'" << parsedUrl.Query() << "\' == \'" << ExpectedQueries[i] << "\'" << Endl;
            UNIT_ASSERT_EQUAL(parsedUrl.Query(), ExpectedQueries[i]);
        }
    }

    TURLParseTest() {
        Py_Initialize();
        PyObject* urlparseModule = PyImport_ImportModule("urlparse");
        PyObject* urlparseFuncs = PyModule_GetDict(urlparseModule);
        PyObject* urlparse = PyDict_GetItemString(urlparseFuncs, "urlparse");
        UNIT_ASSERT(PyCallable_Check(urlparse));
        TestUrls = {
            "http://ya.ru",
            "http://ya.ru",
            "http://ya.ru/path1/path2",
            "https://ya.ru?a=x&b=y,z",
            "https://ya.ru/path1?a=x",
            "https://ya.ru/?a=",
            "httpszzz://ya.ru",
            "http://YA.ru"
        };
        for (const auto& url : TestUrls) {
            PyObject* pyUrl = Py_BuildValue("(s)", (char*) url.data());
            UNIT_ASSERT_VALUES_UNEQUAL(pyUrl, nullptr);
            PyObject* parsedUrl = PyObject_CallObject(urlparse, pyUrl);
            UNIT_ASSERT_VALUES_UNEQUAL(parsedUrl, nullptr);
            PyObject* netLoc = PyObject_GetAttrString(parsedUrl, "netloc");
            UNIT_ASSERT_VALUES_UNEQUAL(netLoc, nullptr);
            ExpectedNetLocs.emplace_back(PyString_AsString(netLoc));
            PyObject* path = PyObject_GetAttrString(parsedUrl, "path");
            UNIT_ASSERT_VALUES_UNEQUAL(path, nullptr);
            ExpectedPaths.emplace_back(PyString_AsString(path));
            PyObject* query = PyObject_GetAttrString(parsedUrl, "query");
            UNIT_ASSERT_VALUES_UNEQUAL(query, nullptr);
            ExpectedQueries.emplace_back(PyString_AsString(query));
            Py_DECREF(pyUrl);
            Py_DECREF(parsedUrl);
        }
        Py_DECREF(urlparseModule);
        Py_DECREF(urlparse);
    }

    ~TURLParseTest() override = default;
private:
    TVector<TString> TestUrls;
    TVector<TString> ExpectedNetLocs;
    TVector<TString> ExpectedPaths;
    TVector<TString> ExpectedQueries;
};

UNIT_TEST_SUITE_REGISTRATION(TURLParseTest);

class TParseQueryTest : public TTestBase {
public:
    UNIT_TEST_SUITE(TParseQueryTest);
    UNIT_TEST(ParsesQueryCorrectly);
    UNIT_TEST_SUITE_END();

    void ParsesQueryCorrectly() {
        for (decltype(TestQueries.size()) i = 0; i < TestQueries.size(); ++i) {
            auto result = ParseQuery(TestQueries[i]);
            Cerr << "Test case: \'" << TestQueries[i] << "\' \'" << result << "\' == \'" << ExpectedQueries[i] << "\'" << Endl;
            UNIT_ASSERT_EQUAL(result, ExpectedQueries[i]);
        }
    }

    TParseQueryTest() {
        Py_Initialize();
        PyObject* urlparseModule = PyImport_ImportModule("urlparse");
        PyObject* urlparseFuncs = PyModule_GetDict(urlparseModule);
        PyObject* parseQS = PyDict_GetItemString(urlparseFuncs, "parse_qs");
        UNIT_ASSERT(PyCallable_Check(parseQS));
        TestQueries = {
            "",
            "a=x",
            "a=x&b=y",
            "a=x,y&b=z",
            "a=x,y&a=z",
            "a="
        };
        for (const auto& query : TestQueries) {
            ExpectedQueries.emplace_back();
            PyObject* pyUrl = Py_BuildValue("(s)", (char*) query.data());
            UNIT_ASSERT_VALUES_UNEQUAL(pyUrl, nullptr);
            PyObject* parsedQuery = PyObject_CallObject(parseQS, pyUrl);
            UNIT_ASSERT_VALUES_UNEQUAL(parsedQuery, nullptr);
            PyObject *key, *value;
            Py_ssize_t pos = 0;
            while (PyDict_Next(parsedQuery, &pos, &key, &value)) {
                UNIT_ASSERT_VALUES_UNEQUAL(key, nullptr);
                UNIT_ASSERT_VALUES_UNEQUAL(value, nullptr);
                for (Py_ssize_t i = 0; i < PyList_Size(value); ++i) {
                    PyObject* paramValue = PyList_GetItem(value, i);
                    UNIT_ASSERT_VALUES_UNEQUAL(paramValue, nullptr);
                    ExpectedQueries.back().emplace(PyString_AsString(key), PyString_AsString(paramValue));
                }
            }
            Py_DECREF(pyUrl);
            Py_DECREF(parsedQuery);
        }
        Py_DECREF(parseQS);
        Py_DECREF(urlparseModule);
        Py_Finalize();
    }

    ~TParseQueryTest() override = default;

private:
    TVector<TString> TestQueries;
    TVector<TCgiParameters> ExpectedQueries;
};

UNIT_TEST_SUITE_REGISTRATION(TParseQueryTest);
