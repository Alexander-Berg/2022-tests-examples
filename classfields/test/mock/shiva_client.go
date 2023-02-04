package mock

import (
	"github.com/YandexClassifieds/shiva/pb/shiva/api/deploy2"
	"github.com/YandexClassifieds/shiva/pb/shiva/types/deployment"
	"github.com/YandexClassifieds/shiva/pb/shiva/types/flags"
	"github.com/YandexClassifieds/shiva/pb/shiva/types/info"
	"github.com/YandexClassifieds/shiva/pb/shiva/types/layer"
)

type ShivaApiMock struct {
	result <-chan *deploy2.StateResponse
}

func NewShivaApiMock(result <-chan *deploy2.StateResponse) *ShivaApiMock {

	return &ShivaApiMock{
		result: result,
	}
}

func (s ShivaApiMock) State(_ string) <-chan *deploy2.StateResponse {
	return s.result
}

func (s ShivaApiMock) Run(_, _, _, _, _, _, _ string, _ []string, _ map[string]string, _ []string, _, _ bool) (<-chan *deploy2.StateResponse, error) {
	return s.result, nil
}

func (s ShivaApiMock) Promote(_, _ string, _ int64) (<-chan *deploy2.StateResponse, error) {
	return s.result, nil
}

func (s ShivaApiMock) Restart(_, _, _, _, _, _ string) (<-chan *deploy2.StateResponse, error) {
	return s.result, nil
}

func (s ShivaApiMock) Revert(_, _, _, _, _, _ string) (<-chan *deploy2.StateResponse, error) {
	return s.result, nil
}

func (s ShivaApiMock) Stop(_, _, _, _, _, _ string) (<-chan *deploy2.StateResponse, error) {
	return s.result, nil
}

func (s ShivaApiMock) Cancel(_, _ string, _ int64) error {
	return nil
}

func (s ShivaApiMock) Approve(_, _ string, _ int64) (<-chan *deploy2.StateResponse, error) {
	return s.result, nil
}

func (s ShivaApiMock) ApproveList(_ string) ([]*deployment.Deployment, error) {
	panic("implement me")
}

func (s ShivaApiMock) Status(_ string) (*deploy2.StatusResponse, error) {
	return &deploy2.StatusResponse{
		Info: &deploy2.StatusResponse_ServiceInfo{
			ServiceInfo: &deploy2.StatusResponse_Service{
				Entries: []*deploy2.Info{
					{
						Layer: layer.Layer_TEST,
						Name:  "my_service",
						Now: &info.DeploymentInfo{
							Deployment: &deployment.Deployment{
								Version: "0.0.1",
								Layer:   layer.Layer_TEST,
								User:    "robot-vertis-shiva",
							},
							ManifestUrl: "https://www.example.com/shiva.yml",
							MapUrl:      "https://www.example.com/shiva.yml",
						},
					},
				},
			},
		},
	}, nil
}

func (s ShivaApiMock) Settings() ([]*flags.FeatureFlag, error) {
	return []*flags.FeatureFlag{}, nil
}
