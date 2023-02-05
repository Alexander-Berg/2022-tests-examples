#include <yandex/maps/navi/obj_mesh.h>

#include <yandex/maps/runtime/exception.h>

#include <boost/test/unit_test.hpp>

#include <ostream>
#include <string>
#include <vector>

using yandex::maps::runtime::RuntimeError;

namespace boost::test_tools::tt_detail {

template<>
struct print_log_value<yandex::maps::navi::ObjMesh::Face> {
    void operator()(std::ostream& os, const yandex::maps::navi::ObjMesh::Face& face)
    {
        os << face[0] << " " << face[1] << " " << face[2];
    }
};

} // namespace boost


namespace yandex::maps::navi {

std::ostream& operator<<(std::ostream& stream, const ObjMesh::Face& face)
{
    return stream << face[0] << " " << face[1] << " " << face[2];
}

namespace {

template<class Elem>
boost::test_tools::predicate_result comparePoints(
    const std::vector<Elem>& one, const std::vector<Elem>& two)
{
    boost::test_tools::predicate_result result{ true };
    auto sameSize = one.size() == two.size();
    if (sameSize) {
        for (size_t ind = 0; ind < one.size(); ++ind) {
            auto first = one[ind][0];
            auto second = two[ind][0];
            auto single_result = boost::test_tools::check_is_close(one[ind][0], two[ind][0], 1);
            if (!single_result) {
                result = false;
                result.message() << "\ndiffers at index: " << ind <<
                                    ", needs to be " << first <<
                                    " but in fact is " << second;
            }
        }
    } else {
        result = false;
        result.message() << "collections of points have different sizes";
    }
    return result;
}

void performTest(const ObjMesh& mesh)
{
    std::vector<ObjMesh::Position> positions = {
        {{1 ,1 ,1}},
        {{2, 2, 2}},
        {{3, 3, 3}},
        {{4, 4, 4}}
    };
    std::vector<ObjMesh::TextureCoordinate> texCoords = {
        {{11, 11}},
        {{22, 22}},
        {{33, 33}},
        {{44, 44}}
    };
    std::vector<ObjMesh::Face> faces = {
        {{0, 1, 2}},
        {{0, 2, 3}},
        {{0, 3, 1}},
        {{1, 3, 2}}
    };
    BOOST_CHECK_MESSAGE(comparePoints(positions, mesh.positions), "positions are different");
    BOOST_CHECK_MESSAGE(comparePoints(texCoords, mesh.textureCoordinates),
        "texture coordinates are different");
    BOOST_CHECK_EQUAL_COLLECTIONS(faces.begin(), faces.end(),
        mesh.faces.begin(), mesh.faces.end());
}

}

BOOST_AUTO_TEST_CASE(load_tetrahedron)
{
    const std::string file = ""
    "v 1 1 1\n"
    "v 2 2 2\n"
    "v 3 3 3\n"
    "v 4 4 4\n"
    "vt 11 11\n"
    "vt 22 22\n"
    "vt 33 33\n"
    "vt 44 44\n"
    "f 1/1 2/2 3/3\n"
    "f 1/1 3/3 4/4\n"
    "f 1/1 4/4 2/2\n"
    "f 2/2 4/4 3/3\n";
    auto mesh = loadObj(file);
    performTest(mesh);
}

BOOST_AUTO_TEST_CASE(load_tetrahedron_with_ignored_normal_indexes)
{
    const std::string file = ""
    "v 1 1 1\n"
    "v 2 2 2\n"
    "v 3 3 3\n"
    "v 4 4 4\n"
    "vt 11 11\n"
    "vt 22 22\n"
    "vt 33 33\n"
    "vt 44 44\n"
    "f 1/1/1 2/2/1 3/3/1\n"
    "f 1/1/1 3/3/1 4/4/1\n"
    "f 1/1/1 4/4/1 2/2/1\n"
    "f 2/2/1 4/4/1 3/3/1\n";
    auto mesh = loadObj(file);
    performTest(mesh);
}

BOOST_AUTO_TEST_CASE(load_tetrahedron_ignore_irrelevant)
{
    const std::string file = ""
    "v 1 1 1\n"
    "v 2 2 2\n"
    "v 3 3 3 #some comment\n"
    "v 4 4 4\n"
    "vt 11 11\n"
    "#next 3 lines are valid obj, but we do not consider them"
    "vn 0 0 0\n"
    "newmat\n"
    "o\n"
    "vt 22 22\n"
    "vt 33 33\n"
    "vt 44 44\n"
    "f 1/1 2/2 3/3\n"
    "f 1/1 3/3 4/4\n"
    "f 1/1 4/4 2/2\n"
    "f 2/2 4/4 3/3\n";
    auto mesh = loadObj(file);
    performTest(mesh);
}

BOOST_AUTO_TEST_CASE(load_tetrahedron_interleaved_lines)
{
    const std::string file = ""
    "v 1 1 1\n"
    "vt 11 11\n"
    "v 2 2 2\n"
    "vt 22 22\n"
    "v 3 3 3\n"
    "vt 33 33\n"
    "f 1/1 2/2 3/3\n"
    "v 4 4 4\n"
    "vt 44 44\n"
    "f 1/1 3/3 4/4\n"
    "f 1/1 4/4 2/2\n"
    "f 2/2 4/4 3/3\n";
    auto mesh = loadObj(file);
    performTest(mesh);
}

BOOST_AUTO_TEST_CASE(load_tetrahedron_negative_indicies)
{
    const std::string file = ""
    "v 1 1 1\n"
    "v 2 2 2\n"
    "v 3 3 3\n"
    "v 4 4 4\n"
    "vt 11 11\n"
    "vt 22 22\n"
    "vt 33 33\n"
    "vt 44 44\n"
    "f -4/-4 2/2 3/3\n"
    "f 1/1 3/-2 -1/-1\n"
    "f -4/1 4/4 2/2\n"
    "f -3/-3 -1/-1 -2/-2\n";
    auto mesh = loadObj(file);
    performTest(mesh);
}

BOOST_AUTO_TEST_CASE(fail_to_load_ill_formed)
{
    std::string file = "v 1\n";
    BOOST_CHECK_THROW(loadObj(file), RuntimeError );
    file = "v 1 1 1 v 2 2 2";
    BOOST_CHECK_THROW(loadObj(file), RuntimeError );
    file = "vt 1 1 1\n";
    BOOST_CHECK_THROW(loadObj(file), RuntimeError );
    file = "v 1 1 1\n f 1 1 1 1\n";
    BOOST_CHECK_THROW(loadObj(file), RuntimeError );
}

BOOST_AUTO_TEST_CASE(fail_to_load_index_out_of_bound)
{
    std::string file = "v 1 1 1\n vt 1 1\n f 2/2 2/2 2/2\n";
    BOOST_CHECK_THROW(loadObj(file), RuntimeError );
    file = "v 1 1 1\n vt 1 1\nf -2/-2 -2/-2 -2/-2\n";
    BOOST_CHECK_THROW(loadObj(file), RuntimeError );
}

}
