package handler

import (
	"testing"
	"time"

	"github.com/YandexClassifieds/shiva/cmd/telegram/message"
	"github.com/YandexClassifieds/shiva/pb/shiva/api/deploy2"
	"github.com/YandexClassifieds/shiva/pb/shiva/types/batch"
	"github.com/YandexClassifieds/shiva/pb/shiva/types/batch_task"
	"github.com/YandexClassifieds/shiva/pb/shiva/types/deployment"
	dType "github.com/YandexClassifieds/shiva/pb/shiva/types/dtype"
	flagsProto "github.com/YandexClassifieds/shiva/pb/shiva/types/flags"
	"github.com/YandexClassifieds/shiva/pb/shiva/types/info"
	"github.com/YandexClassifieds/shiva/pb/shiva/types/layer"
	dState "github.com/YandexClassifieds/shiva/pb/shiva/types/state"
	"github.com/YandexClassifieds/shiva/pb/shiva/types/traffic"
	"github.com/YandexClassifieds/shiva/pkg/feature_flags"
	"github.com/YandexClassifieds/shiva/test"
	"github.com/stretchr/testify/assert"
	"google.golang.org/protobuf/types/known/timestamppb"
)

var (
	defLoc = time.FixedZone("default_city", 3*3600) // TZ env may be empty
	start  = timestamppb.New(time.Date(2020, 6, 15, 1, 15, 30, 0, defLoc))
)

func TestButtonLabel(t *testing.T) {
	msgS := message.NewService(test.NewLogger(t), &NdaMock{}, message.NewMarkdown())
	msgH := NewHandler(test.NewLogger(t), msgS)
	s := msgH.ApproveButtonLabel(&deployment.Deployment{
		ServiceName: "my-service",
		Version:     "v0.4.2",
	})
	assert.Equal(t, "Approve my-service:v0.4.2", s)
}

func TestApproveListMessage(t *testing.T) {
	msgS := message.NewService(test.NewLogger(t), &NdaMock{}, message.NewMarkdown())
	ctx := &deployment.Deployment{
		ServiceName: "my-service",
		Type:        dType.DeploymentType_RUN,
		Layer:       layer.Layer_PROD,
		Version:     "v0.4.2",
		User:        "my-user",
		Issues:      []string{"VOID-42", "VOID-43"},
	}

	expectedMsg := msgS.BacktickHack(`Command: Run
Service: ¬my-service¬
Layer: Prod
Version: ¬v0.4.2¬
Initiator: [my-user](https://staff.yandex-team.ru/my-user)
Issues: [VOID-42](https://st.yandex-team.ru/VOID-42), [VOID-43](https://st.yandex-team.ru/VOID-43)`)

	assert.Equal(t, expectedMsg, msgS.RenderDeployment(ctx))
}

