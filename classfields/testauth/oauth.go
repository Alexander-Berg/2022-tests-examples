package testauth

import "github.com/YandexClassifieds/goLB"

type OAuthProvider struct {
	OAuthToken string
}

func (p *OAuthProvider) Token() []byte {
	return []byte(p.OAuthToken)
}

func (p *OAuthProvider) Type() goLB.TokenType {
	return goLB.TokenOAuth
}
