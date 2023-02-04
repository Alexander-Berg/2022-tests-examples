#pragma once

#include <util/generic/maybe.h>
#include <util/generic/string.h>

#include <utility>

//TMaybe<std::pair<bool, TMaybe<bool>>> ParseIsGdprB(TString val);

TMaybe<std::pair<int, TMaybe<int>>> ParseIsGdprB(TString val);
