package client

import (
	"github.com/stretchr/testify/assert"
	"testing"
)

func TestYsonString(t *testing.T) {
	assert.Equal(t, "this-is-a-string-here", DecodeYsonString(YsonString("this-is-a-string-here")))
}
