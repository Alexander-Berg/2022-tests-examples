#include "../../test_types/common.h"
#include "../suite.h"

#include "../../test_types/face_validation_test_data.h"

#include <maps/libs/geolib/include/point.h>
#include <maps/libs/geolib/include/segment.h>
#include <maps/libs/geolib/include/polyline.h>

#include <string>
#include <map>
#include <vector>

using namespace maps;
using namespace maps::wiki;
using namespace maps::wiki::topo;
using namespace maps::wiki::topo::test;
using namespace maps::geolib3;

TEST_SUITE_START(face_validation_tests, FaceValidationTestData)

TEST_DATA(test_1_1)
{
    "new unclosed",
    MockStorage {
        test::NodeDataVector {
            { 1, {1, 1} },
            { 2, {4, 4} }
        },
        test::EdgeDataVector {
            { 1, 1, 2, Polyline2 { PointsVector {{1, 1}, {4, 4}} } }
        }
    },
    MockStorageDiff {
        {}, // nodes
        {}, // edges
        {
            { 10, test::Face {10, EdgeIDSet {1} } }
        }
    },
    10, // face id
    FaceRelationsAvailability::Diff,
    FaceValidationTestData::Type::Incorrect
};

TEST_DATA(test_1_2)
{
    "new from multiple components",
    MockStorage {
        test::NodeDataVector {
            { 1, {1, 1} },
            { 2, {1, 3} },
            { 3, {4, 4} }
        },
        test::EdgeDataVector {
            { 1, 1, 2, Polyline2 { PointsVector {{1, 1}, {1, 3}} } } ,
            { 2, 1, 2, Polyline2 { {{1, 1}, {3, 1}, {1, 3}} } } ,
            { 3, 3, 3, Polyline2 { {{4, 4}, {5, 2}, {5, 4}, {4, 4}} } }
        }
    },
    MockStorageDiff {
        {}, // nodes
        {}, // edges
        {
            { 10, test::Face {10, EdgeIDSet {1, 2, 3} } }
        }
    },
    10, // face id
    FaceRelationsAvailability::Diff,
    FaceValidationTestData::Type::Incorrect
};

TEST_DATA(test_1_3)
{
    "new with wrong incidences",
    MockStorage {
        test::NodeDataVector {
            { 1, {1, 1} },
            { 2, {1, 3} },
            { 3, {3, 1} }
        },
        test::EdgeDataVector {
            { 1, 1, 2, Polyline2 { PointsVector {{1, 1}, {1, 3}} } },
            { 2, 1, 3, Polyline2 { PointsVector {{1, 1}, {3, 1}} } },
            { 3, 3, 2, Polyline2 { PointsVector {{3, 1}, {1, 3}} } },
            { 4, 3, 3, Polyline2 { {{3, 1}, {5, 2}, {5, 4}, {4, 4}, {3, 1}} } }
        }
    },
    MockStorageDiff {
        {}, // nodes
        {}, // edges
        {
            { 10, test::Face {10, EdgeIDSet {1, 2, 3, 4} } }
        }
    },
    10, // face id
    FaceRelationsAvailability::Diff,
    FaceValidationTestData::Type::Incorrect
};

TEST_DATA(test_2)
{
    "add path through existing face nodes",
    MockStorage {
        test::NodeDataVector {
            { 1, {2, 2} },
            { 2, {4, 2} },
            { 3, {6, 2} },
            { 4, {6, 4} },
            { 5, {4, 4} },
            { 6, {2, 4} }
        },
        test::EdgeDataVector {
            { 1, 1, 6, Polyline2 { {{2, 2}, {1, 3}, {2, 4}} } },
            { 2, 1, 2, Polyline2 { PointsVector {{2, 2}, {4, 2}} } },
            { 3, 2, 3, Polyline2 { PointsVector {{4, 2}, {6, 2}} } },
            { 4, 3, 4, Polyline2 { {{6, 2}, {7, 3}, {6, 4}} } },
            { 5, 4, 5, Polyline2 { PointsVector {{6, 4}, {4, 4}} } },
            { 6, 5, 6, Polyline2 { PointsVector {{4, 4}, {2, 4}} } }
        },
        test::FaceDataVector {
            { 100, EdgeIDSet {1, 2, 3, 4, 5, 6} }
        }
    },
    MockStorageDiff {
        {}, // nodes
        {
            { 4, test::Edge { 4, 4, 5, Polyline2 {{{6, 4}, {5, 6}, {4, 4}}} } },
            { 10, test::Edge { 10, 5, 2, Polyline2 { PointsVector {{4, 4}, {4, 2}} } } },
            { 11, test::Edge { 11, 2, 3, Polyline2 {{{4, 2}, {4, 0}, {6, 2}}} } }
        },
        {
            { 100, test::Face {100, EdgeIDSet {1, 2, 3, 4, 5, 6, 10, 11} } }
        }
    },
    100, // face id
    FaceRelationsAvailability::Diff,
    FaceValidationTestData::Type::Incorrect
};

