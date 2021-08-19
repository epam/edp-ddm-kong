package stages

import com.epam.edp.stages.impl.ci.ProjectType
import com.epam.edp.stages.impl.ci.Stage

@Stage(name = "create-branch-gerrit", buildTool = ["any"], type = [ProjectType.APPLICATION, ProjectType.AUTOTESTS,
        ProjectType.LIBRARY])
class CreateBranchGerrit {
    Script script

    void run(context) {
        script.dir("${context.workDir}") {
            script.println "[DEBUG] Using credentials: ${context["${context.gerritGitserver}"].credentialsId}"
            script.withCredentials([script.usernamePassword(credentialsId: "${context["${context.gerritGitserver}"].credentialsId}",
                    passwordVariable: 'password', usernameVariable: 'username')]) {
                try {
                    script.httpRequest authentication: "${context["${context.gerritGitserver}"].credentialsId}",
                            httpMode: 'PUT',
                            consoleLogResponseBody: true,
                            quiet: false,
                            responseHandle: 'NONE',
                            url: "https://${context["${context.gerritGitserver}"].host}/a/projects/${context.projectName.replaceAll('/', '%2F')}",
                            validResponseCodes: '201,409',
                            wrapAsMultipart: false
                    script.sh """
                        git config --global user.email ${context["${context.gerritGitserver}"].user}@epam.com
                        git config --global user.name ${context["${context.gerritGitserver}"].user}
                        if [[ -z `git ls-remote --heads ${context["${context.gerritGitserver}"].repoUrl} ${context.job.releaseName}` ]]; then
                            git branch ${context.job.releaseName} ${context.job.releaseFromCommitId}
                            git push ${context["${context.gerritGitserver}"].repoUrl} ${context.job.releaseName}
                        fi
                    """
                }
                catch (Exception ex) {
                    script.error "[JENKINS][ERROR] Create branch has failed with exception - ${ex}"
                }

            }
        }
    }
}

return CreateBranchGerrit
