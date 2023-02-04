package main

import (
	"github.com/stretchr/testify/assert"
	"testing"
)

func TestMatchServiceName(t *testing.T) {
	testProjectName := "shiva"
	testCases := map[string]bool{
		"shiva-test":    true,
		"shiva-testing": true,
		"shiva":         true,
		"shiva-gh-test": false,
		"shiva-gh":      false,
		"auto2-search":  false,
	}

	for k, v := range testCases {
		if !assert.Equal(t, v, matchServiceName(k, testProjectName)) {
			print(k)
		}
	}
}
