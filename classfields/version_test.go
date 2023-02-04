package grpc

import (
	"github.com/hashicorp/go-version"
	"github.com/stretchr/testify/require"
	"testing"
)

func TestVersionParse(t *testing.T) {
	_, err := version.NewSemver(CurrentProtocolVersion)
	require.NoError(t, err)
	_, err = version.NewSemver(MinimumProtocolVersion)
	require.NoError(t, err)
}
