package mock

import (
	"encoding/json"
	"strings"

	"github.com/YandexClassifieds/shiva/common"
	"github.com/YandexClassifieds/shiva/pkg/i/locker"
)

// context like deployment.LockContext
type MockContext struct {
	Type        common.Type `json:"type"`
	Layer       string      `json:"layer"`
	ServiceName string      `json:"service_name"`
	Branch      string      `json:"branch"`
}

func NewContext(t common.Type, layer string, serviceName string, branch string) *MockContext {
	return &MockContext{
		Type:        t,
		Layer:       layer,
		ServiceName: serviceName,
		Branch:      branch,
	}
}

func (ctx *MockContext) Name() string {

	return strings.Join([]string{ctx.Layer, ctx.ServiceName, ctx.Branch}, "_")
}

func (ctx *MockContext) Compare(undefinedCtx locker.Context) bool {

	ctx2, ok := undefinedCtx.(*MockContext)
	if !ok {
		return false
	}

	return ctx.Type == ctx2.Type &&
		ctx.Layer == ctx2.Layer &&
		ctx.ServiceName == ctx2.ServiceName &&
		ctx.Branch == ctx2.Branch

}

func (ctx *MockContext) AllowSteal(undefinedCtx locker.Context) bool {

	ctx2, ok := undefinedCtx.(*MockContext)
	if !ok {
		return false
	}

	return ctx.Type == common.Cancel && ctx2.Type != common.Cancel
}

func (ctx *MockContext) New() locker.Context {
	return &MockContext{}
}

func (ctx *MockContext) Marshal() ([]byte, error) {
	return json.Marshal(ctx)
}

func (ctx *MockContext) Unmarshal(b []byte) error {
	return json.Unmarshal(b, ctx)
}
