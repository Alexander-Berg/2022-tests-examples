package service

import (
	"fmt"
	"math/rand"
	"testing"
	"time"

	"github.com/YandexClassifieds/shiva/cmd/telegram/message"
	"github.com/YandexClassifieds/shiva/pb/shiva/api/deploy2"
	"github.com/YandexClassifieds/shiva/pb/shiva/service_map"
	"github.com/YandexClassifieds/shiva/pb/shiva/types/deployment"
	"github.com/YandexClassifieds/shiva/pb/shiva/types/dtype"
	pbError "github.com/YandexClassifieds/shiva/pb/shiva/types/error"
	"github.com/YandexClassifieds/shiva/pb/shiva/types/info"
	"github.com/YandexClassifieds/shiva/pb/shiva/types/layer"
	"github.com/YandexClassifieds/shiva/pb/shiva/types/revert"
	dState "github.com/YandexClassifieds/shiva/pb/shiva/types/state"
	"github.com/YandexClassifieds/shiva/test"
	"github.com/golang/protobuf/ptypes"
	"github.com/golang/protobuf/ptypes/timestamp"
	"github.com/stretchr/testify/assert"
)

var (
	r     *rand.Rand
	start *timestamp.Timestamp
)

func init() {

	test.InitTestEnv()
	var err error
	r = rand.New(rand.NewSource(time.Now().UnixNano()))
	start, err = ptypes.TimestampProto(time.Date(2020, 6, 15, 1, 15, 30, 0, time.Local))
	if err != nil {
		panic(err)
	}
}

func TestCommentBlock(t *testing.T) {
	builder := NewMessageBuilder(test.NewLogger(t), message.NewService(test.NewLogger(t), &NdaMock{}, message.NewMarkdown()))
	cases := []struct {
		name     string
		response *deploy2.StateResponse
		result   string
	}{
		{
			name: "empty comment",
			response: &deploy2.StateResponse{
				Service: &info.DeploymentInfo{
					Deployment: &deployment.Deployment{
						Comment: "",
					},
				},
			},
			result: "",
		},
		{
			name: "too long comment",
			response: &deploy2.StateResponse{
				Service: &info.DeploymentInfo{
					Deployment: &deployment.Deployment{
						Comment: string(make([]rune, maxCommentLength+1)),
					},
				},
			},
			result: fmt.Sprintf(commentFormat, commentDummy),
		},
		{
			name: "normal comment",
			response: &deploy2.StateResponse{
				Service: &info.DeploymentInfo{
					Deployment: &deployment.Deployment{
						Comment: "Comment",
					},
				},
			},
			result: fmt.Sprintf(commentFormat, "Comment"),
		},
	}

	for _, c := range cases {
		t.Run(c.name, func(t *testing.T) {
			assert.Equal(t, c.result, builder.commentBlock(c.response))
		})
	}
}

func TestContextBlock(t *testing.T) {
	expectTime := time.Unix(1601019815, 0).In(time.Local)
	msgS := message.NewService(test.NewLogger(t), &NdaMock{}, message.NewMarkdown())
	var expectedContext = msgS.BacktickHack(`Command: Run
Service: ¬my-service¬
Layer: Test
Version: ¬v42¬
Branch: ¬mybranch¬
Initiator: [alexander-s](https://staff.yandex-team.ru/alexander-s)
Issues: [VOID-123](https://st.yandex-team.ru/VOID-123), [VOID-785](https://st.yandex-team.ru/VOID-785)
Time: ¬%s¬`)
	expectedContext = fmt.Sprintf(expectedContext, expectTime.Format(timeFormat))

	s := msgS.RenderDeploymentWrapper(NewWrapper(msgS, &deploy2.StateResponse{
		Service: &info.DeploymentInfo{
			Deployment: &deployment.Deployment{
				ServiceName: "my-service",
				Version:     "v42",
				Issues:      []string{"VOID-123", "VOID-785"},
				Layer:       layer.Layer_TEST,
				Branch:      "mybranch",
				User:        "alexander-s",
				Type:        dtype.DeploymentType_RUN,
				Start:       &timestamp.Timestamp{Seconds: expectTime.Unix(), Nanos: 0},
			},
		},
	}, test.NewLogger(t)))
	assert.Equal(t, expectedContext, s)
}