func TestMakeStatusMessageByBatchType(t *testing.T) {
	test.InitTestEnv()

	msgS := message.NewService(test.NewLogger(t), &NdaMock{}, message.NewMarkdown())
	msgH := NewHandler(test.NewLogger(t), msgS)
	type TestCase struct {
		name     string
		expected string
		data     []*deploy2.BatchInfo
	}
	cases := []TestCase{
		{
			name: "full",
			expected: `Status for service *service_name*

*Prod*
Active v. ¬0.1.1¬ by [danevge](https://staff.yandex-team.ru/danevge) at  ([map](https://map.local) [manifest](https://manifest.local))
Periodic: ¬* 3 * * *¬
Next run: ¬2020-06-15T01:15¬
Task in process at ¬2020-06-15T01:15¬

*Test*
Active v. ¬0.1.1¬ by [danevge](https://staff.yandex-team.ru/danevge) at  ([map](https://map.local) [manifest](https://manifest.local))
Periodic: ¬* 3 * * *¬
Next run: ¬2020-06-15T01:15¬
Task in process by [danevge2](https://staff.yandex-team.ru/danevge2) at ¬2020-06-15T01:15¬

*branches*
- ¬br1¬ ver ¬0.1.1¬ by [danevge](https://staff.yandex-team.ru/danevge) at  ([map](https://map.local) [manifest](https://manifest.local))
`,
			data: []*deploy2.BatchInfo{
				makeBatchInfo(layer.Layer_TEST, "", batch.State_Active, &batch_task.BatchTask{
					State:     batch_task.State_Process,
					Author:    "danevge2",
					StartDate: start,
				}),
				makeBatchInfo(layer.Layer_TEST, "br1", batch.State_Active, nil),
				makeBatchInfo(layer.Layer_PROD, "", batch.State_Active, &batch_task.BatchTask{
					State:     batch_task.State_Process,
					StartDate: start,
				}),
			},
		},
		{
			name: "activeNoProcessTask",
			expected: `Status for service *service_name*

*Prod*
Active v. ¬0.1.1¬ by [danevge](https://staff.yandex-team.ru/danevge) at  ([map](https://map.local) [manifest](https://manifest.local))
Periodic: ¬* 3 * * *¬
Next run: ¬2020-06-15T01:15¬

*Test*
Active v. ¬0.1.1¬ by [danevge](https://staff.yandex-team.ru/danevge) at  ([map](https://map.local) [manifest](https://manifest.local))
Periodic: ¬* 3 * * *¬
Next run: ¬2020-06-15T01:15¬
`,
			data: []*deploy2.BatchInfo{
				makeBatchInfo(layer.Layer_TEST, "", batch.State_Active, nil),
				makeBatchInfo(layer.Layer_PROD, "", batch.State_Active, nil),
			},
		},
		{
			name: "onlyTest",
			expected: `Status for service *service_name*

*Test*
Active v. ¬0.1.1¬ by [danevge](https://staff.yandex-team.ru/danevge) at  ([map](https://map.local) [manifest](https://manifest.local))
Periodic: ¬* 3 * * *¬
Next run: ¬2020-06-15T01:15¬
`,
			data: []*deploy2.BatchInfo{
				makeBatchInfo(layer.Layer_TEST, "", batch.State_Active, nil),
			},
		},
		{
			name: "onlyProd",
			expected: `Status for service *service_name*

*Prod*
Active v. ¬0.1.1¬ by [danevge](https://staff.yandex-team.ru/danevge) at  ([map](https://map.local) [manifest](https://manifest.local))
Periodic: ¬* 3 * * *¬
Next run: ¬2020-06-15T01:15¬
`,
			data: []*deploy2.BatchInfo{
				makeBatchInfo(layer.Layer_PROD, "", batch.State_Active, nil),
			},
		},
		{
			name: "inactiveNoProcessTask",
			expected: `Status for service *service_name*

*Prod*
Inactive v. ¬0.1.1¬ by [danevge](https://staff.yandex-team.ru/danevge) at  ([map](https://map.local) [manifest](https://manifest.local))

*Test*
Inactive v. ¬0.1.1¬ by [danevge](https://staff.yandex-team.ru/danevge) at  ([map](https://map.local) [manifest](https://manifest.local))
`,
			data: []*deploy2.BatchInfo{
				makeBatchInfo(layer.Layer_TEST, "", batch.State_Inactive, nil),
				makeBatchInfo(layer.Layer_PROD, "", batch.State_Inactive, nil),
			},
		},
		{
			name: "inactiveWithProcessTask",
			expected: `Status for service *service_name*

*Prod*
Inactive v. ¬0.1.1¬ by [danevge](https://staff.yandex-team.ru/danevge) at  ([map](https://map.local) [manifest](https://manifest.local))
Task in process at ¬2020-06-15T01:15¬

*Test*
Inactive v. ¬0.1.1¬ by [danevge](https://staff.yandex-team.ru/danevge) at  ([map](https://map.local) [manifest](https://manifest.local))
Task in process by [danevge2](https://staff.yandex-team.ru/danevge2) at ¬2020-06-15T01:15¬
`,
			data: []*deploy2.BatchInfo{
				makeBatchInfo(layer.Layer_TEST, "", batch.State_Inactive, &batch_task.BatchTask{
					State:     batch_task.State_Process,
					Author:    "danevge2",
					StartDate: start,
				}),
				makeBatchInfo(layer.Layer_PROD, "", batch.State_Inactive, &batch_task.BatchTask{
					State:     batch_task.State_Process,
					StartDate: start,
				}),
			},
		},
	}
	for _, c := range cases {
		t.Run(c.name, func(t *testing.T) {
			assert.Equal(t, msgS.BacktickHack(c.expected), msgH.makeStatusMessageBatch(c.data))
		})
	}
}

