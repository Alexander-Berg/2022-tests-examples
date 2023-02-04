#pragma once
#include <util/system/defaults.h>
#include <util/generic/string.h>
#include <library/cpp/getopt/last_getopt.h>

class TDaemonConfig;
class TConfigPatches;

void PutPreffixedPatch(TConfigPatches* patches, bool prefixed, ui32 shardsNumber);
TString GetDaemonConfigText(const TString& runPath);
void ConfigureAndStartBackend(const TString& rootDir, const TDaemonConfig& daemonConfig, const TString& protocolType = 0, int maxDocs = -1);
extern TString SuggestZonesFileName;
