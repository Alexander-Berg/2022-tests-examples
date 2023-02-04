package writer

import (
	"testing"

	"github.com/stretchr/testify/require"
)

func TestWrapStderrMessage(t *testing.T) {
	_, err := WrapStderrMessage([]byte(`some stuff`))
	require.NoError(t, err)
}
