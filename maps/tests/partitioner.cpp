#include "partitioner.h"
#include "suite.h"

using namespace maps;
using namespace maps::wiki;
using namespace maps::wiki::geom_tools;
using namespace maps::wiki::geom_tools::test;

TEST_SUITE_START(partitioner_tests, PolygonPartitionTestData)

TEST_DATA(simple_polygons)
{
    GeolibPolygonVector {
        geolib3::Polygon2 {{
            {1, 1}, {8, 1}, {8, 2}, {4, 2}, {4, 6}, {9, 6}, {9, 7}, {4, 7},
            {2, 6}, {2, 4}, {0, 3}, {1, 2}, {2, 2}, {1, 1}
        }},
        geolib3::Polygon2 {{
            {5, 3}, {8, 3}, {10, 1}, {11, 3}, {11, 5}, {9, 5}, {9, 4}, {7, 4},
            {7, 5}, {5, 5}, {6, 4}, {5, 3}
        }}
    },
    4,
    1.0,
    1e-3,
    GeolibPolygonVector {
        geolib3::Polygon2 {{ {2, 2}, {1, 1}, {8, 1}, {8, 2}, {2, 2} }},
        geolib3::Polygon2 {{ {9, 2}, {10, 1}, {10.5, 2}, {9, 2} }},
        geolib3::Polygon2 {{ {2, 6}, {2, 5}, {4, 5}, {4, 6}, {2, 6} }},
        geolib3::Polygon2 {{ {9, 6}, {9, 7}, {4, 7}, {2, 6}, {9, 6} }},
        geolib3::Polygon2 {{ {0, 3}, {1, 2}, {4, 2}, {4, 3}, {0, 3} }},
        geolib3::Polygon2 {{ {8, 3}, {9, 2}, {10.5, 2}, {11, 3}, {8, 3} }},
        geolib3::Polygon2 {{ {2, 4}, {0, 3}, {4, 3}, {4, 4}, {2, 4} }},
        geolib3::Polygon2 {{ {4, 4}, {4, 5}, {2, 5}, {2, 4}, {4, 4} }},
        geolib3::Polygon2 {{ {6, 4}, {5, 3}, {11, 3}, {11, 4}, {6, 4} }},
        geolib3::Polygon2 {{ {7, 4}, {7, 5}, {5, 5}, {6, 4}, {7, 4} }},
        geolib3::Polygon2 {{ {11, 4}, {11, 5}, {9, 5}, {9, 4}, {11, 4} }}
    }
};

TEST_DATA(simple_polygons_2)
{
    GeolibPolygonVector {
        geolib3::Polygon2 {{
            {1, 1}, {2, 1}, {2, 3}, {3, 3}, {3, 2}, {4, 2}, {4, 1}, {5, 1},
            {5, 3}, {4, 3}, {4, 4}, {3, 4}, {3, 5}, {5, 5}, {5, 6}, {3, 6},
            {3, 7}, {4, 7}, {4, 8}, {2, 8}, {2, 9}, {1, 9}, {1, 7}, {2, 7},
            {2, 4}, {1, 4}, {1, 1}
        }},
        geolib3::Polygon2 {{
            {6, 2}, {7, 2}, {7, 1}, {8, 1}, {8, 0}, {9, 0}, {9, 2}, {8, 2},
            {8, 3}, {7, 3}, {7, 4}, {6, 4}, {6, 2}
        }},
        geolib3::Polygon2 {{
            {5, 9}, {5, 8}, {6, 8}, {6, 7}, {7, 7}, {7, 5}, {8, 5}, {8, 6},
            {9, 6}, {9, 8}, {7, 8}, {7, 9}, {5, 9}
        }}
    },
    4,
    1.0,
    1e-3,
    GeolibPolygonVector {
        geolib3::Polygon2 {{ {7, 4}, {6, 4}, {6, 2}, {7, 2}, {7, 4} }},
        geolib3::Polygon2 {{ {3, 7}, {4, 7}, {4, 8}, {3, 8}, {3, 7} }},
        geolib3::Polygon2 {{ {3, 5}, {5, 5}, {5, 6}, {3, 6}, {3, 5} }},
        geolib3::Polygon2 {{ {8, 3}, {7, 3}, {7, 1}, {8, 1}, {8, 3} }},
        geolib3::Polygon2 {{ {8, 0}, {9, 0}, {9, 2}, {8, 2}, {8, 0} }},
        geolib3::Polygon2 {{ {8, 8}, {7, 8}, {7, 5}, {8, 5}, {8, 8} }},
        geolib3::Polygon2 {{ {8, 6}, {9, 6}, {9, 8}, {8, 8}, {8, 6} }},
        geolib3::Polygon2 {{ {2, 9}, {1, 9}, {1, 7}, {2, 7}, {2, 9} }},
        geolib3::Polygon2 {{ {2, 4}, {1, 4}, {1, 1}, {2, 1}, {2, 4} }},
        geolib3::Polygon2 {{ {2, 3}, {3, 3}, {3, 8}, {2, 8}, {2, 3} }},
        geolib3::Polygon2 {{ {4, 4}, {3, 4}, {3, 2}, {4, 2}, {4, 4} }},
        geolib3::Polygon2 {{ {4, 1}, {5, 1}, {5, 3}, {4, 3}, {4, 1} }},
        geolib3::Polygon2 {{ {6, 9}, {5, 9}, {5, 8}, {6, 8}, {6, 9} }},
        geolib3::Polygon2 {{ {6, 7}, {7, 7}, {7, 9}, {6, 9}, {6, 7} }}
    }
};

