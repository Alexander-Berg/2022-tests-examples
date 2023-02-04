#include "standart_generator.h"

#include <saas/api/factors_erf.h>
#include <util/charset/wide.h>

namespace {
    template <class T>
    bool CheckValueType(const TStandartAttributesFiller::TAttributeValue& attr) {
        try {
            FromString<T>(attr.Value);
            return true;
        } catch(...) {
            if (attr.Value.StartsWith("add:")) {
                try {
                    FromString<T>(attr.Value.substr(4));
                    return true;
                } catch(...) {}
            }
        }
        return false;
    }
}

void TStandartAttributesFiller::FillDocAttr(const TAttributeValue& attr, NRTYServer::TMessage::TDocument& document) const {
    bool isInt = CheckValueType<i64>(attr);
    bool isFloat = CheckValueType<float>(attr);
    ::NRTYServer::TAttribute* prAttr;
    ::NRTYServer::TMessage::TDocument::TProperty* prProp;
    if (atGroup & attr.Types) {
        VERIFY_WITH_LOG(isInt, "Incorrect group attribute for generator");
        prAttr = document.AddGroupAttributes();
        prAttr->set_name(attr.Name);
        prAttr->set_value(attr.Value);
        prAttr->set_type(::NRTYServer::TAttribute::INTEGER_ATTRIBUTE);
    }
    if (atIntSearch & attr.Types) {
        VERIFY_WITH_LOG(isInt, "Incorrect intSearch attribute for generator");
        prAttr = document.AddSearchAttributes();
        prAttr->set_name(attr.Name);
        prAttr->set_value(attr.Value);
        prAttr->set_type(::NRTYServer::TAttribute::INTEGER_ATTRIBUTE);
    }
    if (atLitSearch & attr.Types) {
        prAttr = document.AddSearchAttributes();
        prAttr->set_name(attr.Name);
        prAttr->set_value(attr.Value);
        prAttr->set_type(::NRTYServer::TAttribute::LITERAL_ATTRIBUTE);
    }
    if (atProp & attr.Types) {
        prProp = document.AddDocumentProperties();
        prProp->set_name(attr.Name);
        prProp->set_value(attr.Value);
    }
    if (atFactor & attr.Types) {
        VERIFY_WITH_LOG(isFloat, "Incorrect factor attribute for generator");
        NSaas::AddSimpleFactor(attr.Name, attr.Value, *document.MutableFactors());
    }
}

void TStandartDocumentGenerator::BuildDocumentProperties(ui32 docIndex, i64 messId, NRTYServer::TMessage::TDocument& document) const {
    document.SetMimeType("text/html");
    document.SetCharset("UTF8");
    document.SetLanguage("rus");
    document.SetUrl(WideToUTF8(UTF8ToWide("http://ПаНаМар.cOm/" + ToString(messId + 123456789) + "_" + ToString(messId % 3))));
    if (IsPrefixed) {
        if (PrefixPolicy == pgpGrow)
            document.SetKeyPrefix(BASE_KEY_PREFIX + messId);
        else if (PrefixPolicy == pgpConstant)
            document.SetKeyPrefix(PrefixConstant);
        else
            VERIFY_WITH_LOG(false, "Incorrect prefix policy type for generator");
    }
    if (TextPolicy == tgpArray) {
        VERIFY_WITH_LOG(docIndex < Texts.size(), "Incorrect index for text generation");
        document.SetBody(Texts[docIndex]);
    } else if (TextPolicy == tgpConstant) {
        document.SetBody(TextConstant);
    } else if (TextPolicy == tgpRandom) {
        document.SetBody(GetRandomWord());
    } else
        VERIFY_WITH_LOG(false, "Incorrect text generation policy");
}

