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

Docker image that performs the actual aggregation.  Its whats run on the EC2 instance, the request handler will post a 
batch task, where aws will call this worker given the docker image that is found in elastic container registry.

It stored under javaduck:latest repo for nonproduction-admin

### Job Status service

Lambda function that handles job status requests.
 
## In this project

- ```doc``` supporting documentation files
- ```request-handler```  a maven sub-module to build the lambda deployment package for the request handler
- ```aggregation-worker``` a maven sub-module to build the docker image for the aggregation worker.
- ```job-status-service``` a maven sub-module to build the lambda deployment package for the job status service
- ```lambda``` the output directory for lambda function build. These zip files need to push to the aws
- ```integration-tests``` a maven project that can be used to integration test a deployed AWS/WPS instance at a 
location specified by the WPS_ENDPOINT environment variable, that means you need to run the stack deploy. 
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

You need to run an instance of batch job and the two lambdas in order to run the integration-test.

> **_NOTE:_**  For testing you can use stack-dev-deploy.sh under aggregation-worker/scripts, which push docker image and lambda zips
> then you can run from cloud-deploy directory 
>
> $AWS_WPS_PROJECT/aggregation-worker/scripts/stack-dev-deploy.sh $AWS_WPS_PROJECT $TAG
> 
> where $TAG is a name you choose, this is used to avoid the default name "latest" which is use for production deployment. 

Click the stack you just created and go to the Resources section, find the "WPSRestApi" and click the link. Then
click the APIs on the top menu and find the ID of your API_ID.

WPS_ENDPOINT=https://$API_ID.execute-api.ap-southeast-2.amazonaws.com/LATEST/wps

```shell
Finally you can run the integrateion-test as below

cd integration-tests
WPS_ENDPOINT='https://w4fnovhz73.execute-api.ap-southeast-2.amazonaws.com/LATEST/wps' mvn verify
```

### How to check log on cloud
You can view the log in cloud:

Lambda
1. Goto your stack instance, click on it and goto the Resources section
2. Find RequestHandlerLambdaFunction for request handler lambda function or JobStatusLambdaFunction for another lambda
   function
3. Click the link, go to the Monitor tab, then hit "View logs in CloudWatch"

Batch Instance
1. In cloudwatch search /aws/batch/job

### How to delete
aws cloudformation delete-stack --stack-name YOUR_STACK_NAME --region ap-southeast-2

## To submit a request

A sample request and a script to submit it can be found in the requets directory.   The submit script should be modified
 to submit the request the to the required service url. 

    
## Deployment Pipeline/Process

TBC
