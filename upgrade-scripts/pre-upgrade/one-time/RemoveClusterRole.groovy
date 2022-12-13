void call() {
    sh "echo Removing existing ClusterRoler..."

    sh "oc delete clusterrole kong-cluster-role || true"
}

return this;
