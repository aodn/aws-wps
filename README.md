##AWS Batch WPS Implementation

###Operations to Support

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

#### Execute [operation chosen for prototype]

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

**For OGC compliance we would need to support the following mandatory requirements of the spec:**

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



## Runtime Configuration

Several components will require configuration values to be passed to them at runtime - in order to configure various aspects of the software.
The configuration mechanism chosen should cater for various environments as required.

### Request Handler - hosted in AWS Lambda


Approach derived from https://www.concurrencylabs.com/blog/configure-your-lambda-function-like-a-champ-sail-smoothly/

We have implemented the prototype configuration mechanism as a single properties file being sourced from an Amazon S3 location.
The location of the configuration file is determined by reading the environment variables listed below.
 
####Environment variables:

1. CONFIG_PREFIX : indicates the name of the S3 bucket where the configuration file is located.
2. CONFIG_FILENAME : the configuration file name (i have set to wps-config.properties)
5. S3_AWS_REGION : the region name for S3 [Needed?]

####Managing configuration for multiple environments
Alias versions of the service code as per the suggestion in the aforementioned article - to identify which version is deployed to which environment.
Identified environments:  DEV, SYSTEST, RC, PROD (any others?)
The named bucket for configuration files will contain a folder for each of these environments - plus a '$LATEST' folder which will be a default config.

So: configuration for the DEV environment (for instance) will live in <CONFIG_PREFIX>/DEV/<CONFIG_FILENAME> 

When a version of the service is aliased with an environment name (DEV for instance), the alias name can be detected in the service code by using the 
invokedFunctionArn property of the Context.  If an alias has been assigned to that version of the service, it will appear at the end of the invokedFunctionArn string:

  Example with alias   : arn:aws:lambda:us-east-1:123456789012:function:helloStagedWorld:DEV - DEV indicates the alias name
  Example without alias: arn:aws:lambda:us-east-1:123456789012:function:helloStagedWorld

Code similar to this (this is Java) can be used to determine the presence of an alias:

    private String getEnvironmentName(Context context)
    {
        String envName = null;

        //  If an alias is set for the function version - then it will appear as the last part of the
        //  InvokedFunctionArn property of the context object.  If no alias is set, then the last
        //  part of the property will be the functionName.
        //
        //  Example -
        //    With an alias of 'DEV' for the function version invoked:
        //          arn:aws:lambda:us-east-1:123456789012:function:helloStagedWorld:DEV
        //
        //    With no alias set:
        //          arn:aws:lambda:us-east-1:123456789012:function:helloStagedWorld
        //
        //  We will use the alias name (if it exists) to indicate the environment the code is running in
        //  and hence which configuration file is loaded.
        int lastColonPosition = context.getInvokedFunctionArn().lastIndexOf(":");
        String lastStringSegment = context.getInvokedFunctionArn().substring(lastColonPosition + 1);

        LOGGER.log("Function name        : " + context.getFunctionName());
        LOGGER.log("Invoked function ARN : " + context.getInvokedFunctionArn() );
        if(!context.getFunctionName().equalsIgnoreCase(lastStringSegment))
        {
            //  There must be an alias
            envName = lastStringSegment;
        }
        else
        {
            //  No alias passed - use a default
            envName = DEFAULT_ENV_NAME;
        }

        return envName;
    }


ie:  If the last part of the invokedFunctionArn matches the functionName - there is no alias assigned.
     If they don't match, then the last part of the invokedFunctionArn is the alias name (the environment name for our purposes).

####Configuration file contents:

Simple properties file containing the following items -
    
    STATUS_S3_BUCKET - the base location (S3 bucket name) in which status files will be located
    STATUS_S3_FILENAME - the filename (AWS S3 key) that job status information will be written to.
    OUTPUT_S3_BUCKET - the S3 bucket that the output file will be written to
    OUTPUT_S3_FILENAME - the filename (AWS S3 key) for the output file
         **  Note the output file will be written to <OUTPUT_S3_BUCKET>/<AWS_BATCH_JOB_ID>/<OUTPUT_S3_FILENAME>
             Where <AWS_BATCH_JOB_ID> is the job ID for the AWS batch job raised to do the aggregation.
    JOB_NAME - the name of the AWS batch job to invoke
    JOB_QUEUE_NAME - the queue name for the AWS batch job [might be replaced by a more sophistocated mechanism to dynamically work out the queue to put the job on]
             ** The JOB_NAME & JOB_QUEUE properties will be replaced by a some sort of more sophisticated configuration which allows
                us to specify the job name and queue details dynamically based on some queue/job management logic.
                Current thinking is that large, long running jobs (assessed by doing an estimate of the size of the aggregation) 
                would be put on a different queue to small, fast running jobs.  
    AWS_REGION - the AWS region name for the AWS Batch job to invoke

