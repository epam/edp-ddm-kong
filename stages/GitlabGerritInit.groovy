package stages

import com.epam.edp.customStages.impl.multigit.helper.GitServer
import com.epam.edp.customStages.impl.multigit.helper.GitServerType
import com.epam.edp.stages.impl.ci.ProjectType
import com.epam.edp.stages.impl.ci.Stage

@Stage(name = "gitlab-gerrit-init", buildTool = "any", type = [ProjectType.APPLICATION, ProjectType.LIBRARY,
        ProjectType.AUTOTESTS])
class GitlabGerritInit {
    Script script

    private final String GITSERVER_CR_NAME = "gitserver"

    void run(context) {
        context.projectName = context.git.repositoryRelativePath.replaceFirst("/", "")
        context.gitServers = [:]
        GitServerType.values().each {
            String name = it.getValue() == "gerrit" ? "gerrit-public" : it.getValue()
            String user = getGitserverSpecField(context, name, "gitUser")
            String host = getGitserverSpecField(context, name, "gitHost")
            String port = getGitserverSpecField(context, name, "sshPort")
            String credentialsId = getGitserverSpecField(context, name, "nameSshKeySecret")
            String repoUrl = "ssh://${user}@${host}:${port}/${context.projectName}"
            boolean isActive = (name == context.git.gitServerCrName)
            context.gitServers.put(it, new GitServer(name, user, host, port, credentialsId, repoUrl, isActive))
        }
    }

    private String getGitserverSpecField(context, String gitserver, String jsonPath) {
        return context.platform.getJsonPathValue(GITSERVER_CR_NAME, gitserver, ".spec.${jsonPath}")
    }
}
return GitlabGerritInit