package tests

import (
	"testing"

	"a.yandex-team.ru/infra/hostctl/internal/units/tasks"
	pb "a.yandex-team.ru/infra/hostctl/proto"
	"github.com/stretchr/testify/assert"
)

func TestSimpleCond(t *testing.T) {
	c := &pb.Condition{}
	sc := tasks.NewSimpleCond(c)
	sc.True("true msg")
	assert.Equal(t, c.Status, "True")
	assert.Equal(t, c.Message, "true msg")

	sc.False("false msg")
	assert.Equal(t, c.Status, "False")
	assert.Equal(t, c.Message, "false msg")

	sc.Unknown("unk msg")
	assert.Equal(t, c.Status, "Unknown")
	assert.Equal(t, c.Message, "unk msg")
}

func TestSetupCond(t *testing.T) {
	c := &pb.Condition{}
	sc := tasks.NewSetupFailureAccumulatorCond(c)
	assert.Equal(t, c.Status, "True")
	sc.True("msg")
	assert.Equal(t, c.Status, "True")

	sc.False("f1")
	assert.Equal(t, c.Status, "False")
	assert.Equal(t, c.Message, "f1")

	sc.Unknown("u1")
	assert.Equal(t, c.Status, "False")
	assert.Equal(t, c.Message, "f1, u1")

	sc.True("t2")
	assert.Equal(t, c.Status, "False")
	assert.Equal(t, c.Message, "f1, u1")
}

func TestTeardownCond(t *testing.T) {
	c := &pb.Condition{}
	sc := tasks.NewTeardownFailureAccumulatorCond(c)
	assert.Equal(t, c.Status, "False")
	sc.False("msg")
	assert.Equal(t, c.Status, "False")

	sc.True("t1")
	assert.Equal(t, c.Status, "True")
	assert.Equal(t, c.Message, "t1")

	sc.Unknown("u1")
	assert.Equal(t, c.Status, "True")
	assert.Equal(t, c.Message, "t1, u1")

	sc.False("f2")
	assert.Equal(t, c.Status, "True")
	assert.Equal(t, c.Message, "t1, u1")
}
