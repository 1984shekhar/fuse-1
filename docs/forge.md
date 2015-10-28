## Forge Addons

fabric8 comes with various [JBoss Forge](http://forge.jboss.org/) add ons to help improve your developer experience.

First [install Forge](http://forge.jboss.org/download) and run it:

    forge

you can install the Forge add ons via:

    addon-install --coordinate io.fabric8.forge:camel,2.2.55
    addon-install --coordinate io.fabric8.forge:devops,2.2.55
    addon-install --coordinate io.fabric8.forge:kubernetes,2.2.55

You should be able to see the new commands that are installed via:

    camel<TAB>
    devops<TAB>
    kubernetes<TAB>

You have to be a little patient; first time you try tab complete it can take a few seconds to figure out what's available.


### Camel

The [Apache Camel](http://camel.apache.org/) commands allow you to do all kinds of things on Camel projects such as add new components, endpoints, routes, languages.

### DevOps

The **DevOps** commands help you to 

* create new projects
* setup existing project for docker/fabric8 using the **fabric8-setup** command
* configure the DevOps configuration of a project via the [fabric8.yml file](fabric8YmlFile.html)
* generate new integration or system tests for a project

If you are inside a project try:

    forge devops-edit
    
to open the edit devops command which is a wizard that lets you configure the Docker and Fabric8 build metadata along with setting up the Jenkins workflow CD pipeline and linking the project to a team chat room and issue tracker.
    
### Kubernetes

Before you run forge make sure your **KUBERNETES_MASTER** environment variable points to where OpenShift V3 is running.

#### Applying JSON

If you are in a build which has [generated a kubernetes JSON file](mavenPlugin.html#generating-the-json) **target/classes/kubernetes.json** you can apply this via...

    kubernetes-apply --file target/classes/kubernetes.json

