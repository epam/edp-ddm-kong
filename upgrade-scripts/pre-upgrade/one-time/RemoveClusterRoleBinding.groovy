void call() {
    sh "echo Removing existing ClusterRoleBinding..."

    sh "oc delete clusterrolebinding kong-cluster-role || true"
}

return this;