TEST_DATA(test_3_1)
{
    "remove all edges from face",
    MockStorage {
        test::NodeDataVector {
            { 1, {1, 1} },
            { 2, {1, 3} },
            { 3, {3, 1} }
        },
        test::EdgeDataVector {
            { 1, 1, 2, Polyline2 { PointsVector {{1, 1}, {1, 3}} } },
            { 2, 1, 3, Polyline2 { PointsVector {{1, 1}, {3, 1}} } },
            { 3, 3, 2, Polyline2 { PointsVector {{3, 1}, {1, 3}} } }
        },
        test::FaceDataVector {
            { 100, EdgeIDSet {1, 2, 3} }
        }
    },
    MockStorageDiff {
        {}, // nodes
        {}, // edges
        {
            { 100, test::Face {100, EdgeIDSet {} } }
        }
    },
    100, // face id
    FaceRelationsAvailability::Diff,
    FaceValidationTestData::Type::Incorrect
};

TEST_DATA(test_3_2)
{
    "add new loop to face",
    MockStorage {
        test::NodeDataVector {
            { 1, {1, 1} },
            { 2, {1, 3} },
            { 3, {3, 1} },
            { 4, {4, 4} },
            { 5, {4, 6} },
            { 6, {6, 4} }
        },
        test::EdgeDataVector {
            { 1, 1, 2, Polyline2 { PointsVector {{1, 1}, {1, 3}} } },
            { 2, 1, 3, Polyline2 { PointsVector {{1, 1}, {3, 1}} } },
            { 3, 3, 2, Polyline2 { PointsVector {{3, 1}, {1, 3}} } },
            { 4, 4, 5, Polyline2 { PointsVector {{4, 4}, {4, 6}} } },
            { 5, 5, 6, Polyline2 { PointsVector {{4, 6}, {6, 4}} } },
            { 6, 4, 6, Polyline2 { PointsVector {{4, 4}, {6, 4}} } }
        },
        test::FaceDataVector {
            { 100, EdgeIDSet {1, 2, 3} }
        }
    },
    MockStorageDiff {
        {}, // nodes
        {}, // edges
        {
            { 100, test::Face {100, EdgeIDSet {1, 2, 3, 4, 5, 6} } }
        }
    },
    100, // face id
    FaceRelationsAvailability::Diff,
    FaceValidationTestData::Type::Incorrect
};

TEST_DATA(test_3_3)
{
    "rebuild face, remove loop and add other",
    MockStorage {
        test::NodeDataVector {
            { 1, {1, 1} },
            { 2, {1, 3} },
            { 3, {3, 1} },
            { 4, {4, 4} },
            { 5, {4, 6} },
            { 6, {6, 4} }
        },
        test::EdgeDataVector {
            { 1, 1, 2, Polyline2 { PointsVector {{1, 1}, {1, 3}} } },
            { 2, 1, 3, Polyline2 { PointsVector {{1, 1}, {3, 1}} } },
            { 3, 3, 2, Polyline2 { PointsVector {{3, 1}, {1, 3}} } },
            { 4, 4, 5, Polyline2 { PointsVector {{4, 4}, {4, 6}} } },
            { 5, 5, 6, Polyline2 { PointsVector {{4, 6}, {6, 4}} } },
            { 6, 4, 6, Polyline2 { PointsVector {{4, 4}, {6, 4}} } }
        },
        test::FaceDataVector {
            { 100, EdgeIDSet {1, 2, 3} }
        }
    },
    MockStorageDiff {
        {}, // nodes
        {}, // edges
        {
            { 100, test::Face {100, EdgeIDSet {4, 5, 6} } }
        }
    },
    100, // face id
    FaceRelationsAvailability::Diff,
    FaceValidationTestData::Type::Correct
};

