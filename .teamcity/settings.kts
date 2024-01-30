import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildFeatures.dockerSupport
import jetbrains.buildServer.configs.kotlin.buildFeatures.perfmon
import jetbrains.buildServer.configs.kotlin.buildSteps.dockerCommand
import jetbrains.buildServer.configs.kotlin.buildSteps.gradle
import jetbrains.buildServer.configs.kotlin.buildSteps.script
import jetbrains.buildServer.configs.kotlin.projectFeatures.dockerRegistry
import jetbrains.buildServer.configs.kotlin.triggers.vcs

/*
The settings script is an entry point for defining a TeamCity
project hierarchy. The script should contain a single call to the
project() function with a Project instance or an init function as
an argument.

VcsRoots, BuildTypes, Templates, and subprojects can be
registered inside the project using the vcsRoot(), buildType(),
template(), and subProject() methods respectively.

To debug settings scripts in command-line, run the

    mvnDebug org.jetbrains.teamcity:teamcity-configs-maven-plugin:generate

command and attach your debugger to the port 8000.

To debug in IntelliJ Idea, open the 'Maven Projects' tool window (View
-> Tool Windows -> Maven Projects), find the generate task node
(Plugins -> teamcity-configs -> teamcity-configs:generate), the
'Debug' option is available in the context menu for the task.
*/

version = "2023.11"

project {

    buildType(Build)

    features {
        feature {
            id = "PROJECT_EXT_2"
            type = "sonar-qube"
            param("useToken", "true")
            param("name", "Homelab Sonar")
            param("id", "0bff9787-1b19-49e4-8f93-c492b5271bf3")
            param("url", "http://192.168.1.4:9000")
            param("token", "scrambled:c3FhX2ZjYzM4MmNiOTU1N2E0YWE5MTNiN2YyYTUzNDNmNGFjNWQwNzJmMWY=")
        }
        dockerRegistry {
            id = "PROJECT_EXT_3"
            name = "Docker Registry"
            userName = "bmricha4"
            password = "credentialsJSON:475833c3-4083-40de-bfef-aa5815336d76"
        }
    }
}

object Build : BuildType({
    name = "Build"

    vcs {
        root(DslContext.settingsRoot)
    }

    steps {
        gradle {
            name = "gradle build"
            id = "gradle_runner"
            tasks = "clean build"
        }
        dockerCommand {
            name = "docker build"
            id = "DockerCommand"
            commandType = build {
                source = file {
                    path = "Dockerfile"
                }
                namesAndTags = """
                    bmricha4/cicd-demo:myapp-%build.number%
                    bmricha4/cicd-demo:latest
                """.trimIndent()
                commandArgs = "--pull"
            }
        }
        dockerCommand {
            name = "push image"
            id = "push_image"
            commandType = push {
                namesAndTags = """
                    bmricha4/cicd-demo:myapp-%build.number%
                    bmricha4/cicd-demo:latest
                """.trimIndent()
            }
        }
        script {
            name = "deploy to openshift"
            id = "deploy_to_openshift"
            scriptContent = """
                oc delete all -l app=cicd-demo
                oc new-app bmricha4/cicd-demo:latest --name=cicd-demo
                oc create route edge cicd-demo --service=cicd-demo
            """.trimIndent()
        }
        step {
            name = "Sonar Analysis"
            id = "Sonar_Analysis"
            type = "sonar-plugin"
            param("sonarServer", "0bff9787-1b19-49e4-8f93-c492b5271bf3")
        }
    }

    triggers {
        vcs {
        }
    }

    features {
        perfmon {
        }
        dockerSupport {
            loginToRegistry = on {
                dockerRegistryId = "PROJECT_EXT_3"
            }
        }
    }
})
