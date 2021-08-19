package stages

import com.epam.edp.stages.impl.ci.ProjectType
import com.epam.edp.stages.impl.ci.Stage

@Stage(name = "gitlab-gerrit-init", buildTool = "any", type = [ProjectType.APPLICATION, ProjectType.LIBRARY,
        ProjectType.AUTOTESTS])
class GitlabGerritInit {
    Script script

    private final String GITSERVER_CR_NAME = "gitserver"

    void run(context) {
        context.gitlabGitserver = "gitlab"
        context.gerritGitserver = "gerrit"
        context.projectName = context.git.repositoryRelativePath.replaceFirst("/", "")

        context["${context.gitlabGitserver}"] = [:]
        context["${context.gitlabGitserver}"].user = getGitserverSpecField(context, context.gitlabGitserver, "gitUser")
        context["${context.gitlabGitserver}"].host = getGitserverSpecField(context, context.gitlabGitserver, "gitHost")
        context["${context.gitlabGitserver}"].port = getGitserverSpecField(context, context.gitlabGitserver, "sshPort")
        context["${context.gitlabGitserver}"].credentialsId = getGitserverSpecField(context, context.gitlabGitserver, "nameSshKeySecret")
        context["${context.gitlabGitserver}"].repoUrl = "ssh://${context["${context.gitlabGitserver}"].user}@${context["${context.gitlabGitserver}"].host}:" +
                "${context["${context.gitlabGitserver}"].port}/${context.projectName}"

        context["${context.gerritGitserver}"] = [:]
        context["${context.gerritGitserver}"].user = getGitserverSpecField(context, context.gerritGitserver, "gitUser")
        context["${context.gerritGitserver}"].host = context.platform.getJsonPathValue("route", "gerrit", ".spec.host")
        context["${context.gerritGitserver}"].port = getGitserverSpecField(context, context.gerritGitserver, "httpsPort")
        context["${context.gerritGitserver}"].credentialsId = "gerrit-ciuser-password"
        script.withCredentials([script.usernamePassword(credentialsId: "${context["${context.gerritGitserver}"].credentialsId}",
                passwordVariable: 'password', usernameVariable: 'username')]) {
            context["${context.gerritGitserver}"].repoUrl = "https://${context["${context.gerritGitserver}"].user}:${script.password}@" +
                    "${context["${context.gerritGitserver}"].host}:" +
                    "${context["${context.gerritGitserver}"].port}/${context.projectName}"
        }

        context["${context.gitlabGitserver}"].token = new String(context.platform.getJsonPathValue("secret",
                "git-epam-ciuser-api-token", ".data.token").decodeBase64())

    }

    private String getGitserverSpecField(context, String gitserver, String jsonPath) {
        return context.platform.getJsonPathValue(GITSERVER_CR_NAME, gitserver, ".spec.${jsonPath}")
    }
}

return GitlabGerritInit