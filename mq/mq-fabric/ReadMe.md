# MQ Fabric support

This module providers support for using Fabric to manage groups of brokers efficiently and to create tools to help visualise and manage things.

Firstly brokers are put into logical groups. A logical group is then used for discovery. So messaging clients just connect to a group & they don't care which broker in the group they connect to.

There are a few different ways to configure Logical Brokers which map to 1 or more physical brokers.

## A. Master / Slave Broker

In master slave we create a logical broker in a group. e.g. group A, broker1 and broker2.

Now we've 2 logical brokers. Each of these 2 logical brokers gets a Fabric profile. We can run 1 or more instances of each.

If we run 2 instances of broker1 profile in 2 separate containers; one is the master the other is the slave (with failover).

## B. Replicated Broker

In replicated mode you run N replicas of the same logical broker. Typically you'd run, say, 3 replicas; typically inheriting from the **mq-replicated** profile.

So you'd have 1 profile for a replica set of brokers and you deploy 3 instances of that container.

## C. N + 1 Broker

In N + 1 you define N brokers (broker name and configurations) in a group. e.g. group A has broker1 and broker2. Then you create N+1 containers each having all the N brokers inside.

This maps to a single profile for the N+1 group, which contains broker1 and broker2; then you'd run 3 of these containers; with 2 of the containers being master and one being slave to the other 2 brokers.

The **standby.group** (which defaults to the group) is used to ensure that each container is only master of 1 logical broker; to avoid running 3 containers and 1 of them being master of both broker1 and broker2

## Implementation details

Each logical Master/Slave broker, Replicated broker set, or N + 1 group of brokers maps to a Profile in Fabric. Each will have a single broker inside the profile configuration - apart from (C) N+1 Broker which will have N broker configurations.

Broker configurations are defined by the file: **org.fusesource.mq.fabric.server-$brokerName.properties** inside the profile

## Using mq-create

There follows examples on how to create each of the 3 kinds of broker configurations using the comand line shell:

### Master / Slave

Lets create a logical broker in group *a* and have a master and slave

    mq-create --group a broker1

    container-create-child --profile mq-a-broker1 root broker1c1
    container-create-child --profile mq-a-broker1 root broker1c2

### Replicated

Lets create a logical broker in group *a* with a logical broker1 replicated in 3 replicas

    mq-create --parent-profile=mq-replicated --group a broker1

    container-create-child --profile mq-a-broker1 root broker1c1
    container-create-child --profile mq-a-broker1 root broker1c2
    container-create-child --profile mq-a-broker1 root broker1c3


### N+1 with 2 brokers

Lets create a logical broker in group *a* and have a master and slave

    mq-create --profile broker1and2 --group a broker1
    mq-create --profile broker1and2 --group a broker2

    container-create-child --profile broker1and2 root brokerc1
    container-create-child --profile broker1and2 root brokerc2
    container-create-child --profile broker1and2 root brokerc3


### Try them all out

Copy/paste this example script to setup 3 profiles with a group each showing all the above in a demo:

    mq-create --group a --profile mq-a.masterSlave broker1
    mq-create --group b --profile mq-b.replicated --parent-profile=mq-replicated broker2
    mq-create --group c --profile mq-c.n.plus.1 broker3
    mq-create --group c --profile mq-c.n.plus.1 broker4

Then you'll have 3 profiles created, mq-a.masterSlave,  mq-b.replicated and mq-c.n.plus.1 which show master/slave, replicated, N+1 options with a profile for each.

So you're console should look like this (notice the scale column shows how many container instances are required for each profile).

![hawtio screenshot](https://raw.github.com/jboss-fuse/fuse/master/mq/mq-fabric/src/images/broker-requirements.png)

* clicking on the red badges on the right then lets you create containers.
* clicking on the Scale button on the toolbar will let you scale up/down the number of instances of each logical broker
* if you run Fabric on a cloud with an Auto-Scaler (like OpenShift) then it will automatically spin up all the brokers for you (neat eh! :)




