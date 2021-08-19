package stages

import com.epam.edp.stages.impl.ci.ProjectType
import com.epam.edp.stages.impl.ci.Stage

@Stage(name = "replication", buildTool = "any", type = [ProjectType.APPLICATION, ProjectType.LIBRARY,
        ProjectType.AUTOTESTS])
class Replication {
    Script script

    void run(context) {
        context.passiveGitserver = context.git.gitServerCrName == context.gerritGitserver ? context.gitlabGitserver
                : context.gerritGitserver

        script.println "[INFO] PASSIVE GIT SERVER IS: ${context.passiveGitserver}"

        script.dir("${context.workDir}") {
            if (context.passiveGitserver == context.gerritGitserver) {
                script.withCredentials([script.usernamePassword(credentialsId: "${context["${context.passiveGitserver}"].credentialsId}",
                        passwordVariable: 'password', usernameVariable: 'username')]) {
                    try {
                        script.sh """
                        git config --global user.email ${context["${context.passiveGitserver}"].user}@epam.com
                        git config --global user.name ${context["${context.passiveGitserver}"].user}
                        git checkout -b ${context.git.branch}
                        git push ${context["${context.passiveGitserver}"].repoUrl} --all --force    
                        """
                    } catch (Exception e) {
                        script.error "[ERROR] replication to ${context.passiveGitserver} has failed: ${e}"
                    }
                }
            } else {
                script.withCredentials([script.sshUserPrivateKey(credentialsId: context["${context.passiveGitserver}"].credentialsId.trim(),
                        keyFileVariable: 'key', passphraseVariable: '', usernameVariable: 'git_user')]) {
                    try {
                        script.sh """
                        mkdir -p ~/.ssh
                        eval "\$(ssh-agent -s)"
                        pgrep ssh-agent
                        ssh-add -D
                        ssh-add -k ${script.key}
                        ssh-add -l
                        whoami
                        echo \$HOME
                        ssh-keyscan -p ${context["${context.passiveGitserver}"].port} ${context["${context.passiveGitserver}"].host} | tee -a ~/.ssh/known_hosts
                        cp /home/jenkins/.ssh/known_hosts /root/.ssh/known_hosts || true
                        git config --global user.email ${context["${context.passiveGitserver}"].user}@epam.com
                        git config --global user.name ${context["${context.passiveGitserver}"].user}
                        git checkout -b ${context.git.branch}
                        git push ${context["${context.passiveGitserver}"].repoUrl} --all --force
                        eval "\$(ssh-agent -k)"
                    """
                    } catch (Exception e) {
                        script.println "[WARN] replication to ${context.passiveGitserver} has failed: ${e}"
                    }
                }
            }
        }
    }
}

return Replication