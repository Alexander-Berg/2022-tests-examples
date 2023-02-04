package actions

import (
	"context"
	"testing"

	"github.com/golang/mock/gomock"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"

	"a.yandex-team.ru/billing/configshop/pkg/core/entities"
	"a.yandex-team.ru/billing/configshop/pkg/core/entities/runtime"
	"a.yandex-team.ru/billing/configshop/pkg/core/entities/template"
	"a.yandex-team.ru/billing/configshop/pkg/storage/mock"
	"a.yandex-team.ru/billing/library/go/billingo/pkg/xyaml"
)

func TestActions_SentMessagePayload(t *testing.T) {
	ctrl := gomock.NewController(t)
	runtimeStorage := mock.NewMockRuntimeStorage(ctrl)
	templateStorage := mock.NewMockTemplateStorage(ctrl)
	actions := NewActions(runtimeStorage, templateStorage, nil, nil, nil)

	blockName := entities.BlockName("my_block")
	export := entities.Export{
		ConfigurationVersionID: 125,
		Environment:            "test",
	}
	graphID := runtime.GraphID(1846)

	tcs := []struct {
		name         string
		newBlockFn   func() *runtime.Block
		expectedArgs xyaml.MapStr
		errContains  string
	}{
		{
			name: "ok",
			newBlockFn: func() *runtime.Block {
				block := runtime.NewBlock()
				block.Type = template.IntegrationBlock{}.Type()
				block.Inputs = map[entities.VarName]any{
					{Block: "block1", Name: "value1"}: "value1",
					{Block: "block1", Name: "value2"}: 3,
					{Block: "block2", Name: "value2"}: "key4",
					{Block: "block2", Name: "value3"}: true,
				}
				block.Args = xyaml.MapStr{
					"key1": "$block1.value1",
					"key2": xyaml.MapStr{
						"$block2.value2": "$block2.value3",
					},
					"key3": []any{1, 2, "$block1.value2"},
				}
				return block
			},
			expectedArgs: xyaml.MapStr{
				"key1": "value1",
				"key2": xyaml.MapStr{
					"key4": true,
				},
				"key3": []any{1, 2, 3},
			},
		},
		{
			name: "inputs not filled yet",
			newBlockFn: func() *runtime.Block {
				block := runtime.NewBlock()
				block.Type = template.IntegrationBlock{}.Type()
				block.Args = xyaml.MapStr{
					"key1": "$block1.value1",
				}
				return block
			},
			errContains: "absent in inputs",
		},
		{
			name: "invalid block type",
			newBlockFn: func() *runtime.Block {
				block := runtime.NewBlock()
				block.Type = "something evil"
				return block
			},
			errContains: "only for integration blocks",
		},
	}

	for _, tc := range tcs {
		tc := tc
		t.Run(tc.name, func(t *testing.T) {
			templateStorage.EXPECT().GetGraphID(gomock.Any(), export).Return(graphID, nil).Times(1)
			runtimeStorage.EXPECT().GetBlockByName(gomock.Any(), graphID, blockName).Return(tc.newBlockFn(), nil).Times(1)

			args, err := actions.SentMessagePayload(context.Background(), blockName, export)
			if tc.errContains != "" {
				require.Error(t, err)
				assert.Contains(t, err.Error(), tc.errContains)
				return
			}
			require.NoError(t, err)
			assert.Equal(t, tc.expectedArgs, args)
		})
	}

}
