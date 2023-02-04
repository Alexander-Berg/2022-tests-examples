#include "cutter.h"
#include "suite.h"

using namespace maps;
using namespace maps::wiki;
using namespace maps::wiki::geom_tools;
using namespace maps::wiki::geom_tools::test;

TEST_SUITE_START(cutter_tests, PolygonCutterTestData)

TEST_DATA(test_1_1)
{
    geolib3::Polygon2 {{ {0, 2}, {-1, 2}, {-1, 0}, {1, 0}, {1, 2} }},
    CutLine {Direction::X, 0, geolib3::EPS},
    PolygonCutter::Result {
        { geolib3::Polygon2 {{ {0, 2}, {-1, 2}, {-1, 0}, {0, 0} }} },
        { geolib3::Polygon2 {{ {0, 0}, {1, 0}, {1, 2}, {0, 2} }} }
    }
};

TEST_DATA(test_1_2)
{
    geolib3::Polygon2 {{ {0, 2}, {-2, 2}, {-2, 0}, {2, 0}, {2, 1}, {-1, 1} }},
    CutLine {Direction::X, 0, geolib3::EPS},
    PolygonCutter::Result {
        { geolib3::Polygon2 {{ {0, 2}, {-2, 2}, {-2, 0}, {0, 0}, {0, 1}, {-1, 1} }} },
        { geolib3::Polygon2 {{ {0, 0}, {2, 0}, {2, 1}, {0, 1} }} }
    }
};

TEST_DATA(test_1_3)
{
    geolib3::Polygon2 {{ {0, 2}, {-2, 2}, {-2, 0}, {2, 0}, {2, 1}, {0, 1} }},
    CutLine {Direction::X, 0, geolib3::EPS},
    PolygonCutter::Result {
        { geolib3::Polygon2 {{ {0, 2}, {-2, 2}, {-2, 0}, {0, 0} }} },
        { geolib3::Polygon2 {{ {0, 0}, {2, 0}, {2, 1}, {0, 1} }} }
    }
};

TEST_DATA(test_1_4)
{
    geolib3::Polygon2 {{ {2, 2}, {-2, 2}, {-2, 0}, {2, 0} }},
    CutLine {Direction::X, 0, geolib3::EPS},
    PolygonCutter::Result {
        { geolib3::Polygon2 {{ {0, 2}, {-2, 2}, {-2, 0}, {0, 0} }} },
        { geolib3::Polygon2 {{ {0, 0}, {2, 0}, {2, 2}, {0, 2} }} }
    }
};

TEST_DATA(test_1_5)
{
    geolib3::Polygon2 {{ {0, 3}, {-2, 3}, {-2, 0}, {2, 0}, {2, 1}, {0, 1}, {0, 2} }},
    CutLine {Direction::X, 0, geolib3::EPS},
    PolygonCutter::Result {
        { geolib3::Polygon2 {{ {0, 3}, {-2, 3}, {-2, 0}, {0, 0} }} },
        { geolib3::Polygon2 {{ {0, 0}, {2, 0}, {2, 1}, {0, 1} }} }
    }
};

TEST_DATA(test_1_6)
{
    geolib3::Polygon2 {{ {0, 4}, {-2, 4}, {-2, 0}, {2, 0}, {2, 1}, {-1, 1}, {-1, 2}, {0, 2}, {0, 3} }},
    CutLine {Direction::X, 0, geolib3::EPS},
    PolygonCutter::Result {
        { geolib3::Polygon2 {{ {0, 4}, {-2, 4}, {-2, 0}, {0, 0}, {0, 1}, {-1, 1}, {-1, 2}, {0, 2} }} },
        { geolib3::Polygon2 {{ {0, 0}, {2, 0}, {2, 1}, {0, 1} }} }
    }
};

TEST_DATA(test_1_7)
{
    geolib3::Polygon2 {{ {0, 0}, {3, 0}, {3, 4}, {1.5, 4}, {1.5, 1.5}, {2, 2}, {2, 1}, {1, 1}, {1, 4}, {0, 4} }},
    CutLine {Direction::Y, 2, geolib3::EPS},
    PolygonCutter::Result {
        {
            geolib3::Polygon2 {{ {1.5, 2}, {1.5, 1.5}, {2, 2} }},
            geolib3::Polygon2 {{ {2, 2}, {2, 1}, {1, 1}, {1, 2}, {0, 2}, {0, 0}, {3, 0}, {3, 2} }}
        },
        {
            geolib3::Polygon2 {{ {3, 2}, {3, 4}, {1.5, 4}, {1.5, 2} }},
            geolib3::Polygon2 {{ {1, 2}, {1, 4}, {0, 4}, {0, 2} }}
        }
    }
};

