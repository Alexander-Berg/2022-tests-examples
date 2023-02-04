#pragma once

#include <ads/bsyeti/libs/yt_rpc_client/master_client.h>

void ManualTest(const TString& path, const TString& counters, NYTRpc::IMasterMultiClientPtr client);
void ManualTestCodecs(const TString& path, const TString& counters, NYTRpc::IMasterMultiClientPtr client);
void ManualTestLoadSaveUserProfile(const TString& path, const TString& counters, NYTRpc::IMasterMultiClientPtr client);
void ManualTestXdeltaCodec(const TString& path, const TString& counters, NYTRpc::IMasterMultiClientPtr client);
void ManualTestLoadProfilesWithWrongCodecId(const TString& path, const TString& counters, NYTRpc::IMasterMultiClientPtr client);
