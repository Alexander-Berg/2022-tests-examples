#pragma once
#include <library/cpp/logger/global/global.h>
#include <util/system/shellcommand.h>

bool SendSignal(TProcessId pid, size_t signum);
