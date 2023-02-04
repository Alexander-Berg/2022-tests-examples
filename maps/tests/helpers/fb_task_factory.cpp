#include <maps/wikimap/mapspro/libs/social/tests/helpers/fb_task_factory.h>
#include <maps/libs/json/include/value.h>

namespace maps::wiki::social::feedback::tests {

namespace {

const geolib3::Point2 DEFAULT_POSITION    = {0, 0};
const Type            DEFAULT_TYPE        = feedback::Type::Road;
const std::string     DEFAULT_SOURCE      = "fbapi";
const Description     DEFAULT_DESCRIPTION = feedback::Description();

}  // unnamed namespace

FbTaskFactory::FbTaskFactory()
    : task_{}
{
    task_.position_ = DEFAULT_POSITION;
    task_.type_ = DEFAULT_TYPE;
    task_.source_ = DEFAULT_SOURCE;
    task_.description_ = DEFAULT_DESCRIPTION;
}

FbTaskFactory& FbTaskFactory::type(Type type)
{
    task_.type_ = type;
    return *this;
}

FbTaskFactory& FbTaskFactory::source(std::string source)
{
    task_.source_ = std::move(source);
    return *this;
}

FbTaskFactory& FbTaskFactory::id(TId id)
{
    task_.id_ = id;
    return *this;
}

FbTaskFactory& FbTaskFactory::position(const geolib3::Point2& position)
{
    task_.position_ = position;
    return *this;
}

FbTaskFactory& FbTaskFactory::attrs(const json::Value& jsonAttrs)
{
    task_.attrs_ = Attrs(jsonAttrs);
    return *this;
}

Task FbTaskFactory::create() const
{
    return task_;
}

} // namespace maps::wiki::social::feedback::tests
