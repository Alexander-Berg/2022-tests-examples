#pragma once

#include <library/cpp/json/json_value.h>
namespace NSandbox {
    bool GetTask(const TString& host, ui16 port, ui64 taskId, NJson::TJsonValue& result);
}
