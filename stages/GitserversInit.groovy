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

package stages

import com.epam.edp.customStages.impl.multigit.helper.GitServer
import com.epam.edp.customStages.impl.multigit.helper.GitServerType
import com.epam.edp.stages.impl.ci.ProjectType
import com.epam.edp.stages.impl.ci.Stage

@Stage(name = "gitlab-gerrit-init", buildTool = "any", type = [ProjectType.APPLICATION, ProjectType.LIBRARY,
        ProjectType.AUTOTESTS])
class GitserversInit {
    Script script

    private final String GITSERVER_CR_NAME = "gitserver"

    void run(context) {
        script.println("[DEBUG] Init git servers")
        context.projectName = context.git.repositoryRelativePath.replaceFirst("/", "")
        context.gitServers = [:]
        String jenkinsSSHDir = "${script.env.NODE_NAME}" == "master" ? "/var/lib/jenkins/.ssh" : "/home/jenkins/.ssh"
        script.println("[DEBUG] Jenkins SSH dir: ${jenkinsSSHDir}")
        GitServerType.values().each {
            String name = it.getValue()
            if(name == "gerrit")
                name = "${name}-public"
            String user = getGitserverSpecField(context, name, "gitUser")
            String host = getGitserverSpecField(context, name, "gitHost")
            String port = getGitserverSpecField(context, name, "sshPort")
            String credentialsId = getGitserverSpecField(context, name, "nameSshKeySecret")
            String repoUrl = "ssh://${user}@${host}:${port}/${context.projectName}"
            boolean isActive = (name == context.git.gitServerCrName)
            context.gitServers.put(it, new GitServer(name, user, host, port, credentialsId, repoUrl, isActive))
            script.sh "mkdir -p ${jenkinsSSHDir}; ssh-keyscan -p ${port} ${host} " +
                    "| tee -a ${jenkinsSSHDir}/known_hosts"
        }
        String buildUser = script.sh(script: "whoami", returnStdout: true).trim()
        script.println("[DEBUG] build user: ${buildUser}")
        if (buildUser == "root") {
            script.println("Running as root so additional copy of known hosts file is required")
            script.sh "cp ${jenkinsSSHDir}/known_hosts /root/.ssh/known_hosts"
        }
    }

    private String getGitserverSpecField(context, String gitserver, String jsonPath) {
        return context.platform.getJsonPathValue(GITSERVER_CR_NAME, gitserver, ".spec.${jsonPath}")
    }
}
return GitserversInit
