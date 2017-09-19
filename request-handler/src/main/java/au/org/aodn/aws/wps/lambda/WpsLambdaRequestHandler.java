package au.org.aodn.aws.wps.lambda;

import au.org.aodn.aws.wps.AwsApiRequest;
import au.org.aodn.aws.wps.AwsApiResponse;
import au.org.aodn.aws.wps.WpsRequestHandler;
import au.org.aodn.aws.wps.status.StatusHelper;
import au.org.aodn.aws.wps.status.WpsConfig;
import com.amazonaws.services.kms.AWSKMS;
import com.amazonaws.services.kms.AWSKMSClientBuilder;
import com.amazonaws.services.kms.model.DecryptRequest;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Properties;

public class WpsLambdaRequestHandler implements RequestHandler<AwsApiRequest, AwsApiResponse> {

    private Logger LOGGER = LoggerFactory.getLogger(WpsLambdaRequestHandler.class);


    @Override
    public AwsApiResponse handleRequest(AwsApiRequest request, Context context) {

        WpsRequestHandler handler = new WpsRequestHandler();
        AwsApiResponse response = null;


        try
        {
            String envName = getEnvironmentName(context);
            Properties config = WpsConfig.getConfigProperties();

            //  TODO:  null check and act on null configuration
            config.setProperty(WpsConfig.ENVIRONMENT_NAME_CONFIG_KEY, envName);
            LOGGER.info("Loaded configuration.");

            //  Execute the request
            response = handler.handleRequest(request, config);

        }
        catch(Exception ex)
        {

            //  Bad stuff happened
            LOGGER.error("Exception running WPS Lambda function: " + ex.getMessage(), ex);
            //  TODO:  send caller a WPS error response
            AwsApiResponse.ResponseBuilder builder = new AwsApiResponse.ResponseBuilder();
            builder.statusCode(500);
            builder.body(StatusHelper.getExceptionReportString("Exception running WPS Lambda function: " + ex.getMessage(), "WpsError"));
            return builder.build();

        }

        return response;
    }





    /**
     * Decrypt the value of a named environment variable.
     *
     * @param keyName
     * @return  Decrypted value of the named environment variable.
     */
    private String getEncryptedEnvironmentVariable(String keyName)
    {
        return decryptKey(System.getenv(keyName));
    }


    /**
     * Decrypt an encrypted environment variable value.
     *
     * @param keyValue
     * @return  Decrypted key value.
     */
    private String decryptKey(String keyValue) {
        if(keyValue != null)
        {
            byte[] encryptedKey = Base64.decode(keyValue);
            AWSKMS client = AWSKMSClientBuilder.defaultClient();
            DecryptRequest request = new DecryptRequest()
                    .withCiphertextBlob(ByteBuffer.wrap(encryptedKey));
            ByteBuffer plainTextKey = client.decrypt(request).getPlaintext();
            return new String(plainTextKey.array(), Charset.forName("UTF-8"));
        }
        return null;
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

        LOGGER.info("Function name        : " + context.getFunctionName());
        LOGGER.info("Invoked function ARN : " + context.getInvokedFunctionArn() );
        if(!context.getFunctionName().equalsIgnoreCase(lastStringSegment))
        {
            //  There must be an alias
            envName = lastStringSegment;
        }
        else
        {
            //  No alias passed - use a default
            envName = WpsConfig.DEFAULT_ENV_NAME;
        }

        return envName;
    }
}
