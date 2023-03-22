void call() {
    sh "echo Removing existing ClusterRoleBinding..."

    sh "oc delete clusterrolebinding kong-kong-$NAMESPACE || true"
}

return this;
