package configops

import (
	"testing"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"

	"a.yandex-team.ru/billing/library/go/billingo/pkg/xyaml"
)

func TestAnd(t *testing.T) {
	trueOp := xyaml.MapStr{"eq": []any{1, 1}}
	falseOp := xyaml.MapStr{"eq": []any{1, 2}}

	t.Run("multiple true", func(t *testing.T) {
		and, err := NewAnd([]any{trueOp, trueOp, trueOp})
		require.NoError(t, err)

		err = and.Validate(nil)
		require.NoError(t, err)

		isTrue, err := and.Evaluate(nil)
		require.NoError(t, err)

		assert.True(t, isTrue)
	})

	t.Run("multiple false", func(t *testing.T) {
		and, err := NewAnd([]any{trueOp, falseOp, trueOp})
		require.NoError(t, err)

		err = and.Validate(nil)
		require.NoError(t, err)

		isTrue, err := and.Evaluate(nil)
		require.NoError(t, err)

		assert.False(t, isTrue)
	})

	t.Run("one operand", func(t *testing.T) {
		and, err := NewAnd([]any{trueOp})
		require.NoError(t, err)

		err = and.Validate(nil)
		require.Error(t, err)
		assert.Contains(t, err.Error(), "operand")
	})

	t.Run("no operands", func(t *testing.T) {
		and, err := NewAnd([]any{})
		require.NoError(t, err)

		err = and.Validate(nil)
		require.Error(t, err)
		assert.Contains(t, err.Error(), "operand")
	})
}

func TestOr(t *testing.T) {
	trueOp := xyaml.MapStr{"eq": []any{1, 1}}
	falseOp := xyaml.MapStr{"eq": []any{1, 2}}

	t.Run("multiple true", func(t *testing.T) {
		or, err := NewOr([]any{falseOp, trueOp, falseOp})
		require.NoError(t, err)

		err = or.Validate(nil)
		require.NoError(t, err)

		isTrue, err := or.Evaluate(nil)
		require.NoError(t, err)

		assert.True(t, isTrue)
	})

	t.Run("multiple false", func(t *testing.T) {
		or, err := NewOr([]any{falseOp, falseOp, falseOp})
		require.NoError(t, err)

		err = or.Validate(nil)
		require.NoError(t, err)

		isTrue, err := or.Evaluate(nil)
		require.NoError(t, err)

		assert.False(t, isTrue)
	})

	t.Run("one operand", func(t *testing.T) {
		or, err := NewOr([]any{trueOp})
		require.NoError(t, err)

		err = or.Validate(nil)
		require.Error(t, err)
		assert.Contains(t, err.Error(), "operand")
	})

	t.Run("no operands", func(t *testing.T) {
		or, err := NewOr([]any{})
		require.NoError(t, err)

		err = or.Validate(nil)
		require.Error(t, err)
		assert.Contains(t, err.Error(), "operand")
	})
}
