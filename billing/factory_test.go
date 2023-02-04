package memoryruntime

import (
	"context"
	"testing"

	"github.com/golang/mock/gomock"
	"github.com/stretchr/testify/require"

	"a.yandex-team.ru/billing/configshop/pkg/core/entities/runtime"
	"a.yandex-team.ru/billing/configshop/pkg/core/entities/template"
	"a.yandex-team.ru/billing/configshop/pkg/core/entities/tplops"
	pipelinemock "a.yandex-team.ru/billing/configshop/pkg/core/runtime/pipeline/mock"
	configopstype "a.yandex-team.ru/billing/library/go/billingo/pkg/configops/type"
)

func TestIntegrationBlockExecFunc(t *testing.T) {
	ctrl := gomock.NewController(t)
	p := pipelinemock.NewMockPipeline(ctrl)

	p.EXPECT().SetFactory(gomock.Any()).AnyTimes()
	p.EXPECT().FinishBlock(gomock.Any(), gomock.Any()).
		DoAndReturn(func(ctx context.Context, b runtime.ExecuteOutput) error {
			require.Contains(t, b.State, "test_output")
			require.Len(t, b.State, 1)
			require.Equal(t, tplops.NilValue{}, b.State["test_output"])

			return nil
		})

	bm := NewFactory(p).NewBlockInstanceManager()

	execFunc, err := bm.GetBlockExecFunction(template.IntegrationBlock{}.Type())
	require.NoError(t, err)

	block := runtime.Block{
		ID:         666,
		GraphID:    999,
		Name:       "test_integration",
		Type:       "integration",
		ChangeType: runtime.UpdateActionType,
		OutputNames: []tplops.OutputValue{
			{Name: "test_output", Type: configopstype.ValueTypeString},
		},
		State: map[string]any{"test_output": "old_value"},
	}

	err = execFunc(context.Background(), &block, nil)
	require.NoError(t, err)
}