func TestContextBlock_SingleIssue(t *testing.T) {
	msgS := message.NewService(test.NewLogger(t), &NdaMock{}, message.NewMarkdown())
	var expectedContext = msgS.BacktickHack(`Command: Run
Service: ¬my-service¬
Layer: Prod
Version: ¬v42¬
Branch: ¬mybranch¬
Initiator: [alexander-s](https://staff.yandex-team.ru/alexander-s)
Issues: [VOID-123](https://st.yandex-team.ru/VOID-123)`)
	s := msgS.RenderDeploymentWrapper(NewWrapper(msgS, &deploy2.StateResponse{
		Service: &info.DeploymentInfo{
			Deployment: &deployment.Deployment{
				ServiceName: "my-service",
				Version:     "v42",
				Issues:      []string{"VOID-123"},
				Layer:       layer.Layer_PROD,
				Branch:      "mybranch",
				User:        "alexander-s",
				Type:        dtype.DeploymentType_RUN,
			},
		},
	}, test.NewLogger(t)))
	assert.Equal(t, expectedContext, s)
}

//TODO add  description
func TestContextBlockRevertWithoutPrevious(t *testing.T) {
	msgS := message.NewService(test.NewLogger(t), &NdaMock{}, message.NewMarkdown())
	var expectedContext = msgS.BacktickHack(`Command: Run
Service: ¬my-service¬
Layer: Test
Branch: ¬mybranch¬
Initiator: [alexander-s](https://staff.yandex-team.ru/alexander-s)`)
	s := msgS.RenderDeploymentWrapper(NewWrapper(msgS, &deploy2.StateResponse{
		Service: &info.DeploymentInfo{
			Deployment: &deployment.Deployment{
				State:       dState.DeploymentState_REVERT,
				ServiceName: "my-service",
				Version:     "v42",
				Layer:       layer.Layer_TEST,
				Branch:      "mybranch",
				User:        "alexander-s",
				Type:        dtype.DeploymentType_RUN,
			},
		},
	}, test.NewLogger(t)))
	assert.Equal(t, expectedContext, s)
}

func TestStateBlock(t *testing.T) {
	msgS := message.NewService(test.NewLogger(t), &NdaMock{}, message.NewMarkdown())
	cases := []struct {
		name   string
		d      *deployment.Deployment
		result string
	}{
		{
			name: "nil",
			d: &deployment.Deployment{
				State: dState.DeploymentState_IN_PROGRESS,
			},
			result: "\n\nState: *progress*",
		},
		{
			name: "err",
			d: &deployment.Deployment{
				State: dState.DeploymentState_FAILED,
				Status: &deployment.Status{
					Error: &pbError.UserError{
						Error: "It is error!",
					},
				},
			},
			result: "\n\nState: *failed*\nError: `It is error!`",
		},
		{
			name: "ruMessage",
			d: &deployment.Deployment{
				State: dState.DeploymentState_FAILED,
				Status: &deployment.Status{
					Error: &pbError.UserError{
						RuMessage: "Что-то пошло не так!",
					},
				},
			},
			result: "\n\nState: *failed*\nError: `Что-то пошло не так!`",
		},
		{
			name: "enMessage",
			d: &deployment.Deployment{
				State: dState.DeploymentState_FAILED,
				Status: &deployment.Status{
					Error: &pbError.UserError{
						EnMessage: "It is fail!",
					},
				},
			},
			result: "\n\nState: *failed*\nError: `It is fail!`",
		},
		{
			name: "ruMessage_err_enMessage",
			d: &deployment.Deployment{
				State: dState.DeploymentState_FAILED,
				Status: &deployment.Status{
					Error: &pbError.UserError{
						RuMessage: "Что-то пошло не так!",
						Error:     "It is error!",
						EnMessage: "It is fail!",
					},
				},
			},
			result: "\n\nState: *failed*\nError: `Что-то пошло не так!`",
		},
		{
			name: "err_enMessage",
			d: &deployment.Deployment{
				State: dState.DeploymentState_FAILED,
				Status: &deployment.Status{
					Error: &pbError.UserError{
						Error:     "It is error!",
						EnMessage: "It is fail!",
					},
				},
			},
			result: "\n\nState: *failed*\nError: `It is fail!`",
		},
		{
			name: "empty",
			d: &deployment.Deployment{
				State: dState.DeploymentState_FAILED,
				Status: &deployment.Status{
					Error: &pbError.UserError{
						Docs: []string{},
					},
				},
			},
			result: "\n\nState: *failed*",
		},
		{
			name: "doc",
			d: &deployment.Deployment{
				State: dState.DeploymentState_FAILED,
				Status: &deployment.Status{
					Error: &pbError.UserError{
						RuMessage: "текст",
						Docs:      []string{"doc"},
					},
				},
			},
			result: "\n\nState: *failed*\nError: `текст`\nDocs:\n • doc",
		},
		{
			name: "docs",
			d: &deployment.Deployment{
				State: dState.DeploymentState_FAILED,
				Status: &deployment.Status{
					Error: &pbError.UserError{
						RuMessage: "текст",
						Docs:      []string{"doc1", "doc2", "doc3"},
					},
				},
			},
			result: "\n\nState: *failed*\nError: `текст`\nDocs:\n • doc1\n • doc2\n • doc3",
		},
		{
			name: "single warning",
			d: &deployment.Deployment{
				State: dState.DeploymentState_IN_PROGRESS,
				Status: &deployment.Status{
					Warnings: []*pbError.UserError{{RuMessage: "my _md_ *warning*"}},
				},
			},
			result: "\n\nState: *progress*\nWarnings:\n • `my _md_ *warning*`",
		},
		{
			name: "double warning",
			d: &deployment.Deployment{
				State: dState.DeploymentState_IN_PROGRESS,
				Status: &deployment.Status{
					Warnings: []*pbError.UserError{
						{RuMessage: "my _md_ *warning*"},
						{RuMessage: "second"},
					},
				},
			},
			result: "\n\nState: *progress*\nWarnings:\n • `my _md_ *warning*`\n • `second`",
		},
	}

	for _, c := range cases {
		t.Run(c.name, func(t *testing.T) {
			w := NewWrapper(msgS, &deploy2.StateResponse{
				Service: &info.DeploymentInfo{
					Deployment: c.d,
				},
			}, test.NewLogger(t))
			text := joinMessages(msgS.RenderTgTemplate(stateTemplate, w), msgS.RenderDescription(c.d))
			assert.Equal(t, c.result, text)
		})
	}
}

