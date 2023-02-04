//go:build !release
// +build !release

package test_helpers

import (
	"math/rand"
	"strconv"
	"strings"
	"testing"
	"time"
)

const (
	JobPrefix     = "shiva-ci-"
	Image         = "shiva-ci"
	Version       = "1.2.11"
	UpdateVersion = "1.2.12"
)

var (
	r    *rand.Rand
	salt string
)

func init() {
	r = rand.New(rand.NewSource(time.Now().UnixNano()))
	salt = strconv.Itoa(time.Now().Minute()) + "-" + strconv.Itoa(r.Intn(100))
}

func ServiceName(t *testing.T) string {
	return JobPrefix + strings.ReplaceAll(strings.ReplaceAll(t.Name(), "_", "-"), "/", "-") + "-" + salt
}
