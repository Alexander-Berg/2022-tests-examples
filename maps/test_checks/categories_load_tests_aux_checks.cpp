#define MODULE_NAME "categories-load-tests-aux"

#include <maps/wikimap/mapspro/libs/validator/common/categories_list.h>

#include <yandex/maps/wiki/validator/check.h>

namespace maps {
namespace wiki {
namespace validator {

using namespace categories;

template<class Category>
struct LoadCheck : ICheckPart
{
    void run(CheckContext* context) override
    {
        typedef typename Category::TObject TObject;

        context->objects<Category>().visit([&](const TObject* object) {
            context->warning(Category::id(), boost::none, {object->id()});
        });
    }

    static std::vector<TCategoryId> categoryDependencies()
    { return { Category::id() }; }
};

template<class T> struct LoadCheckRegistrar;

template<class... Categories>
struct LoadCheckRegistrar<TypeList<Categories...>>
{
    LoadCheckRegistrar()
    { std::vector<bool> loaded = { registerLoadCheckPart<Categories>()... }; }

    template<class Category>
    bool registerLoadCheckPart()
    {
        globalCheckRegistry().registerCheckPart<LoadCheck<Category>>(
            MODULE_NAME, "categories_load", Category::id());
        return true;
    }
};

LoadCheckRegistrar<CategoriesList> s_registrar;

} // namespace validator
} // namespace wiki
} // namespace maps
