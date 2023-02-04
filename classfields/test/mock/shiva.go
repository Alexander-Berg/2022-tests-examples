package mock

import (
	"github.com/YandexClassifieds/envoy-api/pb/shiva/types/info"
	"github.com/YandexClassifieds/envoy-api/shiva"
)

type ShivaMock struct {
	data []*info.DeploymentInfo
}

func NewShivaMock(data []*info.DeploymentInfo) shiva.IService {
	return &ShivaMock{data: data}
}

func (s *ShivaMock) StatusInfo() []*info.DeploymentInfo {
	return s.data
}
