package downtime

import (
	"testing"

	"github.com/spf13/cobra"
	"github.com/stretchr/testify/require"
)

func TestCmd_validateArgs(t *testing.T) {
	c := &cobra.Command{}

	err := validateArgs(c, []string{})
	require.EqualError(t, err, "accepts 1 arg, received 0")

	err = validateArgs(c, []string{"a", "b"})
	require.EqualError(t, err, "accepts 1 arg, received 2")

	err = validateArgs(c, []string{"a"})
	require.EqualError(t, err, "accepts valid time duration, like 2h, not: a")

	err = validateArgs(c, []string{"2h1m10s"})
	require.NoError(t, err)
}
