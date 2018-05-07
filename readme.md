Kubeflix: Kubernetes integration of Netflix components
------------------------------------------------------

This project provides [Kubernetes](http://kubernetes.io/) integration with [Netflix](https://netflix.github.io/) open source components such as [Hystrix](https://github.com/Netflix/Hystrix), [Turbine](https://github.com/Netflix/Turbine) and [Ribbon](https://github.com/Netflix/Ribbon).

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.fabric8.kubeflix/ribbon-discovery/badge.svg?style=flat-square)](https://maven-badges.herokuapp.com/maven-central/io.fabric8.kubeflix/ribbon-discovery/) ![Apache 2](http://img.shields.io/badge/license-Apache%202-red.svg)


<p align="center">
  <a href="http://github.com/fabric8io/kubeflix/">
  	<img src="https://raw.githubusercontent.com/fabric8io/kubeflix/master/docs/images/logo.png" height="200" width="200" alt="kubeflix logo"/>
  </a>
</p>

Specifically it provides:

* Kubernetes Instance Discovery for Turbine and Ribbon
* Kubernetes configuration and images for Turbine Server and Hystrix Dashboard
* Examples

### Turbine Discovery

Turbine is meant to discover and aggregate Hystrix metrics streams, so that its possible to extract and display meaningful information (e.g. display the aggregate stream on the dashboard).

#### How does Turbine work inside Kubernetes?
-----------------------------------

For use inside Kubernetes the project provides a special discovery module called turbine-discovery:

    <dependency>
        <groupId>io.fabric8.kubeflix</groupId>
        <artifactId>turbine-discovery</artifactId>
        <version>x.y.z</version>
    </dependency>    

This discovery module can be configured to discover instances either by name or by label. The default behavior is to discover all endpoints that contain the label ``hystrix.enabled=true``.

The supported labels are:

- hystrix.enabled:   Flag to indicate that the pod *(that provides the endpoint)* is exposing a hystrix stream
- hystrix.cluster:   Optional label to define the name of the Hystrix cluster.

From the endpoint Turbine obtains the ip and from configuration obtains information like port and path. This way it creates a URL to each individual hystrix.stream.

## Turbine Server

This project also provides a server that runs turbine with the Kubernetes discovery module preinstalled/preconfigured.

The server is configured out of the box to aggregate all the hystrix streams located in the current namespace.
If wider or narrower scope is required it can be done via configuration *(see below)*.

To build the server docker image and apply it to your kubernetes cluster:

    cd turbine-server
    mvn clean package fabric8:build fabric8:deploy   

The **Turbine Server** can be accessed at: http://turbine-server.vagrant.f8/turbine.stream (or wherever the turbine-server service is bound).

### Configuring the Turbine Server

The turbine server can be configured with two ways:

- using env variables
- using Kubernetes ConfigMap

#### Using env variables

You can specify any turbine configuration property as an environment variable by converting the property to upper case and replacing dots with underscores.

Example:

For creating the url to each individual hystrix stream, turbine is using the instance ip and appends a suffix specified by the ``turbine.instanceUrlSuffix``.
To configure this property via env variable you just need to set an environment variable with name ``TURBINE_INSTANCEURLSUFFIX``.

#### Using ConfigMap

The turbine server is powered by [Spring Cloud Kubernetes](https://github.com/fabric8io/spring-cloud-kubernetes) which among other allows you to externalize your ``application.yml`` in a [ConfigMap](http://kubernetes.io/docs/user-guide/configmap/).
This means that you can just create a ConfigMap named ``turbine-server`` and add the application.yml inside it:

        kind: ConfigMap
        apiVersion: v1
        metadata:
          name: turbine-server
        data:¬
         application.yml: | 
           turbine.instanceUrlSuffix: :8080/hystrix.stream

### Turbine Discovery scopes

Out of the box the turbine server will discover all endpoints in the current namespace with the required labels *(hystrix.enabled)*.
All discovered instances will be part of the ``default`` cluster. You can configure additional clusters and provide more fine grained configuration and which instances belong to each cluster.

    turbine.aggregator.clusters.<cluster name>=<namespace>.<service>    
        
So for example if we need to define a cluster called ``example`` that will encapsulate services ``bar`` and ``baz`` in the ``foo`` namespace, you can define:

    turbine.aggregator.clusters.example=foo.bar,foo.baz
    
or if we would like **all** service of the ``foo`` namespace:

    turbine.aggregator.clusters.example=foo.*

    
Note, that the option can be also provided as environment variables (e.g. TURBINE_AGGREGATOR_CLUSTER_FOO) or even via ConfigMap as described above.

## Ribbon Discovery

Ribbon is an IPC framework that among other provides load balancing features over multiple protocols.
This project provides a ribbon discovery module that can be used with ribbon in order to discover and loadbalance over Kubernetes endpoints.

### How does Ribbon work inside Kubernetes?
-----------------------------------

For use inside Kubernetes the project provides a special discovery module called ribbon-discovery:

    <dependency>
        <groupd>io.fabric8.kubeflix</groupId>
        <artifactId>ribbon-discovery</artifactId>
        <version>x.y.z</version>
    </dependency>   

Now, all you need to do is to tell Ribbon that you need to use the [KubernetesServerList](ribbon-discovery/src/main/java/io/fabric8/kubeflix/ribbon/KubernetesServerList.java) for server discovery.

In java code:

    IClientConfig config = IClientConfig.Builder.newBuilder()
                                    .withDefaultValues()
                                    .build()                                   
                                    .set(IClientConfigKey.Keys.NIWSServerListClassName, KubernetesServerList.class.getName())

The [KubernetesServerList](ribbon-discovery/src/main/java/io/fabric8/kubeflix/ribbon/KubernetesServerList.java) will use the created configuration in order to discover service endpoints.

The endpoint name used is always the same as the name of the client.
The namespace and the service port of interest need to be specified in the **IClientConfig** using the following keys:

- KubernetesNamespace
- PortName

for more information look at: [KubernetesConfigKey](ribbon-discovery/src/main/java/io/fabric8/kubeflix/ribbon/KubernetesConfigKey.java).


## Hystrix Dashboard

The Hystrix Dashboard is a web application which allows you to visualize one or more Hystrix streams.
For use inside Docker/Kubernetes this project provides a wrapper module called hystrix-dashboard.

This version of the dashboard comes pre-configured with the **Turbine Server** stream.

![Hystrix Dashboard Configuration Screen](images/dashboard-configuration.png "Hystrix Dashboard Configuration Screen")


To build:

    cd hystrix-dashboard
    mvn clean package fabric8:build fabric8:deploy   

The dashboard can be accessed at:  http://hystrix-dashboard.vagrant.f8:8080 (or wherever the hystrix-dashboard service is bound).

## Examples:

* [Hello World](examples/hello-world/readme.md)
* [Loan Broker](examples/loanbroker/readme.md)
* [Zuul Proxy](examples/zuul-proxy/readme.md)