TEST_DATA(test_3_4)
{
    "exclude shared edge from face",
    MockStorage {
        test::NodeDataVector {
            { 1, {1, 4} },
            { 2, {3, 3} },
            { 3, {3, 1} }
        },
        test::EdgeDataVector {
            { 1, 1, 2, Polyline2 { {{1, 4}, {3, 4}, {3, 3}} } },
            { 2, 1, 3, Polyline2 { {{1, 4}, {1, 0}, {3, 0}, {3, 1}} } },
            { 3, 2, 3, Polyline2 { PointsVector {{3, 3}, {3, 1}} } },
            { 4, 2, 3, Polyline2 { {{3, 3}, {5, 3}, {5, 1}, {3, 1}} } }
        },
        test::FaceDataVector {
            { 100, EdgeIDSet {1, 2, 3} },
            { 101, EdgeIDSet {3, 4} }
        }
    },
    MockStorageDiff {
        {}, // nodes
        {}, // edges
        {
            { 100, test::Face { 100, EdgeIDSet {1, 2} } }
        }
    },
    100, // face id
    FaceRelationsAvailability::Diff,
    FaceValidationTestData::Type::Incorrect
};

TEST_DATA(test_4_1)
{
    "break loop in face by editing its edge",
    MockStorage {
        test::NodeDataVector {
            { 1, {1, 1} },
            { 2, {1, 3} },
            { 3, {3, 1} }
        },
        test::EdgeDataVector {
            { 1, 1, 2, Polyline2 { PointsVector {{1, 1}, {1, 3}} } },
            { 2, 1, 3, Polyline2 { PointsVector {{1, 1}, {3, 1}} } },
            { 3, 3, 2, Polyline2 { PointsVector {{3, 1}, {1, 3}} } }
        },
        test::FaceDataVector {
            { 100, EdgeIDSet {1, 2, 3} }
        }
    },
    MockStorageDiff {
        {
            { 4, test::Node { 4, {4, 4} } }
        },
        {
            { 3, test::Edge { 3, 3, 4, Polyline2 { PointsVector {{3, 1}, {4, 4}} } } }
        },
        {} // faces
    },
    100, // face id
    FaceRelationsAvailability::Diff,
    FaceValidationTestData::Type::Incorrect
};

TEST_DATA(test_4_2)
{
    "break loop in face by removing its edge",
    MockStorage {
        test::NodeDataVector {
            { 1, {1, 1} },
            { 2, {1, 3} },
            { 3, {3, 1} }
        },
        test::EdgeDataVector {
            { 1, 1, 2, Polyline2 { PointsVector {{1, 1}, {1, 3}} } },
            { 2, 1, 3, Polyline2 { PointsVector {{1, 1}, {3, 1}} } },
            { 3, 3, 2, Polyline2 { PointsVector {{3, 1}, {1, 3}} } }
        },
        test::FaceDataVector {
            { 100, EdgeIDSet {1, 2, 3} }
        }
    },
    MockStorageDiff {
        {}, // nodes
        {}, // edges
        {
            { 100, test::Face { 100, EdgeIDSet {1, 2} } }
        }
    },
    100, // face id
    FaceRelationsAvailability::Diff,
    FaceValidationTestData::Type::Incorrect
};

TEST_DATA(test_5_1)
{
    "exchange paths from face",
    MockStorage {
        test::NodeDataVector {
            { 1, {2, 2} },
            { 2, {6, 2} },
            { 3, {6, 4} },
            { 4, {2, 4} }
        },
        test::EdgeDataVector {
            { 1, 1, 2, Polyline2 { PointsVector {{2, 2}, {6, 2}} } },
            { 2, 2, 3, Polyline2 { PointsVector {{6, 2}, {6, 4}} } },
            { 3, 3, 4, Polyline2 { PointsVector {{6, 4}, {2, 4}} } },
            { 4, 4, 1, Polyline2 { PointsVector {{2, 4}, {2, 2}} } }
        },
        test::FaceDataVector {
            { 100, EdgeIDSet {1, 2, 3, 4} }
        }
    },
    MockStorageDiff {
        {}, // nodes
        {
            { 2, boost::none },
            { 10, test::Edge { 10, 1, 3, Polyline2 { PointsVector {{2, 2}, {6, 4}} } } },
            { 4, test::Edge { 4, 2, 4, Polyline2 { PointsVector {{6, 2}, {2, 4}} } } }
        },
        {
            { 100, test::Face { 100, EdgeIDSet {1, 3, 4, 10} } }
        }
    },
    100, // face id
    FaceRelationsAvailability::Diff,
    FaceValidationTestData::Type::Incorrect
};