func makeBatchInfo(l layer.Layer, branch string, st batch.State, last *batch_task.BatchTask) *deploy2.BatchInfo {
	result := &deploy2.BatchInfo{
		Deployment: &info.DeploymentInfo{
			Deployment: &deployment.Deployment{
				Layer:       l,
				ServiceName: "service_name",
				Branch:      branch,
				Version:     "0.1.1",
				User:        "danevge",
				State:       dState.DeploymentState_SUCCESS,
			},
			MapUrl:      "https://map.local",
			ManifestUrl: "https://manifest.local",
		},
		Batch: &batch.Batch{
			State: st,
		},
		Last: last,
	}
	if st == batch.State_Active {
		result.GetBatch().Periodic = "* 3 * * *"
		result.GetBatch().NextRun = start
	}
	return result
}

func TestMakeStatusMessageByServiceType(t *testing.T) {

	type TestCase struct {
		name string
		info []*deploy2.Info
		want string
	}
	msgS := message.NewService(test.NewLogger(t), &NdaMock{}, message.NewMarkdown())
	msgH := NewHandler(test.NewLogger(t), msgS)
	data := deployInfoFixture()

	testCases := []TestCase{
		{
			name: "all",
			info: []*deploy2.Info{
				makeInfo(data["test_success"], data["test_in_progress"]),
				makeInfo(data["prod_success"], data["prod_in_progress"]),
				makeInfo(data["test_branch_success"], nil),
				makeInfo(data["test_traffic_branch_success"], nil),
				makeInfo(data["prod_branch_traffic_success"], nil),
			},
			want: `Status for service *svc-status-msg*
*test*
-  v. ¬0.0.1¬ by [somebody](https://staff.yandex-team.ru/somebody) ([map](https://map.local) [manifest](https://manifest.local))
-  in progress v. ¬0.0.1¬ by [somebody](https://staff.yandex-team.ru/somebody) ([map](https://map.local) [manifest](https://manifest.local))

*test branches*
-  branch=¬branchwithtraffic¬ with traffic share v. ¬0.0.1¬ by [somebody](https://staff.yandex-team.ru/somebody) ([map](https://map.local) [manifest](https://manifest.local))
-  branch=¬branchwotraffic¬ v. ¬0.0.1¬ by [somebody](https://staff.yandex-team.ru/somebody) ([map](https://map.local) [manifest](https://manifest.local))

*prod*
-  v. ¬0.0.1¬ by [somebody](https://staff.yandex-team.ru/somebody) ([map](https://map.local) [manifest](https://manifest.local))
-  in progress v. ¬0.0.1¬ by [somebody](https://staff.yandex-team.ru/somebody) ([map](https://map.local) [manifest](https://manifest.local))

*prod branches*
-  branch=¬branchwithtraffic¬ with traffic share v. ¬0.0.1¬ by [somebody](https://staff.yandex-team.ru/somebody) ([map](https://map.local) [manifest](https://manifest.local))

`,
		},
		{
			name: "canary",
			info: []*deploy2.Info{
				makeInfo(data["prod_success"], data["canary"]),
			},
			want: `Status for service *svc-status-msg*
*canary*
-  v. ¬0.0.1¬ by [somebody](https://staff.yandex-team.ru/somebody) ([map](https://map.local) [manifest](https://manifest.local))

*prod*
-  v. ¬0.0.1¬ by [somebody](https://staff.yandex-team.ru/somebody) ([map](https://map.local) [manifest](https://manifest.local))

`,
		},
		{
			name: "canary_in_progress",
			info: []*deploy2.Info{
				makeInfo(data["prod_success"], data["canary"]),
			},
			want: `Status for service *svc-status-msg*
*canary*
-  v. ¬0.0.1¬ by [somebody](https://staff.yandex-team.ru/somebody) ([map](https://map.local) [manifest](https://manifest.local))

*prod*
-  v. ¬0.0.1¬ by [somebody](https://staff.yandex-team.ru/somebody) ([map](https://map.local) [manifest](https://manifest.local))

`,
		},
		{
			name: "canary_in_progress",
			info: []*deploy2.Info{
				makeInfo(data["prod_success"], data["canary"]),
			},
			want: `Status for service *svc-status-msg*
*canary*
-  v. ¬0.0.1¬ by [somebody](https://staff.yandex-team.ru/somebody) ([map](https://map.local) [manifest](https://manifest.local))

*prod*
-  v. ¬0.0.1¬ by [somebody](https://staff.yandex-team.ru/somebody) ([map](https://map.local) [manifest](https://manifest.local))

`,
		},
	}
	for _, c := range testCases {
		t.Run(c.name, func(t *testing.T) {
			assert.Equal(t, msgS.BacktickHack(c.want), msgH.makeStatusMessageByServiceType(c.info))
		})
	}
}