TEST_DATA(test_2_1)
{
    geolib3::Polygon2 {
        geolib3::LinearRing2 {{{-4, -4}, {4, -4}, {4, 4}, {-4, 4}, {-4, -4}}},
        {
            geolib3::LinearRing2 {{{-2, -2}, {-2, 2}, {2, 2}, {2, -2}, {-2, -2}}}
        }
    },
    CutLine {Direction::X, 0, geolib3::EPS},
    PolygonCutter::Result {
        { geolib3::Polygon2 {{ {-4, -4}, {0, -4}, {0, -2}, {-2, -2}, {-2, 2}, {0, 2}, {0, 4}, {-4, 4}, {-4, -4} }} },
        { geolib3::Polygon2 {{ {0, -4}, {4, -4}, {4, 4}, {0, 4}, {0, 2}, {2, 2}, {2, -2}, {0, -2}, {0, -4} }} }
    }
};

TEST_DATA(test_2_2)
{
    geolib3::Polygon2 {
        geolib3::LinearRing2 {{{2, 1}, {7, 1}, {7, 7}, {2, 7}}},
        {
            geolib3::LinearRing2 {{{3, 2}, {6, 2}, {4, 4}, {6, 6}, {3, 6}}}
        }
    },
    CutLine {Direction::X, 4, geolib3::EPS},
    PolygonCutter::Result {
        { geolib3::Polygon2 {{ {2, 1}, {4, 1}, {4, 2}, {3, 2}, {3, 6}, {4, 6}, {4, 7}, {2, 7} }} },
        { geolib3::Polygon2 {{ {4, 1}, {7, 1}, {7, 7}, {4, 7}, {4, 6}, {6, 6}, {4, 4}, {6, 2}, {4, 2} }} }
    }
};

TEST_DATA(test_2_3)
{
    geolib3::Polygon2 {
        geolib3::LinearRing2 {{{2, 1}, {7, 1}, {7, 8}, {2, 8}}},
        {
            geolib3::LinearRing2 {{{3, 2}, {6, 2}, {6, 7}, {5, 7}, {4, 6}, {5, 5}, {3, 3}}}
        }
    },
    CutLine {Direction::X, 4, geolib3::EPS},
    PolygonCutter::Result {
        { geolib3::Polygon2 {{ {2, 1}, {4, 1}, {4, 2}, {3, 2}, {3, 3}, {4, 4}, {4, 8}, {2, 8} }} },
        {
            geolib3::Polygon2 {{ {4, 1}, {7, 1}, {7, 8}, {4, 8}, {4, 6}, {5, 7}, {6, 7}, {6, 2}, {4, 2} }},
            geolib3::Polygon2 {{ {4, 4}, {5, 5}, {4, 6} }}
        }
    }
};

TEST_DATA(test_2_4)
{
    geolib3::Polygon2 {
        geolib3::LinearRing2 {{{2, 1}, {7, 1}, {7, 8}, {2, 8}}},
        {
            geolib3::LinearRing2 {{{4, 2}, {6, 2}, {6, 7}, {5, 7}, {4, 6}, {5, 5}, {4, 4}}}
        }
    },
    CutLine {Direction::X, 4, geolib3::EPS},
    PolygonCutter::Result {
        { geolib3::Polygon2 {{ {2, 1}, {4, 1}, {4, 8}, {2, 8} }} },
        {
            geolib3::Polygon2 {{ {4, 1}, {7, 1}, {7, 8}, {4, 8}, {4, 6}, {5, 7}, {6, 7}, {6, 2}, {4, 2} }},
            geolib3::Polygon2 {{ {4, 4}, {5, 5}, {4, 6} }}
        }
    }
};

TEST_DATA(test_2_5)
{
    geolib3::Polygon2 {
        geolib3::LinearRing2 {{{1, 1}, {7, 1}, {7, 8}, {1, 8}}},
        {
            geolib3::LinearRing2 {{{2, 2}, {6, 2}, {4, 4}}},
            geolib3::LinearRing2 {{{2, 7}, {4, 4}, {6, 7}}}
        }
    },
    CutLine {Direction::X, 4, geolib3::EPS},
    PolygonCutter::Result {
        { geolib3::Polygon2 {{ {1, 1}, {4, 1}, {4, 2}, {2, 2}, {4, 4}, {2, 7}, {4, 7}, {4, 8}, {1, 8} }} },
        { geolib3::Polygon2 {{ {4, 1}, {7, 1}, {7, 8}, {4, 8}, {4, 7}, {6, 7}, {4, 4}, {6, 2}, {4, 2} }} }
    }
};

