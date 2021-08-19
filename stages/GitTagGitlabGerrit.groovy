package stages

import com.epam.edp.stages.impl.ci.ProjectType
import com.epam.edp.stages.impl.ci.Stage

@Stage(name = "git-tag-gitlab-gerrit", buildTool = ["any"], type = [ProjectType.APPLICATION, ProjectType.LIBRARY,
        ProjectType.AUTOTESTS])
class GitTagGitlabGerrit {
    Script script

    void run(context) {
        script.dir("${context.workDir}") {
            script.withCredentials([script.usernamePassword(credentialsId: "${context["${context.gerritGitserver}"].credentialsId}",
                    passwordVariable: 'password', usernameVariable: 'username')]) {
                try {
                    script.sh """
                        git config --global user.email ${context["${context.gerritGitserver}"].user}@epam.com
                        git config --global user.name ${context["${context.gerritGitserver}"].user}
                        git tag -a ${context.codebase.vcsTag} -m 'Tag is added automatically by ${context["${context.gerritGitserver}"].user} user' || echo Tag already exists
                        git push ${context["${context.gerritGitserver}"].repoUrl} --tags
                    """
                } catch (Exception e) {
                    script.error "[ERROR] ${context.gerritGitserver} git tag creation failed due to exception: ${e}"
                }
            }
            
            script.withCredentials([script.sshUserPrivateKey(credentialsId: context["${context.gitlabGitserver}"].credentialsId,
                    keyFileVariable: 'key', passphraseVariable: '', usernameVariable: 'git_user')]) {
                try {
                    script.sh """
                        eval "\$(ssh-agent -s)"
                        ssh-add ${script.key}
                        mkdir -p ~/.ssh
                        ssh-keyscan -p ${context["${context.gitlabGitserver}"].port} ${context["${context.gitlabGitserver}"].host} >> ~/.ssh/known_hosts
                        git config --global user.email ${context["${context.gitlabGitserver}"].user}@epam.com
                        git config --global user.name ${context["${context.gitlabGitserver}"].user}
                        git tag -a ${context.codebase.vcsTag} -m 'Tag is added automatically by ${context["${context.gitlabGitserver}"].user} user' || echo Tag already exists
                        git push ${context["${context.gitlabGitserver}"].repoUrl} --tags
                        eval "\$(ssh-agent -k)"
                        """
                }
                catch (Exception e) {
                    script.println "[WARN] ${context.gitlabGitserver} git tag creation failed due to exception: ${e}"
                }
            }
        }
    }
}

return GitTagGitlabGerrit
