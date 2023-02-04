package service_map

import (
	"testing"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
)

func TestServiceMap_IsProtected(t *testing.T) {
	assert.True(t, (&ServiceMap{Sox: true}).IsProtected())
	assert.True(t, (&ServiceMap{PciDss: true}).IsProtected())
	assert.False(t, (&ServiceMap{}).IsProtected())
}

func TestServiceMapLanguage(t *testing.T) {
	valid := []string{"Java", "Scala", "Go", "NodeJS", "Python", "Php", "C++", "Cpp"}
	invalid := []string{"Rust"}

	for _, pl := range valid {
		_, err := ServiceMapServiceLanguageString(pl)
		require.NoError(t, err)
	}

	for _, pl := range invalid {
		_, err := ServiceMapServiceLanguageString(pl)
		require.Error(t, err)
	}
}
