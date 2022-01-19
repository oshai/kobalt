package com.beust.kobalt.misc

import com.beust.kobalt.TaskResult
import com.beust.kobalt.api.Project
import com.google.inject.Inject
import java.io.File

class Git @Inject constructor(val localProperties: LocalProperties) {
  companion object {
    const val PROPERTY_ACCESS_TOKEN = "github.accessToken"
    const val PROPERTY_USERNAME = "github.username"
  }
    fun maybeTagRelease(
      project: Project, uploadResult: TaskResult, enabled: Boolean, annotated: Boolean,
      push: Boolean, tag: String, message: String
    ) : TaskResult {
        val result =
                if (uploadResult.success && enabled) {
                    val tagSuccess = tagRelease(project, annotated, push, tag, message)
                    if (! tagSuccess) {
                        TaskResult(false, errorMessage  = "Couldn't tag the project")
                    } else {
                        TaskResult()
                    }
                } else {
                    TaskResult()
                }
        return result
    }

    private fun tagRelease(project: Project, annotated: Boolean, push: Boolean, tag: String, message: String) : Boolean {
        val version = if (tag.isNullOrBlank()) project.version else tag
        val success = try {
            log(2, "Tagging this release as \"$version\"")
            val repo = org.eclipse.jgit.storage.file.FileRepositoryBuilder()
                    .setGitDir(File(KFiles.joinDir(project.directory, ".git")))
                    .readEnvironment()
                    .findGitDir()
                    .build()
            val git = org.eclipse.jgit.api.Git(repo)
            // jGit library will complain and not tag if setAnnotated(false)
            var ref = if (annotated) {
                git.tag().setAnnotated(annotated).setName(version).setMessage(message).call()
            } else {
                git.tag().setName(version).setMessage(message).call()
            }
            if (push) {
              val username = localProperties.get(PROPERTY_USERNAME, "no_DOC_URL")
              val password = localProperties.get(PROPERTY_ACCESS_TOKEN, "no_DOC_URL")
                val cp: org.eclipse.jgit.transport.CredentialsProvider =
                  org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider(username, password)
                git.push().setCredentialsProvider(cp).setPushTags().call()
            }
            true
        } catch(ex: Exception) {
            warn("Couldn't create tag ${version}: ${ex.message}", ex)
            false
        }

        return success
    }
}
