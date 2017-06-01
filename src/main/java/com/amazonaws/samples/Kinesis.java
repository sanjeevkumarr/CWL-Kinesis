/*
 * Copyright 2010-2013 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package com.amazonaws.samples;

import java.io.IOException;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagement;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagementClientBuilder;
import com.amazonaws.services.identitymanagement.model.CreateRoleRequest;
import com.amazonaws.services.identitymanagement.model.CreateRoleResult;
import com.amazonaws.services.identitymanagement.model.PutRolePolicyRequest;
import com.amazonaws.services.identitymanagement.model.PutRolePolicyResult;
import com.amazonaws.services.kinesis.AmazonKinesisClientBuilder;
import com.amazonaws.services.kinesis.model.CreateStreamRequest;
import com.amazonaws.services.kinesis.model.CreateStreamResult;
import com.amazonaws.services.logs.AWSLogs;
import com.amazonaws.services.logs.AWSLogsClientBuilder;
import com.amazonaws.services.logs.model.PutSubscriptionFilterRequest;
import com.amazonaws.services.logs.model.PutSubscriptionFilterResult;

/**
 * This sample demonstrates how to make basic requests to Amazon S3 using the
 * AWS SDK for Java.
 * <p>
 * <b>Prerequisites:</b> You must have a valid Amazon Web Services developer
 * account, and be signed up to use Amazon S3. For more information on Amazon
 * S3, see http://aws.amazon.com/s3.
 * <p>
 * <b>Important:</b> Be sure to fill in your AWS access credentials in
 * ~/.aws/credentials (C:\Users\USER_NAME\.aws\credentials for Windows users)
 * before you try to run this sample.
 */
public class Kinesis {
    
    private static final String ACC_NO= "12345678089";

    public static void main(String[] args) throws IOException {

        //Create the 
        AWSCredentialsProvider credentialsProvider = new ProfileCredentialsProvider();
        Kinesis k = new Kinesis();
//        k.createStream(credentialsProvider);
//        k.createRole(credentialsProvider);
        k.subscribeToAwsLogs(credentialsProvider);
        
        
        

//      
    }

    private void createStream(AWSCredentialsProvider credentialsProvider) {

        System.out.println("Creating new prg stream");
        AmazonKinesisClientBuilder kinesisClient;
        kinesisClient = AmazonKinesisClientBuilder.standard().withRegion(Regions.US_EAST_1).withCredentials(credentialsProvider);
        CreateStreamRequest createStreamRequest = new CreateStreamRequest();
        createStreamRequest.setShardCount(1);
        createStreamRequest.setStreamName("prg-metric-stream");
        CreateStreamResult createStreamResult = kinesisClient.build().createStream(createStreamRequest);
        System.out.println("POST creation " + createStreamResult.toString());

    }

    /**
     * Creates role for CWL to access kinesis
     *
     * @param credentialsProvider
     */
    private void createRole(AWSCredentialsProvider credentialsProvider) {

        //creating client for aws iam 
        AmazonIdentityManagement client = AmazonIdentityManagementClientBuilder.standard().withRegion(Regions.US_EAST_1).withCredentials(credentialsProvider).build();
        //Create role                    
        CreateRoleRequest request = new CreateRoleRequest();
        request.setRoleName("CWL-kinesis-role");
        String assumeRolePolicyDocument = "{\n"
                + "  \"Statement\": {\n"
                + "    \"Effect\": \"Allow\",\n"
                + "    \"Principal\": { \"Service\": \"logs.us-east-1.amazonaws.com\" },\n"
                + "    \"Action\": \"sts:AssumeRole\"\n"
                + "  }\n"
                + "}";
        request.setAssumeRolePolicyDocument(assumeRolePolicyDocument);
        
        System.out.println("Creating Role ");
        CreateRoleResult createRoleResult = client.createRole(request);
        System.out.println("Result " + createRoleResult.toString());
        

        System.out.println("attachig the policy  ");
        PutRolePolicyRequest policyRequest = new PutRolePolicyRequest();
        policyRequest.setPolicyName("prg-cwl-policyname");
        policyRequest.setRoleName("CWL-kinesis-role");
        String policyDocument = "{"
                + "  \"Statement\": [\n"
                + "    {\n"
                + "      \"Effect\": \"Allow\",\n"
                + "      \"Action\": \"kinesis:*\",\n"
                + "      \"Resource\": \"arn:aws:logs:us-east-1:"+ACC_NO+":log-group:RDSOSMetrics:*\"\n"
                + "    },\n"
                + "    {\n"
                + "      \"Effect\": \"Allow\",\n"
                + "      \"Action\": \"iam:PassRole\",\n"
                + "      \"Resource\": \"arn:aws:iam::"+ACC_NO+":role/CWL-kinesis-role\"\n"
                + "    }\n"
                + "  ]\n"
                + "}";
        policyRequest.setPolicyDocument(policyDocument);
        PutRolePolicyResult policyResult  =  client.putRolePolicy(policyRequest);

        System.out.println("POST attach " + policyResult.toString());
        
    }

    public void subscribeToAwsLogs(AWSCredentialsProvider credentialsProvider) {
        
        AWSLogs client = AWSLogsClientBuilder.standard().withRegion(Regions.US_EAST_1).withCredentials(credentialsProvider).build();
        
        System.out.println("Creating Destination ");
        PutSubscriptionFilterRequest filterRequest = new PutSubscriptionFilterRequest();
        filterRequest.setLogGroupName("RDSOSMetrics");
        filterRequest.setFilterName("MetricsFilter");
        filterRequest.setFilterPattern("");
        filterRequest.setDestinationArn("arn:aws:kinesis:us-east-1:"+ACC_NO+":stream/prg-metric-stream");
        filterRequest.setRoleArn("arn:aws:iam::"+ACC_NO+":role/CWL-kinesis-role");
        PutSubscriptionFilterResult filterResult = client.putSubscriptionFilter(filterRequest);
        System.out.println("filterResult " + filterResult.toString());
        
    }

}
