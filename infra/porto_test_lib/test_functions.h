#pragma once

#include <infra/pod_agent/libs/porto_client/client.h>

namespace NInfra::NPodAgent::NPortoTestLib  {

TPortoClientPtr GetSimplePortoClient();

TPortoClientPtr GetTestPortoClient(const TString& containersPrefix, const TString& layerPrefix, const TString& storagePrefix, const TString& volumesSubstr);

TPortoClientPtr GetTestPortoClientWithRetries(const TString& containersPrefix, const TString& layerPrefix, const TString& storagePrefix, const TString& volumesSubstr);

void DestroyOldContainers(const TString& prefix);

// Some tests need fixes only into sandbox isolation. Details: DEPLOY-3872
bool IsInsideSandboxPortoIsolation();

} // namespace NInfra::NPodAgent::NPortoTestLib
