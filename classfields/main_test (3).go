package main

import (
	"github.com/hashicorp/go-version"
	"github.com/stretchr/testify/assert"
	"testing"
)

func TestVersion(t *testing.T) {
	_, err := version.NewVersion(SSHVersion)
	assert.NoError(t, err)
}