TEST_DATA(polygons_with_holes)
{
    GeolibPolygonVector {
        geolib3::Polygon2 {
            geolib3::LinearRing2 {{ {2, 6}, {5, 6}, {5, 9}, {2, 9} }},
            {
                geolib3::LinearRing2 {{ {3, 6.5}, {4, 8}, {3, 8} }}
            }
        },
        geolib3::Polygon2 {
            geolib3::LinearRing2 {{
                {3, 4}, {5, 3}, {8, 3}, {10, 5}, {10, 10}, {8, 10}, {8, 9},
                {7, 9}, {6, 7}, {6, 5}, {3, 5}
            }},
            {
                geolib3::LinearRing2 {{ {7, 6}, {9, 6}, {9, 8}, {7, 8} }},
                geolib3::LinearRing2 {{ {6, 4}, {8, 4}, {9, 5}, {7, 5} }}
            }
        },
        geolib3::Polygon2 {{ {1, 1}, {4, 1}, {4, 2}, {1, 3} }},
        geolib3::Polygon2 {{ {5, 1}, {9, 1}, {9, 2}, {12, 2}, {12, 4}, {10, 4}, {8, 2}, {5, 2} }}
    },
    4,
    1.0,
    1e-3,
    GeolibPolygonVector {
        geolib3::Polygon2 {{ {1, 1}, {4, 1}, {4, 2}, {1, 3}, {1, 1} }},
        geolib3::Polygon2 {{ {5, 5}, {3, 5}, {3, 4}, {5, 3}, {5, 5} }},
        geolib3::Polygon2 {{ {3, 9}, {2, 9}, {2, 6}, {3, 6}, {3, 9} }},
        geolib3::Polygon2 {{ {7, 2}, {5, 2}, {5, 1}, {7, 1}, {7, 2} }},
        geolib3::Polygon2 {{ {6, 5}, {5, 5}, {5, 3}, {6, 3}, {6, 5} }},
        geolib3::Polygon2 {{ {6, 3}, {7, 3}, {7, 4}, {6, 4}, {6, 3} }},
        geolib3::Polygon2 {{ {6, 4}, {7, 5}, {7, 9}, {6, 7}, {6, 4} }},
        geolib3::Polygon2 {{ {8, 9}, {7, 9}, {7, 8}, {8, 8}, {8, 9} }},
        geolib3::Polygon2 {{ {8, 6}, {7, 6}, {7, 5}, {8, 5}, {8, 6} }},
        geolib3::Polygon2 {{ {8, 4}, {7, 4}, {7, 3}, {8, 3}, {8, 4} }},
        geolib3::Polygon2 {{ {8, 2}, {7, 2}, {7, 1}, {8, 1}, {8, 2} }},
        geolib3::Polygon2 {{ {5, 8}, {5, 9}, {3, 9}, {3, 8}, {5, 8} }},
        geolib3::Polygon2 {{ {9, 6}, {8, 6}, {8, 5}, {9, 5}, {9, 6} }},
        geolib3::Polygon2 {{ {9, 5}, {8, 4}, {8, 3}, {9, 4}, {9, 5} }},
        geolib3::Polygon2 {{ {9, 10}, {8, 10}, {8, 8}, {9, 8}, {9, 10} }},
        geolib3::Polygon2 {{ {9, 4}, {10, 5}, {10, 10}, {9, 10}, {9, 4} }},
        geolib3::Polygon2 {{ {9, 3}, {8, 2}, {8, 1}, {9, 1}, {9, 3} }},
        geolib3::Polygon2 {{ {10, 4}, {9, 3}, {9, 2}, {10, 2}, {10, 4} }},
        geolib3::Polygon2 {{ {10, 2}, {12, 2}, {12, 4}, {10, 4}, {10, 2} }},
        geolib3::Polygon2 {{ {5, 7}, {5, 8}, {4, 8}, {3.3333333333333335, 7}, {5, 7} }},
        geolib3::Polygon2 {{ {3.3333333333333335, 7}, {3, 6.5}, {3, 6}, {5, 6}, {5, 7}, {3.3333333333333335, 7} }}
    }
};

TEST_DATA(polygons_min_size)
{
    GeolibPolygonVector {
        geolib3::Polygon2 {{{{0,1}, {11,0}, {29,0}, {30,15}, {30,29}, {17,30}, {1,30}, {0,15}}}}
    },
    4,
    11 + geolib3::EPS,
    geolib3::EPS,
    GeolibPolygonVector {
        geolib3::Polygon2{{{{17,15},{17,30},{1,30},{0,15},{17,15}}}},
        geolib3::Polygon2{{{{17,15},{17,0},{29,0},{30,15},{17,15}}}},
        geolib3::Polygon2{{{{30,15},{30,29},{17,30},{17,15},{30,15}}}},
        geolib3::Polygon2{{{{0,15},{0,1},{11,0},{17,0},{17,15},{0,15}}}}
    }
};

TEST_SUITE_END(partitioner_tests)
