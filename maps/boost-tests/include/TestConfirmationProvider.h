#pragma once

#include "../../../core/PostgresLayerAddingConfirmation.h"
#include "../../../core/ExternalConfirmationsDefault.h"

namespace maps { namespace renderer5 { namespace test {

namespace map
{

class AddPostgresLayerConfirmation:
    public core::PostgresLayerAddingConfirmation
{
public:
    AddPostgresLayerConfirmation();

    virtual void confirm();

    void reset();

    std::wstring filterExpr;
    std::wstring schemaName;
    std::wstring tableName;
    core::FeatureType featureType;
};

typedef boost::shared_ptr<AddPostgresLayerConfirmation> AddPostgresLayerConfirmationPtr;

class TestIdColumnNameConfirmation:
    public core::IdColumnNameConfirmationDefault
{
public:
    TestIdColumnNameConfirmation();

    virtual void confirm();

    std::wstring& idColumnName();
};

typedef boost::shared_ptr<TestIdColumnNameConfirmation> TestIdColumnNameConfirmationPtr;

class TestSaveXmlFileConfirmation:
    public core::ISaveXmlFileConfirmation
{
public:
    TestSaveXmlFileConfirmation();
    ~TestSaveXmlFileConfirmation();

    virtual IExternalConfirmation::ExternalConfirmationType type() const;
    virtual const std::string & filename() const;
    virtual void setText(const std::wstring & s);
    virtual void setOriginalFileName(const std::string& s);
    virtual void confirm();

    std::string& filename();

protected:
    std::string m_filename;
};

typedef boost::shared_ptr<TestSaveXmlFileConfirmation> TestSaveXmlFileConfirmationPtr;

class TestConfirmationProvider:
    public core::ExternalConfirmationDefaultProvider
{
public:
    TestConfirmationProvider();

    core::IExternalConfirmationPtr getConfirmation(
        core::IExternalConfirmation::ExternalConfirmationType type);

public:
    AddPostgresLayerConfirmationPtr addPostgresLayerConf;
    TestIdColumnNameConfirmationPtr idColumnNameConf;
    TestSaveXmlFileConfirmationPtr saveXmlFileConf;
};

typedef boost::shared_ptr<TestConfirmationProvider> TestConfirmationProviderPtr;

} // map

} } } // maps::renderer5::test
