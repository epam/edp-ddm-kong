/*
 * Copyright 2021 EPAM Systems.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.epam.edp.stages.impl.ci.impl.getversion


import com.epam.edp.stages.impl.ci.ProjectType
import com.epam.edp.stages.impl.ci.Stage

@Stage(name = "get-version", buildTool = ["docker"], type = [ProjectType.APPLICATION])
class GetVersionDockerApplication {
    Script script

    def setVersionToArtifact(context) {
        script.sh """
         kubectl patch codebasebranches.v2.edp.epam.com ${context.codebase.config.name}-${context.git.branch.replaceAll(/\//, "-")}  --subresource=status  --type=merge -p '{\"status\": {\"build\": "${context.codebase.currentBuildNumber}"}}'
        """
    }

    void run(context) {
        def token
        script.node('master') {
            def tokenSecret = context.platform.getJsonPathValue("serviceaccount", "jenkins", ".secrets[0].name")
            token = new String(context.platform.getJsonPathValue("secret", tokenSecret, ".data.token",
                    context.job.ciProject).decodeBase64())
        }

        script.sh "oc login --token=${token} --server=https://${context.job.dnsWildcard.replace('apps', 'api')}:6443"

        setVersionToArtifact(context)
        context.codebase.vcsTag = "build/${context.codebase.version}"
        context.codebase.isTag = "${context.codebase.version}"
        context.codebase.deployableModuleDir = "${context.workDir}"

        script.println("[JENKINS][DEBUG] Artifact version - ${context.codebase.version}")
        script.println("[JENKINS][DEBUG] VCS tag - ${context.codebase.vcsTag}")
        script.println("[JENKINS][DEBUG] IS tag - ${context.codebase.isTag}")
    }
}

return GetVersionDockerApplication