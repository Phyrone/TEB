pipeline {
  agent any
  stages {
    stage('Clean') {
      steps {
        sh '''mvn clean
'''
      }
    }
    stage('Compile') {
      steps {
        sh 'mvn compile'
      }
    }
    stage('Package') {
      steps {
        sh 'mvn package'
      }
    }
    stage('Archive') {
      steps {
        archiveArtifacts(onlyIfSuccessful: true, artifacts: '**/target/*.jar', excludes: '**/target/original-*.jar')
      }
    }
  }
}