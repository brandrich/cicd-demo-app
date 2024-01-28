import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildFeatures.dockerSupport
import jetbrains.buildServer.configs.kotlin.buildFeatures.perfmon
import jetbrains.buildServer.configs.kotlin.buildSteps.gradle
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
        dockerRegistry {
            id = "PROJECT_EXT_4"
            name = "Docker Registry"
            userName = "bmricha4"
            password = "credentialsJSON:bfa2c468-cebf-4928-b443-a43c550ce300"
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
            id = "gradle_runner"
            tasks = "clean build"
            gradleWrapperPath = ""
        }
        dockerCommand {
            name = "build image"
            id = "__NEW_RUNNER__"
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
            id = "__NEW_RUNNER__"
            commandType = push {
                namesAndTags = """
            bmricha4/cicd-demo:myapp-%build.number%
            bmricha4/cicd-demo:latest
        """.trimIndent()
            }
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
                dockerRegistryId = "PROJECT_EXT_4"
            }
        }
    }
})
