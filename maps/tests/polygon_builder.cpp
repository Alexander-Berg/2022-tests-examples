#include "polygon_builder.h"
#include "suite.h"

using namespace maps;
using namespace maps::wiki;
using namespace maps::wiki::geom_tools;
using namespace maps::wiki::geom_tools::test;

TEST_SUITE_START(polygon_builder_tests, PolygonBuilderTestData)

TEST_DATA(simple_polygon)
{
    GeolibLinearRingVector {
        geolib3::LinearRing2 {{ {0, 0}, {10, 0}, {10, 10}, {0, 10}, {0, 0} }}
    },
    GeolibLinearRingVector {},
    GeolibPolygonVector {
        geolib3::Polygon2 {{ {0, 0}, {10, 0}, {10, 10}, {0, 10}, {0, 0} }}
    }
};

TEST_DATA(polygon_with_one_hole)
{
    GeolibLinearRingVector {
        geolib3::LinearRing2 {{ {0, 0}, {10, 0}, {10, 10}, {0, 10}, {0, 0} }}
    },
    GeolibLinearRingVector {
        geolib3::LinearRing2 {{ {1, 1}, {9, 1}, {9, 9}, {1, 9}, {1, 1} }}
    },
    GeolibPolygonVector {
        geolib3::Polygon2 {
            geolib3::LinearRing2 {{ {0, 0}, {10, 0}, {10, 10}, {0, 10}, {0, 0} }},
            {
                geolib3::LinearRing2 {{ {1, 1}, {9, 1}, {9, 9}, {1, 9}, {1, 1} }}
            }
        }
    }
};

TEST_DATA(two_polygons_with_one_hole_each)
{
    GeolibLinearRingVector {
        geolib3::LinearRing2 {{ {0, 0}, {10, 0}, {10, 10}, {0, 10}, {0, 0} }},
        geolib3::LinearRing2 {{ {20, 20}, {30, 20}, {30, 30}, {20, 30}, {20, 20} }}
    },
    GeolibLinearRingVector {
        geolib3::LinearRing2 {{ {1, 1}, {9, 1}, {9, 9}, {1, 9}, {1, 1} }},
        geolib3::LinearRing2 {{ {21, 21}, {29, 21}, {29, 29}, {21, 29}, {21, 21} }}
    },
    GeolibPolygonVector {
        geolib3::Polygon2 {
            geolib3::LinearRing2 {{ {0, 0}, {10, 0}, {10, 10}, {0, 10}, {0, 0} }},
            {
                geolib3::LinearRing2 {{ {1, 1}, {9, 1}, {9, 9}, {1, 9}, {1, 1} }}
            }
        },
        geolib3::Polygon2 {
            geolib3::LinearRing2 {{ {20, 20}, {30, 20}, {30, 30}, {20, 30}, {20, 20} }},
            {
                geolib3::LinearRing2 {{ {21, 21}, {29, 21}, {29, 29}, {21, 29}, {21, 21} }}
            }
        }
    }
};

TEST_DATA(nested_polygons)
{
    GeolibLinearRingVector {
        geolib3::LinearRing2 {{ {0, 0}, {10, 0}, {10, 10}, {0, 10}, {0, 0} }},
        geolib3::LinearRing2 {{ {2, 2}, {8, 2}, {8, 8}, {2, 8}, {2, 2} }},
        geolib3::LinearRing2 {{ {4, 4}, {6, 4}, {6, 6}, {4, 6}, {4, 4} }}
    },
    GeolibLinearRingVector {
        geolib3::LinearRing2 {{ {1, 1}, {9, 1}, {9, 9}, {1, 9}, {1, 1} }},
        geolib3::LinearRing2 {{ {3, 3}, {7, 3}, {7, 7}, {3, 7}, {3, 3} }}
    },
    GeolibPolygonVector {
        geolib3::Polygon2 {
            geolib3::LinearRing2 {{ {0, 0}, {10, 0}, {10, 10}, {0, 10}, {0, 0} }},
            {
                geolib3::LinearRing2 {{ {1, 1}, {9, 1}, {9, 9}, {1, 9}, {1, 1} }}
            }
        },
        geolib3::Polygon2 {
            geolib3::LinearRing2 {{ {2, 2}, {8, 2}, {8, 8}, {2, 8}, {2, 2} }},
            {
                geolib3::LinearRing2 {{ {3, 3}, {7, 3}, {7, 7}, {3, 7}, {3, 3} }}
            }
        },
        geolib3::Polygon2 {{ {4, 4}, {6, 4}, {6, 6}, {4, 6}, {4, 4} }}
    }
};

TEST_DATA(hole_touches_exterior)
{
    GeolibLinearRingVector {
        geolib3::LinearRing2 {{ {0, 0}, {10, 0}, {10, 10}, {0, 10}, {0, 0} }},
        geolib3::LinearRing2 {{ {4, 4}, {6, 4}, {6, 6}, {4, 6}, {4, 4} }}
    },
    GeolibLinearRingVector {
        geolib3::LinearRing2 {{ {0, 0}, {1, 2}, {2, 1}, {0, 0} }}
    },
    GeolibPolygonVector {
        geolib3::Polygon2 {
            geolib3::LinearRing2 {{ {0, 0}, {10, 0}, {10, 10}, {0, 10}, {0, 0} }},
            {
                geolib3::LinearRing2 {{ {0, 0}, {1, 2}, {2, 1}, {0, 0} }}
            }
        },
        geolib3::Polygon2 {{ {4, 4}, {6, 4}, {6, 6}, {4, 6}, {4, 4} }}
    }
};

TEST_DATA(hole_touches_interior)
{
    GeolibLinearRingVector {
        geolib3::LinearRing2{{{0, 0}, {5, 0}, {5, 5}, {0, 5}}}
    },
    GeolibLinearRingVector {
        geolib3::LinearRing2{{{1, 1}, {1, 2}, {2, 2}, {2, 1}}},
        geolib3::LinearRing2{{{2, 1}, {3, 2}, {3, 1}}}
    },
    GeolibPolygonVector {
        geolib3::Polygon2 {
            geolib3::LinearRing2{{{0, 0}, {5, 0}, {5, 5}, {0, 5}}},
            {   geolib3::LinearRing2{{{1, 1}, {1, 2}, {2, 2}, {2, 1}}},
                geolib3::LinearRing2{{{2, 1}, {3, 2}, {3, 1}}}
            }
        }
    }
};

TEST_SUITE_END(polygon_builder_tests)