TEST_DATA(test_6)
{
    "add new invalid path",
    MockStorage {
        test::NodeDataVector {
            { 1, {2, 2} },
            { 2, {6, 2} },
            { 3, {6, 4} },
            { 4, {2, 4} }
        },
        test::EdgeDataVector {
            { 1, 1, 2, Polyline2 { PointsVector {{2, 2}, {6, 2}} } },
            { 2, 2, 3, Polyline2 { PointsVector {{6, 2}, {6, 4}} } },
            { 3, 3, 4, Polyline2 { PointsVector {{6, 4}, {2, 4}} } },
            { 4, 4, 1, Polyline2 { PointsVector {{2, 4}, {2, 2}} } }
        },
        test::FaceDataVector {
            { 100, EdgeIDSet {1, 2, 3, 4} }
        }
    },
    MockStorageDiff {
        {
            { 5, test::Node { 5, {4, 5} } },
            { 6, test::Node { 6, {3, 6} } },
            { 7, test::Node { 7, {5, 6} } }
        },
        {
            { 10, test::Edge { 10, 4, 5, Polyline2 { PointsVector {{2, 4}, {4, 5}} } } },
            { 11, test::Edge { 11, 3, 5, Polyline2 { PointsVector {{6, 4}, {4, 5}} } } },
            { 12, test::Edge { 12, 5, 6, Polyline2 { PointsVector {{4, 5}, {3, 6}} } } },
            { 13, test::Edge { 13, 5, 7, Polyline2 { PointsVector {{4, 5}, {5, 6}} } } },
            { 14, test::Edge { 14, 6, 7, Polyline2 { PointsVector {{3, 6}, {5, 6}} } } }
        },
        {
            { 100, test::Face { 100, EdgeIDSet {1, 2, 4, 10, 11, 12, 13, 14} } }
        }
    },
    100, // face id
    FaceRelationsAvailability::Diff,
    FaceValidationTestData::Type::Incorrect
};

TEST_DATA(test_7_1)
{
    "correct, replace two paths in contour",
    MockStorage {
        test::NodeDataVector {
            { 1, {2, 4} },
            { 2, {5, 5} },
            { 3, {8, 3} },
            { 4, {6, 1} },
            { 5, {3, 1} }
        },
        test::EdgeDataVector {
            { 1, 1, 2, Polyline2 { PointsVector {{2, 4}, {5, 5}} } },
            { 2, 2, 3, Polyline2 { PointsVector {{5, 5}, {8, 3}} } },
            { 3, 3, 4, Polyline2 { PointsVector {{8, 3}, {6, 1}} } },
            { 4, 4, 5, Polyline2 { PointsVector {{6, 1}, {3, 1}} } },
            { 5, 5, 1, Polyline2 { PointsVector {{3, 1}, {2, 4}} } }
        },
        test::FaceDataVector {
            { 100, EdgeIDSet {1, 2, 3, 4, 5} }
        }
    },
    MockStorageDiff {
        {
            { 1, boost::none },
            { 5, boost::none },
            { 6, test::Node { 6, {6.5, 4} } },
            { 7, test::Node { 7, {7, 2} } },
            { 8, test::Node { 8, {3.5, 4.5} } },
            { 9, test::Node { 9, {4, 1} } },
            { 10, test::Node { 10, {0, 6} } },
            { 11, test::Node { 11, {0, 0} } }
        },
        {
            { 1, test::Edge { 1, 2, 8, Polyline2 { PointsVector {{5, 5}, {3.5, 4.5}} } } },
            { 2, test::Edge { 2, 2, 6, Polyline2 { PointsVector {{5, 5}, {6.5, 4}} } } },
            { 3, test::Edge { 3, 4, 7, Polyline2 { PointsVector {{6, 1}, {7, 2}} } } },
            { 4, test::Edge { 4, 4, 9, Polyline2 { PointsVector {{6, 1}, {4, 1}} } } },
            { 5, boost::none },
            { 10, test::Edge { 10, 6, 7, Polyline2 { {{6.5, 4}, {9, 6}, {9, 2}, {7, 2}} } } },
            { 11, test::Edge { 11, 8, 10, Polyline2 { PointsVector {{3.5, 4.5}, {0, 6}} } } },
            { 12, test::Edge { 12, 10, 11, Polyline2 { PointsVector {{0, 6}, {0, 0}} } } },
            { 13, test::Edge { 13, 9, 11, Polyline2 { {{4, 1}, {4, 0}, {0, 0}} } } }
        },
        {
            { 100, test::Face { 100, EdgeIDSet {1, 2, 3, 4, 10, 11, 12, 13} } }
        }
    },
    100, // face id
    FaceRelationsAvailability::Diff,
    FaceValidationTestData::Type::Correct
};