Example:
    
    STATUS_S3_BUCKET=wps-lambda-status
    STATUS_S3_FILENAME=status.xml
    OUTPUT_S3_BUCKET=wps-lambda-status
    OUTPUT_S3_FILENAME=output.nc
    AWS_BATCH_JOB_NAME=javaduck
    AWS_BATCH_JOB_QUEUE_NAME=javaduck-small-in
    AWS_REGION=us-east-1
    
#### Aggregation Runner - Docker image hosted in AWS Batch

Sources the same configuration file used by the Lambda request handler.  
Relies on 3 environment variables being set to be able to locate the configuration file (CONFIG_PREFIX, CONFIG_FILENAME & AWS_S3_REGION).

The environment name (used to determine the specific configuration file to be used) is passed to the docker container as an environment variable
at runtime when the ExecuteOperation class submits an AWS batch job (ENVIRONMENT_NAME environment variable).



### DESIGN DECISIONS (so far)
 - Agreed to use Java (1.8 as the lambda implementation language)
 - Configuration provided by file stored in S3.  
   Multiple environments catered for by assigning an 'alias' to deployed lambda instances - which indicates the environment they are logically associated with.  
   This environment name is then used to load the correct configuration for that environment.
 - Status documents are stored in S3.  The configuration file contains a bucket name (STATUS_S3_BUCKET) where they are to be stored.
   The status file for a specific job will be stored in:
   
        <S3 bucket>/<AWS Batch JobID>/status.xml
   
   Status file is updated by both the request handler (lambda) + the Aggregation Runner (AWS batch)
 - Output file also stored in S3 in a location specified in configuration (OUTPUT_S3_BUCKET config item)

### TODO

 - define batch job api - i.e. what gets passed to batch job and how - process inputs/output/mimetype selection
 
   **Partially decided** : parameters are passed via command line arguments + environment variables to the class indicated as the docker ENTRYPOINT.
   This is fine for the prototype.
   Current implementation relies on the argument values being in a particular order.  Need to make this more generic + not reliant on position.
   Could append '<argumentname>=' to the front of each argument value passed + then build a map of names/values in the AggregationRunner handler code.
 - wps/javaduck processing for container
 - building/deploying custom ami (needs updates applied etc)
 - ECS registry (or docker registry) setup : use tags to indicate images use for each environment?  Have a non-prod registry?
 - docker image
 - see if we can use xenial/packaged netcdf version rather than building netcdf manually - need to solve AWS role permissions for non amazonlinux base image - need ecs-init package installed? **Resolved?**
 - build/deployment process for docker images
  
   **Resolved**  Build of docker image now performed as part of Maven build.  Push to AWS batch cater for as a separate target.
   
   **Underway**  Jenkin integration.
 - configuration management/deployment process for compute environments, job queues, job definitions, IAM roles, lambda functions, configuration files, api gateway.
 
   **Underway**  investigating CloudFormation to lay out + configure components.
 - Aggregation runner needs to output the requested output mimeType.  Currently outputs only NetCDF file.  Requested mimeType is being passed as an argument to the docker process.
 - Design and implement queue selection/job management logic. 
   We can support multiple queues & instances of the aggregation runner - so we need to decide how we route jobs.
   Ideally we want to improve the situation we currently have where large, long running jobs cause a bottleneck resulting in a backlog of jobs behind them.
   Queueing logic should be implemented in the Lambda request handler.
 - Exception handling.  Need to give caller meaningful error/exception responses when these conditions are encountered.
   WPS specifies error responses - which we will need to generate + return to clients.
   
   
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