## Prototyping/Investigation Notes

### Notes on AWS Batch review

- optimal - 22GB of storage shared between containers
- for more need to use custom ami or EFS
- can mount efs volume directly - requires running in privileged mode - may be issues with I/O burst rate for small EFS volume - needs volume over 1TB to get a decent burst rate
- custom ami is simply amazon linux with large ebs volume configured and ecs-init package installed
- I needed to set required vcpus/memory and desired instance type to control how many containers would run on an instance (1 in this case).
- it can take some time to spin up shut down instances to handle requests e.g.  Start 3 small requests took 17-18 mins to submit, less than a min to run and then 38 -50 mins to shut down the unused instances
- if have a compute environment instance running e.g. for small jobs - job starts within 10 - 30 seconds normally
- Example setup - 3 queues/environments - 1 for small jobs - always one running - 1 for long timeseries jobs - doesn't require extra storage - started as required - 1 for long running/large storage jobs - requires extra storage started as required.

 - use custom ami for larger storage requirement jobs efs requires more management and limited in terms of throughput unless transient - need to confirm that its possible/practical!

### WPS Wrapper Notes

Looked at Version 1.0.0 - there is a 2.0 now but no implementations that I know of.

#### GetCapabilities

HTTP Get request using KVP mandatory
POST optional

Parameters
service/Request manadatory
AcceptVersions optional parameter
language optional parameter - only support en-US?

Need to include organisation/service related metadata/supported operations and a brief summary of available processes

Should be relatively straight forward ideally derived from configured organisation /service / process metadata 

Supported operations should match the implemented service and so probably belongs with the service implementation (GetCapabilites template for example)
Confugrable details should go into a configuration file - see config notes.  Process details should come from process metadata (see DescribeProcess below)

Simple implementation

 - Request validation/Returning appropriate exceptions
 - Return xml with process summary details

#### DescribeProcess

HTTP Get request using KVP mandatory
POST optional but easy to do (in java at least - see prototype - get it for free if parse KVP to JAXB classes) 

Need to provide description of available processes.  Should match implementation so bundle with process implementation code (e.g. DescribeProcess xml fragment)

Simple implementation

 - Request validation/Returning appropriate exceptions
 - Just include selected processes DecsribeProcess xml fragment

#### Execute

Need to support:

 - HTTP Post
 - one or more LiteralData inputs
 - one or more ComplexData outputs (result/provenance document)
 - storing outputs in a web accessible location. 
 - storing/updating response documents (asynchronous processing)

Don't need to support:

 - HTTP Get request
 - ComplexData/BoundingBoxData inputs
 - LiteralData/BoundingBoxData outputs
 - Other options we don't use such as uom, metadata, maximum megabytes, literal choice values, default values, allowed values, values reference

until we want to use them

For OGC compliance we would need to support the following mandatory requirements of the spec:

 - loading inputs from web accessible resources (kind of pointless for our Literal value inputs)
 - synchronous processing (not very useful for gogoduck/useful for netcdf output process)
 - encoding outputs in response
 - raw data output

Simple implementation

Request

 - Need to validate request against schema
 - Need to check that requested inputs/outputs match supported inputs/outputs for process 
 - Need to check request to see if its asking for anything we don't support and throw an exception -  i.e synchronous processing/encoding outputs in response/loading from web accessible services

Output 

 - Need to store status document and keep it updated during processing/retain it afterwards
 - Need to handle multiple outputs/mimetypes

#### API Gateway

Integrations

 - lambda - WPS API request looks to be beyond the capabilities of API Gateway mapping capabilities -  all requests mapped to one resource/method
 - AWS Service - batch not available
 - possible with lambda proxy - proxy all requests to lambda function which works out what the request is and dispatches to an appropriate process
 
 Prototype
 
 - prototyped submitting batch job via lambda java proxy - approx 3s cold startup time then 400-500 ms per request - had to set memory to 1500 to get this - initially getting 10 second cold startup time.
 - java 8 only
 - also tried doing some similar things in python submitting batch job was taking around 100 ms, added validation of xml requests using xmlschema - had to bumpup memory to stop python crashing (500 MB I think) and went to 3s each invocation - but not a fair comparison - didn't spend a lot of time trying to tune this

##### WPS Framework code 

Considered borrowing from
- geoserver
- pyWPS
- deegree
but geoserver's implementation is not ideal and all are tightly coupled with their own execution model

Some work to write our own but focus on minimum for marvl

#### Configuration for lambda

Refer https://www.concurrencylabs.com/blog/configure-your-lambda-function-like-a-champ-sail-smoothly/

Also looked at using lambda python for providing config which was way quicker (20 ms) than s3 (100 ms)
S3 probably easier to manage.

### TODO

 - agree on lambda language to use although would be good if wrapped process could be any
 - define batch job api - i.e. what gets passed to batch job and how - process inputs/output/mimetype selection
 - environment configuration - lambda aliases for example - job queues - configuration such as geoserver endpoints/s3 storage
 - wps/javaduck processing for container
 - building/deploying custom ami (needs updates applied etc)
 - ECS registry (or docker registry) setup
 - docker image
   - see if we can use xenial/packaged netcdf version rather than building netcdf manually - need to solve AWS role permissions for non amazonlinux base image - need ecs-init package installed?
 - build/deployment process for docker images 
 - configuration management/deployment process for compute environments, job queues, job definitions, IAM roles, lambda functions, configuration files, api gateway
 - request handler

