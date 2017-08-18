package au.org.aodn.aws.wps.lambda;


import au.org.aodn.aws.wps.AwsApiRequest;
import au.org.aodn.aws.wps.AwsApiResponse;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.kms.AWSKMS;
import com.amazonaws.services.kms.AWSKMSClientBuilder;
import com.amazonaws.services.kms.model.DecryptRequest;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.amazonaws.util.Base64;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Properties;

public class WpsLambdaRequestHandler implements RequestHandler<AwsApiRequest, AwsApiResponse> {

    private LambdaLogger LOGGER;
    private static final String DEFAULT_ENV_NAME = "$LATEST";
    private static final String ACCESS_KEY_ENV_VARIABLE_NAME = "ACCESS_KEY";
    private static final String SECRET_KEY_ENV_VARIABLE_NAME = "SECRET_KEY";
    private static final String REGION_NAME_ENV_VARIABLE_NAME = "S3_AWS_REGION";
    private static final String S3_BUCKET_ENV_VARIABLE_NAME = "CONFIG_S3_BUCKET";
    private static final String CONFIG_FILENAME_ENV_VARIABLE_NAME = "CONFIG_FILENAME";

    @Override
    public AwsApiResponse handleRequest(AwsApiRequest request, Context context) {
        LOGGER = context.getLogger();

        au.org.aodn.aws.wps.RequestHandler handler = new au.org.aodn.aws.wps.RequestHandler();
        AwsApiResponse response = null;

        //  Read environment variables
        //  Identify the location of the configuration file to use
        String accessKey = getEnvironmentVariable(ACCESS_KEY_ENV_VARIABLE_NAME);
        String secretKey = getEnvironmentVariable(SECRET_KEY_ENV_VARIABLE_NAME);
        String s3RegionName = getEnvironmentVariable(REGION_NAME_ENV_VARIABLE_NAME);
        String bucketName = getEnvironmentVariable(S3_BUCKET_ENV_VARIABLE_NAME);
        String configFilename = getEnvironmentVariable(CONFIG_FILENAME_ENV_VARIABLE_NAME);


        BasicAWSCredentials credentials = new BasicAWSCredentials(accessKey, secretKey);

        AmazonS3Client s3Client = (credentials == null) ? new AmazonS3Client() : new AmazonS3Client(credentials);
        Region region = Region.getRegion(Regions.fromName(s3RegionName));
        s3Client.setRegion(region);

        String envName = getEnvironmentName(context);

        //  Append an environment name to the front of the config filename
        if(envName != null)
        {
            configFilename  = envName + "/" + configFilename;
        }

        LOGGER.log("S3 Config file location: " + configFilename);

        S3Object configFile = s3Client.getObject(bucketName, configFilename);
        S3ObjectInputStream contentStream = configFile.getObjectContent();
        Properties config = new Properties();
        try
        {
            //  Load configuration from S3 file
            config.load(contentStream);

            //  Execute the request
            response = handler.handleRequest(request, config);

        }
        catch(IOException ioex)
        {
            //  Bad stuff happened
            LOGGER.log("Exception running WPS Lambda function: " + ioex.getMessage());
        }

        return response;
    }

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

    private String getEnvironmentVariable(String keyName)
    {
        return System.getenv(keyName);
    }


    private String getEncryptedEnvironmentVariable(String keyName)
    {
        return decryptKey(System.getenv(keyName));
    }

    private String decryptKey(String keyName) {
        byte[] encryptedKey = Base64.decode(keyName);
        AWSKMS client = AWSKMSClientBuilder.defaultClient();
        DecryptRequest request = new DecryptRequest()
                .withCiphertextBlob(ByteBuffer.wrap(encryptedKey));
        ByteBuffer plainTextKey = client.decrypt(request).getPlaintext();
        return new String(plainTextKey.array(), Charset.forName("UTF-8"));
    }


}
