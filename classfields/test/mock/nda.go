package mock

type NDAStub struct {
}

func NewNDAStub() *NDAStub {
	return &NDAStub{}
}

func (n *NDAStub) NdaUrl(url string) (string, error) {
	return url, nil
}
