# Release Process
Bedrock is currently deployed via Sonatype Nexus and the web app is built into a docker container deployed on AWS Elastic Container Service (ECS). We use a self-built container running Tomcat 9 on Open JDK 11.

1) in the development branch, complete all changes, run "mvn clean test && gitgo" to check in the changes. You will need to be running mongod for the database tests to succeed, and you will need to be online with access to bedrock.brettonw.com for the network bag tests to succeed.
2) checkout the master branch, and merge the development branch changes to master.
3) make sure AWS-ECS command-line interface (CLI) tools are up to date.
5) use the "go-maven" tool to invoke the release process (I find the maven release step to be generally broken, so I built one that works). type "go release" in the shell and let it proceed.
6) deploy the site changes by navigating to AWS and stopping the task associated with the running instance. AWS will automatically relaunch a new instance with the updated container definition.
7) merge changes back to development (go updates the version and copies the website artifacts that have to be preserved).
