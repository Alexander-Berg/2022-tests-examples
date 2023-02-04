#include "../include/TestConfirmationProvider.h"
#include <yandex/maps/renderer5/core/syspaths.h>
#include <yandex/maps/renderer/io/io.h>

using namespace maps::renderer;
using namespace maps::renderer5;
using namespace maps::renderer5::test;
using namespace maps::renderer5::test::map;

AddPostgresLayerConfirmation::AddPostgresLayerConfirmation() {}

void AddPostgresLayerConfirmation::confirm()
{
    setFeatureType(featureType);
    setTableName(schemaName, tableName);
    setIdColumnName(idColumnName());
    setGeometryColumnName();
    setFilterClause(filterExpr);
    setCreate(true);
}

void AddPostgresLayerConfirmation::reset()
{
    featureType = core::FeatureType::Null;
    schemaName = L"";
    tableName = L"";
    filterExpr = L"";
}

TestIdColumnNameConfirmation::TestIdColumnNameConfirmation() {}

void TestIdColumnNameConfirmation::confirm()
{
    REN_LOG_WARNING() << "Using answer \"" << idColumnName() << L"\" for question \""
        << m_text << L"\"";
}

std::wstring& TestIdColumnNameConfirmation::idColumnName()
{
    return m_idColumnName;
}

TestSaveXmlFileConfirmation::TestSaveXmlFileConfirmation()
{
    m_filename = io::tempDirPath() + "/map.xml";
}

TestSaveXmlFileConfirmation::~TestSaveXmlFileConfirmation()
{}

core::IExternalConfirmation::ExternalConfirmationType TestSaveXmlFileConfirmation::type() const
{
    return core::IExternalConfirmation::SaveFile;
}

const std::string& TestSaveXmlFileConfirmation::filename() const
{
    return m_filename;
}

void TestSaveXmlFileConfirmation::setText(const std::wstring&)
{}

void TestSaveXmlFileConfirmation::setOriginalFileName(const std::string&s)
{}

void TestSaveXmlFileConfirmation::confirm()
{}

std::string& TestSaveXmlFileConfirmation::filename()
{
    return m_filename;
}

TestConfirmationProvider::TestConfirmationProvider()
{
    addPostgresLayerConf.reset(new AddPostgresLayerConfirmation);

    idColumnNameConf.reset(new TestIdColumnNameConfirmation);

    saveXmlFileConf.reset(new TestSaveXmlFileConfirmation);
}

core::IExternalConfirmationPtr TestConfirmationProvider::getConfirmation(
    core::IExternalConfirmation::ExternalConfirmationType type)
{
    switch (type)
    {
    case core::IExternalConfirmation::AddFromPostgres:
        return addPostgresLayerConf;

    case core::IExternalConfirmation::IdColumnName:
        return idColumnNameConf;

    case core::IExternalConfirmation::SaveFile:
        return saveXmlFileConf;

    default:
        return ExternalConfirmationDefaultProvider::getConfirmation(type);
    }
}