func deployInfoFixture() map[string]*info.DeploymentInfo {

	result := map[string]*info.DeploymentInfo{}
	svcName := "svc-status-msg"
	svcVersion := "0.0.1"
	mapUrl := "https://map.local"
	manifestUrl := "https://manifest.local"
	staffLogin := "somebody"
	branchNameWithTraffic := "branchwithtraffic"
	branchNameWoTraffic := "branchwotraffic"

	result["test_success"] = &info.DeploymentInfo{
		Deployment: &deployment.Deployment{
			Layer:       layer.Layer_TEST,
			ServiceName: svcName,
			Version:     svcVersion,
			Branch:      "",
			User:        staffLogin,
			State:       dState.DeploymentState_SUCCESS,
		},
		MapUrl:      mapUrl,
		ManifestUrl: manifestUrl,
	}

	result["test_in_progress"] = &info.DeploymentInfo{
		Deployment: &deployment.Deployment{
			Layer:       layer.Layer_TEST,
			ServiceName: svcName,
			Version:     svcVersion,
			Branch:      "",
			User:        staffLogin,
			State:       dState.DeploymentState_IN_PROGRESS,
		},
		MapUrl:      mapUrl,
		ManifestUrl: manifestUrl,
	}

	result["test_branch_success"] = &info.DeploymentInfo{
		Deployment: &deployment.Deployment{
			Layer:       layer.Layer_TEST,
			ServiceName: svcName,
			Version:     svcVersion,
			User:        staffLogin,
			State:       dState.DeploymentState_SUCCESS,
			Branch:      branchNameWoTraffic,
		},
		MapUrl:      mapUrl,
		ManifestUrl: manifestUrl,
	}

	result["test_traffic_branch_success"] = &info.DeploymentInfo{
		Deployment: &deployment.Deployment{
			Layer:       layer.Layer_TEST,
			ServiceName: svcName,
			Version:     svcVersion,
			Branch:      branchNameWithTraffic,
			User:        staffLogin,
			State:       dState.DeploymentState_SUCCESS,
			Traffic:     traffic.Traffic_ONE_PERCENT,
		},
		MapUrl:      mapUrl,
		ManifestUrl: manifestUrl,
	}

	result["canary"] = &info.DeploymentInfo{
		Deployment: &deployment.Deployment{
			Layer:       layer.Layer_PROD,
			ServiceName: svcName,
			Version:     svcVersion,
			Branch:      "",
			User:        staffLogin,
			State:       dState.DeploymentState_CANARY,
		},
		MapUrl:      mapUrl,
		ManifestUrl: manifestUrl,
	}

	result["canary_in_progress"] = &info.DeploymentInfo{
		Deployment: &deployment.Deployment{
			Layer:       layer.Layer_PROD,
			ServiceName: svcName,
			Version:     svcVersion,
			Branch:      "",
			User:        staffLogin,
			State:       dState.DeploymentState_CANARY_PROGRESS,
		},
		MapUrl:      mapUrl,
		ManifestUrl: manifestUrl,
	}

	result["prod_success"] = &info.DeploymentInfo{
		Deployment: &deployment.Deployment{
			Layer:       layer.Layer_PROD,
			ServiceName: svcName,
			Version:     svcVersion,
			Branch:      "",
			User:        staffLogin,
			State:       dState.DeploymentState_SUCCESS,
		},
		MapUrl:      mapUrl,
		ManifestUrl: manifestUrl,
	}

	result["prod_in_progress"] = &info.DeploymentInfo{
		Deployment: &deployment.Deployment{
			Layer:       layer.Layer_PROD,
			ServiceName: svcName,
			Version:     svcVersion,
			User:        staffLogin,
			State:       dState.DeploymentState_IN_PROGRESS,
		},
		MapUrl:      mapUrl,
		ManifestUrl: manifestUrl,
	}

	result["prod_branch_traffic_success"] = &info.DeploymentInfo{
		Deployment: &deployment.Deployment{
			Layer:       layer.Layer_PROD,
			ServiceName: svcName,
			Version:     svcVersion,
			Branch:      branchNameWithTraffic,
			Traffic:     traffic.Traffic_ONE_PERCENT,
			User:        staffLogin,
			State:       dState.DeploymentState_SUCCESS,
		},
		MapUrl:      mapUrl,
		ManifestUrl: manifestUrl,
	}

	return result
}

