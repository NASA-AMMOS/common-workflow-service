pipeline {
    agent {
        node {
            label 'miplci3.jpl.nasa.gov'
        }
    }

    stages {

        stage('Start Stages') {
            steps {
                echo "--Stages Started--"
                pwd

                sh """
                pwd
                """

                sh """
                ../.././start.sh
                """
            }
        }

        stage('Branches Check') {
            when {
                branch "*"
            }
            steps {
                echo 'BRANCH NAME: ' + env.BRANCH_NAME
            }
        }

        stage('PR Branches Check') {
            when {
                branch "PR-*"
            }
            steps {
                echo 'PR BRANCH NAME: ' + env.BRANCH_NAME
            }
        }

        stage('Test Script: run_ci.sh') {
            steps {
                sh """
                pwd
                """

                sh """
                ../.././run_ci.sh
                """
            }
        }

    }
}