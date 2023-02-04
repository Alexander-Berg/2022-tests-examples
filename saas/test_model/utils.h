#pragma once

#include "model.h"

#include <library/cpp/json/json_value.h>
#include <library/cpp/xml/doc/xmldoc.h>

#include <library/cpp/charset/ci_string.h>
#include <util/datetime/base.h>
#include <util/generic/strbuf.h>
#include <util/generic/string.h>

bool GetJsonFromHttp(const TString& host, ui16 port, const TStringBuf& query, NJson::TJsonValue& result, const TDuration timeout = TDuration::Seconds(120), bool isHttps=true);
bool GetTextFromHttp(const TString& host, ui16 port, const TStringBuf& query, TString& result, const TDuration timeout = TDuration::Seconds(120), bool isHttps=true);
xmlNode* GetChild(const TCiString& path, xmlNode* parent, size_t beg = 0);
void WriteRevisionInfo(const TRevisionInfo& revision, NJson::TJsonValue& element);
void WriteRevisionInfo(const char* key, const TTestExecution& revision, NJson::TJsonValue& result, bool writeTaskInfo);
bool GetValueFromHttp(const TString& host, ui16 port, const TStringBuf& query, ui64& result);
