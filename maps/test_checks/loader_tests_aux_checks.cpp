#define MODULE_NAME "loader-tests-aux"

#include <maps/wikimap/mapspro/libs/validator/common/categories_list.h>

#include <yandex/maps/wiki/validator/check.h>

namespace maps {
namespace wiki {
namespace validator {

using namespace categories;

namespace {

template<class Category, class NameCategory>
void checkNameLoading(CheckContext* context)
{
    context->objects<Category>().visit([&](const typename Category::TObject* object) {
        if (object->names().empty()) {
            context->error("no-names", boost::none, { object->id() });
        }
        for (const NameRelation& nameRel : object->names()) {
            if (!context->objects<NameCategory>().loaded(nameRel.id)) {
                context->error("name-missing", boost::none, { object->id(), nameRel.id });
            } else {
                std::string expectedName = "name_for_" + Category::id();
                if (context->objects<NameCategory>().byId(nameRel.id)->name()
                       != expectedName) {
                    context->error("wrong-name", boost::none, { object->id(), nameRel.id });
                }
            }
        }
    });
}

} // namespace


VALIDATOR_SIMPLE_CHECK( name_loading_aux, RD, RD_NM, AD, AD_NM, ADDR, ADDR_NM )
{
    checkNameLoading<RD, RD_NM>(context);
    checkNameLoading<AD, AD_NM>(context);
    checkNameLoading<ADDR, ADDR_NM>(context);
}

} // namespace validator
} // namespace wiki
} // namespace maps