TEST_DATA(test_7_2)
{
    "correct, replace two paths in contour, remaining edges",
    MockStorage {
        test::NodeDataVector {
            { 1, {2, 4} },
            { 2, {5, 5} },
            { 3, {8, 3} },
            { 4, {6, 1} },
            { 5, {3, 1} }
        },
        test::EdgeDataVector {
            { 1, 1, 2, Polyline2 { PointsVector {{2, 4}, {5, 5}} } },
            { 2, 2, 3, Polyline2 { PointsVector {{5, 5}, {8, 3}} } },
            { 3, 3, 4, Polyline2 { PointsVector {{8, 3}, {6, 1}} } },
            { 4, 4, 5, Polyline2 { PointsVector {{6, 1}, {3, 1}} } },
            { 5, 5, 1, Polyline2 { PointsVector {{3, 1}, {2, 4}} } }
        },
        test::FaceDataVector {
            { 100, EdgeIDSet {1, 2, 3, 4, 5} }
        }
    },
    MockStorageDiff {
        {
            { 6, test::Node { 6, {6.5, 4} } },
            { 7, test::Node { 7, {7, 2} } },
            { 8, test::Node { 8, {3.5, 4.5} } },
            { 9, test::Node { 9, {4, 1} } },
            { 10, test::Node { 10, {0, 6} } },
            { 11, test::Node { 11, {0, 0} } }
        },
        {
            { 1, test::Edge { 1, 2, 8, Polyline2 { PointsVector {{5, 5}, {3.5, 4.5}} } } },
            { 2, test::Edge { 2, 2, 6, Polyline2 { PointsVector {{5, 5}, {6.5, 4}} } } },
            { 3, test::Edge { 3, 4, 7, Polyline2 { PointsVector {{6, 1}, {7, 2}} } } },
            { 4, test::Edge { 4, 4, 9, Polyline2 { PointsVector {{6, 1}, {4, 1}} } } },
            { 10, test::Edge { 10, 6, 7, Polyline2 { {{6.5, 4}, {9, 6}, {9, 2}, {7, 2}} } } },
            { 11, test::Edge { 11, 8, 10, Polyline2 { PointsVector {{3.5, 4.5}, {0, 6}} } } },
            { 12, test::Edge { 12, 10, 11, Polyline2 { PointsVector {{0, 6}, {0, 0}} } } },
            { 13, test::Edge { 13, 9, 11, Polyline2 { {{4, 1}, {4, 0}, {0, 0}} } } },
            { 14, test::Edge { 14, 1, 8, Polyline2 { PointsVector {{2, 4}, {3.5, 4.5}} } } },
            { 15, test::Edge { 15, 5, 9, Polyline2 { PointsVector {{3, 1}, {4, 1}} } } }
        },
        {
            { 100, test::Face { 100, EdgeIDSet {1, 2, 3, 4, 10, 11, 12, 13} } }
        }
    },
    100, // face id
    FaceRelationsAvailability::Diff,
    FaceValidationTestData::Type::Correct
};

TEST_SUITE_END(face_validation_tests)
