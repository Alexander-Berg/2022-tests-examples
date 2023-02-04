package mock

import (
	"github.com/stretchr/testify/mock"
)

type Registry struct {
	mock.Mock
	EnableTestify bool
}

func (m *Registry) CheckImageExists(name, version string) error {
	if m.EnableTestify {
		m.Called(name, version)
	}
	return nil
}

func NewRegistry(withTestify bool) *Registry {
	return &Registry{
		EnableTestify: withTestify,
	}
}