// Hole touches cut line in two points and creates two borders which have both common endpoints
TEST_DATA(test_2_6_0)
{
    geolib3::Polygon2 {
        geolib3::LinearRing2 {{{1, 1}, {8, 1}, {8, 9}, {1, 9}}},
        {
            geolib3::LinearRing2 {{{7, 4}, {7, 8}, {4, 7}, {6, 6}, {4, 5}, {6, 4}}}
        }
    },
    CutLine {Direction::X, 4, geolib3::EPS},
    PolygonCutter::Result {
        { geolib3::Polygon2 {{ {1, 1}, {4, 1}, {4, 9}, {1, 9} }} },
        {
            geolib3::Polygon2 {{ {4, 5}, {4, 1}, {8, 1}, {8, 9}, {4, 9}, {4, 7}, {7, 8}, {7, 4}, {6, 4} }},
            geolib3::Polygon2 {{ {4, 7}, {6, 6}, {4, 5} }}
        }
    }
};

TEST_DATA(test_2_6_1)
{
    geolib3::Polygon2 {
        geolib3::LinearRing2 {{{1, 1}, {8, 1}, {8, 9}, {1, 9}}},
        {
            geolib3::LinearRing2 {{{7, 2}, {7, 8}, {4, 7}, {6, 6}, {4, 5}, {6, 4}, {4, 3}}}
        }
    },
    CutLine {Direction::X, 4, geolib3::EPS},
    PolygonCutter::Result {
        { geolib3::Polygon2 {{ {1, 1}, {4, 1}, {4, 9}, {1, 9} }} },
        {
            geolib3::Polygon2 {{ {4, 1}, {8, 1}, {8, 9}, {4, 9}, {4, 7}, {7, 8}, {7, 2}, {4, 3} }},
            geolib3::Polygon2 {{ {4, 7}, {6, 6}, {4, 5} }},
            geolib3::Polygon2 {{ {4, 5}, {6, 4}, {4, 3} }}
        }
    }
};

TEST_DATA(test_3_1)
{
    geolib3::Polygon2 {
        geolib3::LinearRing2 {{{1, 1}, {7, 1}, {7, 7}, {1, 7}}},
        {
            geolib3::LinearRing2 {{{4, 2}, {6, 4}, {4, 5}}},
            geolib3::LinearRing2 {{{4, 5}, {6, 5}, {6, 6}, {4, 7}}}
        }
    },
    CutLine {Direction::X, 4, geolib3::EPS},
    PolygonCutter::Result {
        { geolib3::Polygon2 {{ {1, 1}, {4, 1}, {4, 7}, {1, 7} }} },
        { geolib3::Polygon2 {{ {4, 1}, {7, 1}, {7, 7}, {4, 7}, {6, 6}, {6, 5}, {4, 5}, {6, 4}, {4, 2} }} }
    }
};

TEST_DATA(test_3_1_2)
{
    geolib3::Polygon2 {
        geolib3::LinearRing2 {{{1, 1}, {7, 1}, {7, 8}, {1, 8}}},
        {
            geolib3::LinearRing2 {{{4, 1}, {6, 3}, {4, 3}}},
            geolib3::LinearRing2 {{{4, 3}, {6, 5}, {4, 5}}}
        }
    },
    CutLine {Direction::X, 4, geolib3::EPS},
    PolygonCutter::Result {
        { geolib3::Polygon2 {{ {1, 1}, {4, 1}, {4, 8}, {1, 8} }} },
        { geolib3::Polygon2 {{ {4, 1}, {7, 1}, {7, 8}, {4, 8}, {4, 5}, {6, 5}, {4, 3}, {6, 3} }} }
    }
};

