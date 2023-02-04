package service

import (
	"github.com/stretchr/testify/assert"
	"testing"
)

func TestElemExists(t *testing.T) {
	s1 := []string{
		"t1",
		"t2",
		"find",
	}

	key := "find"

	if !elemExists(s1, key) {
		t.Errorf("Can't find elem in slice")
	}
}

func TestRemove(t *testing.T) {
	s1 := []string{
		"t1",
		"t2",
		"killme",
	}

	s2 := []string{
		"t1",
		"t2",
	}

	data := remove(s1, "killme")
	assert.Equal(t, s2, data, "remove isnt working")
}
