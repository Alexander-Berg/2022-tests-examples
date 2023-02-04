package configops

import (
	"testing"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
)

func TestIn(t *testing.T) {
	t.Run("ints true", func(t *testing.T) {
		in, err := NewIn([]any{2, []any{4, 2, 3}})
		require.NoError(t, err)

		err = in.Validate(nil)
		require.NoError(t, err)

		isTrue, err := in.Evaluate(nil)
		require.NoError(t, err)

		assert.True(t, isTrue)
	})

	t.Run("ints false", func(t *testing.T) {
		in, err := NewIn([]any{2, []any{10, 7}})
		require.NoError(t, err)

		err = in.Validate(nil)
		require.NoError(t, err)

		isTrue, err := in.Evaluate(nil)
		require.NoError(t, err)

		assert.False(t, isTrue)
	})

	t.Run("strings true", func(t *testing.T) {
		in, err := NewIn([]any{"2", []any{"1", "2", "7"}})
		require.NoError(t, err)

		err = in.Validate(nil)
		require.NoError(t, err)

		isTrue, err := in.Evaluate(nil)
		require.NoError(t, err)

		assert.True(t, isTrue)
	})

	t.Run("strings false", func(t *testing.T) {
		in, err := NewIn([]any{"2", []any{"1", "7"}})
		require.NoError(t, err)

		err = in.Validate(nil)
		require.NoError(t, err)

		isTrue, err := in.Evaluate(nil)
		require.NoError(t, err)

		assert.False(t, isTrue)
	})

	t.Run("invalid types", func(t *testing.T) {
		in, err := NewIn([]any{3, []any{"1", "7"}})
		require.NoError(t, err)

		err = in.Validate(nil)
		require.Error(t, err)
		assert.Contains(t, err.Error(), "type")
	})

	t.Run("empty set", func(t *testing.T) {
		in, err := NewIn([]any{3, []any{}})
		require.NoError(t, err)

		err = in.Validate(nil)
		require.Error(t, err)
		assert.Contains(t, err.Error(), "empty")
	})
}

func TestEq(t *testing.T) {
	t.Run("ints true", func(t *testing.T) {
		eq, err := NewEq([]any{2, 2})
		require.NoError(t, err)

		err = eq.Validate(nil)
		require.NoError(t, err)

		isTrue, err := eq.Evaluate(nil)
		require.NoError(t, err)

		assert.True(t, isTrue)
	})

	t.Run("ints false", func(t *testing.T) {
		eq, err := NewEq([]any{2, 7})
		require.NoError(t, err)

		err = eq.Validate(nil)
		require.NoError(t, err)

		isTrue, err := eq.Evaluate(nil)
		require.NoError(t, err)

		assert.False(t, isTrue)
	})

	t.Run("strings true", func(t *testing.T) {
		eq, err := NewEq([]any{"2", "2"})
		require.NoError(t, err)

		err = eq.Validate(nil)
		require.NoError(t, err)

		isTrue, err := eq.Evaluate(nil)
		require.NoError(t, err)

		assert.True(t, isTrue)
	})

	t.Run("strings false", func(t *testing.T) {
		eq, err := NewEq([]any{"2", "7"})
		require.NoError(t, err)

		err = eq.Validate(nil)
		require.NoError(t, err)

		isTrue, err := eq.Evaluate(nil)
		require.NoError(t, err)

		assert.False(t, isTrue)
	})

	t.Run("invalid types", func(t *testing.T) {
		eq, err := NewEq([]any{3, "7"})
		require.NoError(t, err)

		err = eq.Validate(nil)
		require.Error(t, err)
		assert.Contains(t, err.Error(), "type")
	})
}
