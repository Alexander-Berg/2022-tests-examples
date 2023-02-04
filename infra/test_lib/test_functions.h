#pragma once

#include <infra/pod_agent/libs/porto_client/client.h>

#include <yp/yp_proto/yp/client/api/proto/pod_agent.pb.h>

#include <library/cpp/protobuf/json/json2proto.h>

namespace NInfra::NPodAgent::NDaemonTest  {

const TString TEST_PREFIX = "DaemonTest_";

NProtobufJson::TJson2ProtoConfig GetSpecHolderJson2ProtoConfig();

API::TPodAgentStatus CorrectPodAgentStatus(const API::TPodAgentStatus &status);

bool CheckAllObjectsReady(const API::TPodAgentStatus& status);

void DestroyContainersWithPrefix(TPortoClientPtr porto, const TString& prefix);

} // namespace NInfra::NPodAgent::NDaemonTest
