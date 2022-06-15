# AWS/WPS

Project to build components for an [OGC Web Processing Service](http://www.opengeospatial.org/standards/wps) providing NetCDF aggregation services on AWS   

## Licensing
This project is licensed under the terms of the GNU GPLv3 license.

## When deployed to AWS

![Overview](doc/Overview.png)

## Components built using this project

### Request Handler

Lambda function that handles OGC WPS requests
  - GetCapabilites - describes services provided
  - DescribeProcess - describes supported aggregation processes
  - Execute - submits an aggregation request to AWS Batch

### Aggregation worker

Docker image that performs the actual aggregation.  Its whats run on the EC2 instance.

### Job Status service

Lambda function that handles job status requests.
 
## In this project

- ```doc``` supporting documentation files
- ```request-handler```  a maven sub-module to build the lambda deployment package for the request handler
- ```aggregation-worker``` a maven sub-module to build the docker image for the aggregation worker.
- ```job-status-service``` a maven sub-module to build the lambda deployment package for the job status service
- ```integration-tests``` a maven project that can be used to integration test a deployed AWS/WPS instance at a 
location specified by the WPS_ENDPOINT environment variable    
- ```requests``` contains a demo request that can be submitted to a deployed AWS/WPS instance
- ```wps-cloudformation-template.yaml``` a cloud formation template for creating AWS components of the AWS/WPS instance

## Supported aggregation processes

 * [gs:GoGoDuck - gridded data aggregator](doc/GoGoDuck.md)
 
## Limitations

Only OGC WPS features required to support the Portal/MARVL have been implemented. 
The following mandatory features from the spec are not supported:

 - loading inputs from web accessible resources
 - synchronous processing
 - encoding outputs in response
 - raw data output
 
## To Build

Requirements:
 
  * maven 3
  * java 11
  * docker

```
$ mvn clean package
```

## To run integration tests

### Create environment for testing in nonproduction env
wps-cloudformation-template.yml is used to create the followings:
- AWS Batch: Module [aggregation-worker](aggregation-worker) contains the code to do the batch job. During the build,
  the maven will invoke docker-build.sh under scripts to create a docker image. You can find the image with the command 
  "docker images" assume you have installed docker.
- AWS Lambda: Module [job-status-service](job-status-service) and [request-handler](request-handler) are two lambda 
  functions created with this template. After running the maven build, the two module will create a zip package and
  place it in the [lambda](lambda) folder.

You need to run an instance of batch job and the two lambdas in order to run the integration-test. Execute the following
command under [cloud-deploy](https://github.com/aodn/cloud-deploy) repo. Details can be found 
[here](https://github.com/aodn/cloud-deploy/blob/master/doc/ansible.md)

####Step 1
Upload the docker image that you created in build to aws, please run the ./aggregation-worker/scripts/docker-deploy.sh 
and <font color="red">must give it a tag name</font>. ie. ./aggregation-worker/scripts/docker-deploy.sh tagname

####Step 2
Run the below command in cloud-deploy directory 

```shell
$AWS_WPS_ROOT/aggregation-worker/scripts/stack-dev-deploy.sh $AWS_WPS_ROOT tagname
```

It will create the application stack and you can find it under AWS Cloudformation, search your stack name with
the tagname value. 

####Step 3
Click the stack you just create and go to the Resources section, find the "WPSRestApi" and click the link. Then
click the APIs on the top menu and find the ID of your API.

WPS_ENDPOINT=https://$API_ID.execute-api.ap-southeast-2.amazonaws.com/LATEST/wps for example something like this 
will run the integration test

```shell
example:
cd integration-tests
WPS_ENDPOINT='https://w4fnovhz73.execute-api.ap-southeast-2.amazonaws.com/LATEST/wps' mvn verify
```

### How to debug
You can view the log in cloud:
1. Goto your stack instance, click on it and goto the Resources section
2. Find RequestHandlerLambdaFunction for request handler lambda function or JobStatusLambdaFunction for another lambda
   function
3. Click the link, go to the Monitor tab, then hit "View logs in CloudWatch"

### How to delete
aws cloudformation delete-stack --stack-name YOUR_STACK_NAME --region ap-southeast-2

## To submit a request

A sample request and a script to submit it can be found in the requets directory.   The submit script should be modified
 to submit the request the to the required service url. 

    
## Deployment Pipeline/Process

TBC
