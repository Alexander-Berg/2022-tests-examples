#!/usr/bin/env sh
aws eks update-kubeconfig --name testing
kubectl delete job -l app.kubernetes.io/name=solver-tests