TEST_DATA(test_3_2)
{
    geolib3::Polygon2 {
        geolib3::LinearRing2 {{{1, 1}, {8, 1}, {8, 9}, {4, 7}, {1, 7}}},
        {
            geolib3::LinearRing2 {{{4, 2}, {7, 2}, {7, 7}, {4, 7}, {6, 6}, {6, 3}, {4, 3}}}
        }
    },
    CutLine {Direction::X, 4, geolib3::EPS},
    PolygonCutter::Result {
        { geolib3::Polygon2 {{ {1, 1}, {4, 1}, {4, 7}, {1, 7} }} },
        {
            geolib3::Polygon2 {{ {4, 1}, {8, 1}, {8, 9}, {4, 7}, {7, 7}, {7, 2}, {4, 2} }},
            geolib3::Polygon2 {{ {4, 7}, {6, 6}, {6, 3}, {4, 3} }}
        }
    }
};

TEST_DATA(test_3_2_2)
{
    geolib3::Polygon2 {
        geolib3::LinearRing2 {{{2, 2}, {4, 2}, {8, 1}, {8, 9}, {2, 9}}},
        {
            geolib3::LinearRing2 {{{4, 2}, {7, 2}, {7, 7}, {4, 7}, {4, 5}, {6, 5}, {6, 3}}}
        }
    },
    CutLine {Direction::X, 4, geolib3::EPS},
    PolygonCutter::Result {
        { geolib3::Polygon2 {{ {2, 2}, {4, 2}, {4, 9}, {2, 9} }} },
        {
            geolib3::Polygon2 {{ {4, 2}, {8, 1}, {8, 9}, {4, 9}, {4, 7}, {7, 7}, {7, 2} }},
            geolib3::Polygon2 {{ {4, 2}, {6, 3}, {6, 5}, {4, 5} }}
        }
    }
};


TEST_DATA(test_4_1)
{
    geolib3::Polygon2 {
        geolib3::LinearRing2 {{{1, 8}, {1, 3}, {5, 3}, {4, 2}, {4, 1}, {7, 1}, {7, 8}}},
        {
            geolib3::LinearRing2 {{{4, 4}, {6, 4}, {6, 6}, {4, 6}}}
        }
    },
    CutLine {Direction::X, 5, geolib3::EPS},
    PolygonCutter::Result {
        {
            geolib3::Polygon2 {{ {5, 8}, {1, 8}, {1, 3}, {5, 3}, {5, 4}, {4, 4}, {4, 6}, {5, 6} }},
            geolib3::Polygon2 {{ {5, 3}, {4, 2}, {4, 1}, {5, 1} }}
        },
        {
            geolib3::Polygon2 {{ {5, 1}, {7, 1}, {7, 8}, {5, 8}, {5, 6}, {6, 6}, {6, 4}, {5, 4} }}
        }
    }
};

// Bug with comparison of border parts with one shared endpoint
TEST_DATA(test_4_2)
{
    geolib3::Polygon2 {{ {1, 10}, {1, 2}, {7, 2}, {7, 9}, {4, 9}, {4, 7}, {5, 6}, {3, 6}, {3, 10} }},
    CutLine {Direction::X, 5, geolib3::EPS},
    PolygonCutter::Result {
        {
            geolib3::Polygon2 {{ {1, 10}, {1, 2}, {5, 2}, {5, 6}, {3, 6}, {3, 10} }},
            geolib3::Polygon2 {{ {4, 9}, {4, 7}, {5, 6}, {5, 9} }}
        },
        {
            geolib3::Polygon2 {{ {5, 2}, {7, 2}, {7, 9}, {5, 9} }}
        }
    }
};

TEST_DATA(test_touch_cut_line)
{
    geolib3::Polygon2 {{
        {6, 9}, {6, 7}, {8, 6}, {6, 5}, {6, 4}, {8, 4}, {8, 3}, {9, 2},
        {6, 2}, {6, 1}, {10, 1}, {10, 10}, {8, 10}
    }},
    CutLine {Direction::X, 8, 0.1},
    PolygonCutter::Result {
        {
            geolib3::Polygon2 {{ {8, 10}, {6, 9}, {6, 7}, {8, 6} }},
            geolib3::Polygon2 {{ {8, 6}, {6, 5}, {6, 4}, {8, 4} }},
            geolib3::Polygon2 {{ {8, 2}, {6, 2}, {6, 1}, {8, 1} }}
        },
        { geolib3::Polygon2 {{ {8, 10}, {8, 3}, {9, 2}, {8, 2}, {8, 1}, {10, 1}, {10, 10} }} }
    }
};

