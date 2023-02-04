package processors

import (
	"testing"
	"time"

	"github.com/stretchr/testify/require"
)

func TestNewPayoutActions(t *testing.T) {
	_, err := NewPayoutActions("123", "322", "name", time.Now(),
		map[string]any{"namespace": "taxi"}, nil)
	require.NoError(t, err)
}

func TestNewPayoutActionsStringObjectID(t *testing.T) {
	_, err := NewPayoutActions("abc", "322", "name", time.Now(),
		map[string]any{"namespace": "taxi"}, nil)
	require.Error(t, err)
}

func TestNewPayoutActionsEmptyObjectID(t *testing.T) {
	_, err := NewPayoutActions("", "322", "name", time.Now(),
		map[string]any{"namespace": "taxi"}, nil)
	require.Error(t, err)
}

func TestNewPayoutActionsNoParams(t *testing.T) {
	_, err := NewPayoutActions("123", "322", "name", time.Now(), nil, nil)
	require.Error(t, err)
}

func TestNewPayoutActionsNoNamespace(t *testing.T) {
	_, err := NewPayoutActions("123", "322", "name", time.Now(),
		map[string]any{}, nil)
	require.Error(t, err)
}
