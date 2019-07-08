#!/usr/bin/env groovy

/**
* Send notifications based on build status
*
* Inspiration from https://jenkins.io/blog/2017/02/15/declarative-notifications/
*/

def call(String buildStatus, Boolean alertTech = false) {
  // build status of null means successful
  buildStatus = buildStatus ?: 'SUCCESS'

  seconds = currentBuild.duration / 1000
  if (seconds < 60) {
    timeTaken = "${seconds}s"
  } else if (seconds < 3600) {
    timeTaken = "${seconds / 60}m ${seconds %60}s"
  } else {
    timeTaken = "${seconds / 3600}h ${seconds / 60}m ${seconds %60}s"
  }

  if (buildStatus == 'SUCCESS' && currentBuild.previousBuild.result == 'FAILURE') {
    status = 'Recovered'
    color = 'good'
    message = ":sweat_smile: Recovered (${timeTaken}) :nail_care:"
  } else if (buildStatus == 'SUCCESS') {
    status = 'Success'
    color = 'good'
    message = ":tada: Succeeded (${timeTaken}) :tada:"
  } else {
    status = 'failed'
    color = 'danger'
    message = ":crying_cat_face: Failed (${timeTaken}) :sadparrot:"
  }

  def repo = env.JOB_NAME.split("/")[1]
  def author = sh script: "git show -s --pretty=\"%an\" ${GIT_COMMIT}", returnStdout: true
  def gitSha = env.GIT_COMMIT.substring(0,7)
  def gitTitle = sh script: "git --no-pager show --pretty=format:%s -s HEAD", returnStdout: true
  def gitLink = "https://github.com/Futurelearn/${repo}/commit/${env.GIT_COMMIT}"
  def commit = "${gitTitle}\n(<${gitLink}|${gitSha}> / ${env.GIT_BRANCH})"

  def mailSubject = "Jenkins build ${status}: Futurelearn/${repo}"
  def mailBody = """
  Build ${env.BUILD_ID} ${status}: ${env.RUN_DISPLAY_URL}
  Branch: ${env.GIT_BRANCH}
  Committer: ${author}
  Repository: Futurelearn/${repo}
  Commit: ${gitLink}
  Time taken: ${timeTaken}
  """

  attachments = [
    [
      fallback: "Jenkins build ${env.BUILD_ID} ${status}",
      color: color,
      title: "Futurelearn/${repo} (#${env.BUILD_ID})",
      title_link: env.RUN_DISPLAY_URL,
      fields: [
        [
          title: 'Status',
          value: message,
          short: true
        ],
        [
          title: 'Committer',
          value: author,
          short: true
        ],
        [
          title: 'Commit',
          value: commit,
          short: false
        ]
      ]
    ]
  ]

  // Send notifications
  slackSend (channel: '#notifications', attachments: attachments)

  if (env.GIT_BRANCH == 'master' && alertTech == true) {
    if (buildStatus == 'SUCCESS' && currentBuild.previousBuild.result == 'FAILURE') {
      emailext (
          to: 'tech+jenkins@futurelearn.com',
          subject: mailSubject,
          body: mailBody,
        )
      slackSend (channel: '#tech', attachments: attachments)
    } else if (buildStatus == 'FAILURE' && currentBuild.previousBuild.result == 'SUCCESS') {
      emailext (
          to: 'tech+jenkins@futurelearn.com',
          subject: mailSubject,
          body: mailBody,
        )
      slackSend (channel: '#tech', attachments: attachments)
    }
  }
}
