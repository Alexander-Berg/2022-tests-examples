package testing

import (
	"testing"

	"a.yandex-team.ru/infra/hostctl/internal/template"
	pb "a.yandex-team.ru/infra/hostctl/proto"
	"github.com/stretchr/testify/assert"
)

// TestCtxCompilerCompile checks if compiler can produce materializeable CompiledCtx
func TestCtxCompilerCompile(t *testing.T, c template.CtxCompiler) {
	def := &template.CtxDefinition{
		Vars: []*pb.Context_Var{
			{
				Name: "v1",
				Match: []*pb.Context_Match{
					{
						Exp: "h('test.yandex.net')",
						Val: "must-be-test",
					},
					{
						Exp: "default()",
						Val: "must-be-default",
					},
				},
			},
			{
				Name: "check",
				Match: []*pb.Context_Match{
					{
						Exp: "v1=='must-be-test'",
						Val: "check-{v1}",
					},
					{
						Exp: "default()",
						Val: "default-{v1}",
					},
				},
			},
		},
		FormatValues: "True",
	}
	cCtx, err := c.Compile(def)
	assert.NoError(t, err)
	hi := &pb.HostInfo{Hostname: "test.yandex.net", KernelRelease: "2.6.36-100"}
	TestCompiledCtxMaterialize(t, cCtx, hi, map[string]string{"v1": "must-be-test", "check": "check-must-be-test"})
	hi.Hostname = "test.search.yandex.net"
	TestCompiledCtxMaterialize(t, cCtx, hi, map[string]string{"v1": "must-be-default", "check": "default-must-be-default"})
}
