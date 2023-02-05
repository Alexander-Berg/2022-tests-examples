#include <maps/wikimap/feedback/api/src/synctool/tests/helpers/printers.h>

#include <ostream>

template <>
void Out<PushTypeToTranslations>(
    IOutputStream& os,
    const PushTypeToTranslations& pushTypeToTranslations)
{
    for (const auto& [key, value] : pushTypeToTranslations) {
        std::ostringstream translationsStream;
        translationsStream << value;
        os << key << ": {" << translationsStream.str() << "}\n";
    }
}

template <>
void Out<SubjectTranslations>(
    IOutputStream& os,
    const SubjectTranslations& subjectTranslations)
{
    for (const auto& [lang, subject] : subjectTranslations) {
        os << lang << ": {" << subject << "}\n";
    }
}