func TestRevertReasonMessage(t *testing.T) {

	msgS := message.NewService(test.NewLogger(t), &NdaMock{}, message.NewMarkdown())
	type TestCase struct {
		name   string
		rv     *Wrapper
		result string
	}
	tcs := []TestCase{
		{
			name: "empty_status",
			rv:   newWrapper(t, msgS, "", &deployment.Status{}),
			result: fmt.Sprintf(`undefined fail
Logs for your service can be found [here](%s)`, grafana("service=my_service layer=test")),
		},
		{
			name: "branch",
			rv:   newWrapper(t, msgS, "bbb", &deployment.Status{}),
			result: fmt.Sprintf(`undefined fail
Logs for your service can be found [here](%s)`, grafana("service=my_service layer=test branch=bbb")),
		},
		{
			name: "terminate_without_allocations",
			rv: newWrapper(t, msgS, "", &deployment.Status{
				Provides:        nil,
				RevertType:      revert.RevertType_Terminate,
				FailAllocations: nil,
			}),
			result: fmt.Sprintf(`docker container exited with non-zero code
Logs for your service can be found [here](%s)`, grafana("service=my_service layer=test")),
		},
		{
			name: "terminate",
			rv: newWrapper(t, msgS, "", &deployment.Status{
				RevertType:      revert.RevertType_Terminate,
				FailAllocations: []string{"111", "222"},
			}),
			result: fmt.Sprintf(`docker container exited with non-zero code
Logs for your service:
- [111](%s)
- [222](%s)`,
				grafana("service=my_service layer=test allocation_id=111"),
				grafana("service=my_service layer=test allocation_id=222")),
		},
		{
			name: "OOM",
			rv: newWrapper(t, msgS, "", &deployment.Status{
				RevertType:      revert.RevertType_OOM,
				FailAllocations: []string{"111", "222"},
			}),
			result: fmt.Sprintf(`docker container exited because of OOM Killer
Logs for your service:
- [111](%s)
- [222](%s)`,
				grafana("service=my_service layer=test allocation_id=111"),
				grafana("service=my_service layer=test allocation_id=222")),
		},
		{
			name: "undefined",
			rv: newWrapper(t, msgS, "", &deployment.Status{
				RevertType: revert.RevertType_Undefined,
			}),
			result: fmt.Sprintf(`undefined fail
Logs for your service can be found [here](%s)`, grafana("service=my_service layer=test")),
		},
		{
			name: "unhealthy_without_provides",
			rv: newWrapper(t, msgS, "", &deployment.Status{
				RevertType:      revert.RevertType_Unhealthy,
				FailAllocations: []string{"111", "222"},
			}),
			result: fmt.Sprintf(`some allocs were unhealthy
Logs for your service:
- [111](%s)
- [222](%s)`,
				grafana("service=my_service layer=test allocation_id=111"),
				grafana("service=my_service layer=test allocation_id=222")),
		},
		{
			name: "unhealthy_monitoring",
			rv: newWrapper(t, msgS, "", &deployment.Status{
				Provides: []*deployment.ProvideStatus{
					{
						Provide: &service_map.ServiceProvides{
							Name:     "monitoring",
							Protocol: service_map.ServiceProvides_http,
							Port:     81,
						},
						Status: false,
					},
				},
				RevertType:      revert.RevertType_Unhealthy,
				FailAllocations: []string{"111", "222"},
			}),
			result: fmt.Sprintf(`some allocs were unhealthy, following checks failed:
- monitoring (port 81), [docs](%s) 
Logs for your service:
- [111](%s)
- [222](%s)`,
				message.DocsMonitoring,
				grafana("service=my_service layer=test allocation_id=111"),
				grafana("service=my_service layer=test allocation_id=222")),
		},
		{
			name: "unhealthy_api",
			rv: newWrapper(t, msgS, "", &deployment.Status{
				Provides: []*deployment.ProvideStatus{
					{
						Provide: &service_map.ServiceProvides{
							Name:     "api",
							Protocol: service_map.ServiceProvides_http,
							Port:     80,
						},
						Status: false,
					},
				},
				RevertType:      revert.RevertType_Unhealthy,
				FailAllocations: []string{"111", "222"},
			}),
			result: fmt.Sprintf(`some allocs were unhealthy, following checks failed:
- api (port 80), [docs](%s) 
Logs for your service:
- [111](%s)
- [222](%s)`,
				message.DocsHealthcheck,
				grafana("service=my_service layer=test allocation_id=111"),
				grafana("service=my_service layer=test allocation_id=222")),
		},
		{
			name: "unhealthy_api",
			rv: newWrapper(t, msgS, "", &deployment.Status{
				Provides: []*deployment.ProvideStatus{
					{
						Provide: &service_map.ServiceProvides{
							Name:     "monitoring",
							Protocol: service_map.ServiceProvides_http,
							Port:     81,
						},
						Status: false,
					},
					{
						Provide: &service_map.ServiceProvides{
							Name:     "api",
							Protocol: service_map.ServiceProvides_http,
							Port:     80,
						},
						Status: false,
					},
				},
				RevertType:      revert.RevertType_Unhealthy,
				FailAllocations: []string{"111", "222"},
			}),
			result: fmt.Sprintf(`some allocs were unhealthy, following checks failed:
- monitoring (port 81), [docs](%s) 
- api (port 80), [docs](%s) 
Logs for your service:
- [111](%s)
- [222](%s)`,
				message.DocsMonitoring,
				message.DocsHealthcheck,
				grafana("service=my_service layer=test allocation_id=111"),
				grafana("service=my_service layer=test allocation_id=222")),
		},
	}
	for _, c := range tcs {
		t.Run(c.name, func(t *testing.T) {
			revertMsg := c.rv.RevertReasonMessage()
			assert.Equal(t, c.result, revertMsg)
		})
	}
}

func grafana(expr string) string {
	return "https://grafana.vertis.yandex-team.ru/explore?orgId=1&left=[\"1592172630000\",\"1592174730000\",\"vertis-logs\",{\"expr\":\"" +
		expr +
		"\",\"fields\":[\"thread\",\"context\",\"message\",\"rest\"],\"limit\":500},{\"ui\":[true,true,true,\"none\"]}]"
}

type NdaMock struct {
}

func (n *NdaMock) NdaUrl(url string) (string, error) {
	return url, nil
}

func newWrapper(t *testing.T, msgS *message.Service, branch string, st *deployment.Status) *Wrapper {
	return NewWrapper(msgS, &deploy2.StateResponse{
		Service: &info.DeploymentInfo{
			Deployment: &deployment.Deployment{
				ServiceName: "my_service",
				Branch:      branch,
				Layer:       layer.Layer_TEST,
				Start:       start,
				Status:      st,
			},
		},
	}, test.NewLogger(t))
}
