package controllers

import (
	"testing"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"

	"a.yandex-team.ru/billing/configshop/pkg/core/entities/runtime"
	"a.yandex-team.ru/billing/configshop/pkg/storage"
	"a.yandex-team.ru/billing/library/go/billingo/pkg/xyaml"
)

func TestCombineOutputsAndErrors(t *testing.T) {
	g := NewRepeatedParentBlockManager(nil)

	testCases := []struct {
		name           string
		stateAndErrors []storage.BlockStateAndError
		expectedState  map[string]any
		expectedError  *runtime.BlockError
	}{
		{
			name:           "empty",
			stateAndErrors: []storage.BlockStateAndError{},
		},
		{
			name:           "simple outputs",
			stateAndErrors: []storage.BlockStateAndError{{State: map[string]any{"one": "two"}}},
			expectedState:  map[string]any{"one": []any{"two"}},
		},
		{
			name:           "simple error",
			stateAndErrors: []storage.BlockStateAndError{{Error: runtime.NewBlockError(runtime.ErrorCodeLogic, map[string]any{})}},
			expectedError:  runtime.NewBlockError(runtime.ErrorCodeLogic, map[string]any{"errors": map[int]xyaml.MapStr{0: map[string]any{}}}),
		},
		{
			name: "error and output",
			stateAndErrors: []storage.BlockStateAndError{
				{State: map[string]any{"one": "two"}, Error: runtime.NewBlockError(runtime.ErrorCodeLogic, map[string]any{})},
			},
			expectedError: runtime.NewBlockError(runtime.ErrorCodeLogic, map[string]any{"errors": map[int]xyaml.MapStr{0: map[string]any{}}}),
		},
		{
			name: "many outputs",
			stateAndErrors: []storage.BlockStateAndError{
				{State: map[string]any{"one": "two"}},
				{State: map[string]any{"one": "three"}},
				{State: map[string]any{"one": "four"}},
			},
			expectedState: map[string]any{"one": []any{"two", "three", "four"}},
		},
		{
			name: "some outputs and similar errors",
			stateAndErrors: []storage.BlockStateAndError{
				{State: map[string]any{"one": "two"}},
				{Error: runtime.NewBlockError(runtime.ErrorCodeLogic, map[string]any{"error": "kek"})},
				{State: map[string]any{"one": "three"}},
				{Error: runtime.NewBlockError(runtime.ErrorCodeLogic, map[string]any{"error": "cheburek"})},
			},
			expectedError: runtime.NewBlockError(runtime.ErrorCodeLogic, map[string]any{"errors": map[int]xyaml.MapStr{
				1: map[string]any{"error": "kek"},
				3: map[string]any{"error": "cheburek"},
			}}),
		},
		{
			name: "some outputs and different errors",
			stateAndErrors: []storage.BlockStateAndError{
				{State: map[string]any{"one": "two"}},
				{Error: runtime.NewBlockError(runtime.ErrorCodeLogic, map[string]any{"error": "kek"})},
				{State: map[string]any{"one": "three"}},
				{Error: runtime.NewBlockError(runtime.ErrorCodeInternal, map[string]any{"error": "cheburek"})},
			},
			expectedError: runtime.NewBlockError(runtime.ErrorCodeComposite, map[string]any{"errors": map[int]xyaml.MapStr{
				1: map[string]any{"error": "kek"},
				3: map[string]any{"error": "cheburek"},
			}}),
		},
	}

	for _, tc := range testCases {
		tc := tc
		t.Run(tc.name, func(t *testing.T) {
			var output runtime.ExecuteOutput
			require.NoError(t, g.combineOutputsAndErrors(tc.stateAndErrors, &output))

			assert.Equal(t, tc.expectedState, output.State)
			assert.Equal(t, tc.expectedError, output.Error)
		})
	}
}