func TestMakeStatusMessageWithLimit(t *testing.T) {
	msgS := message.NewService(test.NewLogger(t), &NdaMock{}, message.NewMarkdown())
	msgH := NewHandler(test.NewLogger(t), msgS)
	msgH.maxStatusEntries = 3
	data := deployInfoFixture()

	expectedMessage := "Status for service *svc-status-msg*\n" +
		"*test*\n" +
		"-  v. `0.0.1` by [somebody](https://staff.yandex-team.ru/somebody) ([map](https://map.local) [manifest](https://manifest.local))\n" +
		"-  in progress v. `0.0.1` by [somebody](https://staff.yandex-team.ru/somebody) ([map](https://map.local) [manifest](https://manifest.local))\n\n" +
		"*test branches*\n" +
		"-  branch=`branchwithtraffic` with traffic share v. `0.0.1` by [somebody](https://staff.yandex-team.ru/somebody) ([map](https://map.local) [manifest](https://manifest.local))\n\n" +
		"*[2 more branches...](https://admin.vertis.yandex-team.ru/services/svc-status-msg)*"

	assert.Equal(t, expectedMessage, msgH.makeStatusMessageByServiceType([]*deploy2.Info{
		makeInfo(data["test_success"], data["test_in_progress"]),
		makeInfo(data["prod_success"], data["prod_in_progress"]),
		makeInfo(data["test_branch_success"], nil),
		makeInfo(data["test_traffic_branch_success"], nil),
		makeInfo(data["prod_branch_traffic_success"], nil),
	}))
}

func TestMakeStatusMessageByBranchType(t *testing.T) {

}

func makeInfo(now, new *info.DeploymentInfo) *deploy2.Info {
	return &deploy2.Info{
		Layer:  now.GetDeployment().GetLayer(),
		Name:   now.GetDeployment().GetServiceName(),
		Branch: now.GetDeployment().GetBranch(),
		Now:    now,
		New:    new,
	}
}

func TestMakeDeployStatusMessageNotClosed(t *testing.T) {

	msgS := message.NewService(test.NewLogger(t), &NdaMock{}, message.NewMarkdown())
	msgH := NewHandler(test.NewLogger(t), msgS)
	expected := "Все датацентры доступны"

	assert.Equal(t, expected, msgH.MakeDeployStatusMessage([]*flagsProto.FeatureFlag{}))
}

func TestMakeDeployStatusMessageClosed(t *testing.T) {

	msgS := message.NewService(test.NewLogger(t), &NdaMock{}, message.NewMarkdown())
	msgH := NewHandler(test.NewLogger(t), msgS)
	expected := "По причине \"учения\" недоступны следующие дц:\n" +
		"- sas в слое Prod\n" +
		"- myt в слое Prod\n"

	assert.Equal(t, expected, msgH.MakeDeployStatusMessage([]*flagsProto.FeatureFlag{{
		Name:   feature_flags.ProdSasOff.String(),
		Reason: "учения",
	}, {
		Name:   feature_flags.ProdMytOff.String(),
		Reason: "учения",
	}}))
}

func TestMakeDeployStatusMessageClosedEscape(t *testing.T) {

	msgS := message.NewService(test.NewLogger(t), &NdaMock{}, message.NewMarkdown())
	msgH := NewHandler(test.NewLogger(t), msgS)
	expected := "По причине \"test\\_reason\" недоступны следующие дц:\n" +
		"- yd\\_sas в слое Prod\n"

	assert.Equal(t, expected, msgH.MakeDeployStatusMessage([]*flagsProto.FeatureFlag{{
		Name:   feature_flags.ProdYdSasOff.String(),
		Reason: "test_reason",
	}}))
}

type NdaMock struct {
}

func (n *NdaMock) NdaUrl(url string) (string, error) {
	return url, nil
}