TEST_DATA(test_holes_touch_cut_line)
{
    geolib3::Polygon2 {
        geolib3::LinearRing2 {{{1, 1}, {19, 1}, {19, 15}, {1, 15}}},
        {
            geolib3::LinearRing2 {{{2, 10}, {6, 10}, {6, 14}, {2, 14}}}, // less
            geolib3::LinearRing2 {{{8, 2}, {10, 2}, {10, 6}, {8, 6}}}, // less, touches cut line by segment
            geolib3::LinearRing2 {{{8, 8}, {10, 9}, {8, 10}}}, // less, touches cut line by point
            geolib3::LinearRing2 {{{14, 2}, {18, 2}, {18, 6}, {14, 6}}}, // greater
            geolib3::LinearRing2 {{{10, 10}, {12, 10}, {12, 14}, {10, 14}}}, // greater, touches cut line by segment
            geolib3::LinearRing2 {{{10, 7}, {12, 6}, {12, 8}}} // greater, touches cut line by point
        }
    },
    CutLine {Direction::X, 10, 0.1},
    PolygonCutter::Result {
        {
            geolib3::Polygon2 {
                geolib3::LinearRing2 {{{1, 1}, {10, 1}, {10, 2}, {8, 2}, {8, 6}, {10, 6}, {10, 15}, {1, 15}}},
                {
                    geolib3::LinearRing2 {{{2, 10}, {6, 10}, {6, 14}, {2, 14}}},
                    geolib3::LinearRing2 {{{8, 8}, {9.95, 9}, {8, 10}}}
                }
            }
        },
        {
            geolib3::Polygon2 {
                geolib3::LinearRing2 {{{10, 1}, {19, 1}, {19, 15}, {10, 15}, {10, 14}, {12, 14}, {12, 10}, {10, 10}}},
                {
                    geolib3::LinearRing2 {{{14, 2}, {18, 2}, {18, 6}, {14, 6}}},
                    geolib3::LinearRing2 {{{10.05, 7}, {12, 6}, {12, 8}}}
                }
            }
        }
    }
};

TEST_DATA(test_multiple_cuts)
{
    geolib3::Polygon2 {{
        {1, 17}, {17, 17}, {17, 3}, {5, 3}, {5, 13}, {13, 13}, {13, 7},
        {9, 7}, {9, 9}, {11, 9}, {11, 11}, {7, 11}, {7, 5}, {15, 5},
        {15, 15}, {3, 15}, {3, 1}, {19, 1}, {19, 19}, {1, 19}
    }},
    CutLine {Direction::X, 10, geolib3::EPS},
    PolygonCutter::Result {
        {
            geolib3::Polygon2({{1, 19}, {10, 19}, {10, 17}, {1, 17}}),
            geolib3::Polygon2({{10, 15}, {3, 15}, {3, 1}, {10, 1}, {10, 3}, {5, 3}, {5, 13}, {10, 13}}),
            geolib3::Polygon2({{10, 11}, {7, 11}, {7, 5}, {10, 5}, {10, 7}, {9, 7}, {9, 9}, {10, 9}})
        },
        {
            geolib3::Polygon2({{10, 19}, {19, 19}, {19, 1}, {10, 1}, {10, 3}, {17, 3}, {17, 17}, {10, 17}}),
            geolib3::Polygon2({{10, 15}, {15, 15}, {15, 5}, {10, 5}, {10, 7}, {13, 7}, {13, 13}, {10, 13}}),
            geolib3::Polygon2({{10, 9}, {11, 9}, {11, 11}, {10, 11}})
        }
    }
};

TEST_DATA(test_arm_cuts)
{
    geolib3::Polygon2 {
        geolib3::LinearRing2 {{{0, 0}, {9, 0}, {9, 15}, {0, 15}, {0, 12}, {6, 12}, {6, 9 - geolib3::EPS / 4.}, {0, 9 - geolib3::EPS / 4.}}},
        {   geolib3::LinearRing2 {{{3, 3}, {3, 6}, {6, 6}, {6, 3}}}
        }
    },
    CutLine {Direction::Y, 9, geolib3::EPS},
    PolygonCutter::Result {
        {   geolib3::Polygon2 {
                geolib3::LinearRing2 {{{0, 0}, {9, 0}, {9, 9}, {0, 9}}},
                {   geolib3::LinearRing2 {{{3, 3}, {3, 6}, {6, 6}, {6, 3}}}
                }
            }
        },
        {   geolib3::Polygon2({{6, 9}, {9, 9}, {9, 15}, {0, 15}, {0, 12}, {6, 12}})
        }
    }
};

TEST_SUITE_END(cutter_tests)
