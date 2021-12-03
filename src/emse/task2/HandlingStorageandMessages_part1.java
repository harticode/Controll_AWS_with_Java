package emse.task2;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.waiters.WaiterResponse;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketResponse;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.waiters.S3Waiter;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlResponse;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SqsException;


public class HandlingStorageandMessages_part1 {
	
	public static void main(String[] args) {
		
		//The default Region in my case, feel free to change it 
	    Region region = Region.EU_CENTRAL_1;
	    
	    final String HOWTOUSE = "\n" +
                "Usage:\n" +
                "   Set in the Code  <queueName>\n" +
                "   Set in the Code  <bucket>\n" +
                "   Set in the Code  <filename>\n" +
                "   Set in the Code  <filePath>\n" +
                "Where:\n" +
                "   queueName - Set a name to your Queue \n" +
                "   bucket - Set a name for your bucket (Try a long unique name)\n" +
                "   filename - Find the name of the file in the system\n" +
                "   filePath - Get the path of the file you want to send in the system\n";

	    String queueName = "queue_hamzaAitBaali";
	    String bucket = "testhamzaaitbaali0002";
	    String filename = "values.csv";
	    String filePath = "D:\\Users\\harti\\Downloads\\"+filename;
	    
	    if (queueName.length() == 0 || bucket.length() == 0 || filename.length() == 0 || filePath.length() == 0) {
	        System.out.println(HOWTOUSE);
	        System.exit(1);
   		}
	    
	    //The Message( the delimiter in a semicolon )
	    String data = bucket + ";" + filename;
	    
	    final S3Client s3 = S3Client.builder().region(region).build();
	    final SqsClient sqs = SqsClient.builder().region(region).build();
	    
	    //Create the Queue in the Amazon SQS
	    //And get the queue **URL VERY IMPORTANT FOR THE NEXT PART**
	    //Copy the URL displayed in the console
	    String queueUrl = createQueue(sqs, queueName);
	    
	    //Check if bucket exist already or not
	    if(!S3bucketExist(s3, bucket)) {
	    	createBucket( s3, bucket);
	    }
	    //Upload The FILE to S3 Bucket
	    putS3Object(s3, bucket, filename, filePath);
	    
	    //Sending a message containing the name of the bucket and file to the queue
	    sendMessage(sqs,queueUrl, data);
	}
	
	public static void sendMessage(SqsClient sqsClient,String queueUrl, String msg) {
		System.out.println("\nSend message");

        try {
            sqsClient.sendMessage(SendMessageRequest.builder()
                .queueUrl(queueUrl)
                .messageBody(msg)
                .delaySeconds(2)
                .build());
            System.out.println("\nmessage has been sent");
        } catch (SqsException e) {
            System.err.println(e.awsErrorDetails().errorMessage());
            System.exit(1);
        }
	}
	
	public static void createBucket( S3Client s3Client, String bucketName) {

        try {
            S3Waiter s3Waiter = s3Client.waiter();
            CreateBucketRequest bucketRequest = CreateBucketRequest.builder()
                    .bucket(bucketName)
                    .build();

            s3Client.createBucket(bucketRequest);
            HeadBucketRequest bucketRequestWait = HeadBucketRequest.builder()
                    .bucket(bucketName)
                    .build();

            // Wait until the bucket is created and print out the response.
            WaiterResponse<HeadBucketResponse> waiterResponse = s3Waiter.waitUntilBucketExists(bucketRequestWait);
            waiterResponse.matched().response().ifPresent(System.out::println);
            System.out.println("\n\nThe bucket " + bucketName + " is ready");

        } catch (S3Exception e) {
            System.err.println(e.awsErrorDetails().errorMessage());
            System.exit(1);
        }
    }
	
	public static String createQueue(SqsClient sqsClient,String queueName ) {

        try {
            System.out.println("\nCreate Queue");
            CreateQueueRequest createQueueRequest = CreateQueueRequest.builder()
                .queueName(queueName)
                .build();

            sqsClient.createQueue(createQueueRequest);

            System.out.println("\nGet queue url");
            GetQueueUrlResponse getQueueUrlResponse =
                sqsClient.getQueueUrl(GetQueueUrlRequest.builder().queueName(queueName).build());
            String queueUrl = getQueueUrlResponse.queueUrl();
            System.out.println("\nqueueUrl is = " + queueUrl);
            return queueUrl;
        } catch (SqsException e){
            System.err.println(e.awsErrorDetails().errorMessage());
            System.exit(1);
        }
        return "";
    }
	
	public static String putS3Object(S3Client s3,
								     String bucketName,
								     String filename,
								     String objectPath) {

		try {
			Map<String, String> metadata = new HashMap<>();
			metadata.put("cloud_computing", "lab2");
			
			PutObjectRequest putOb = PutObjectRequest.builder()
													.bucket(bucketName)
													.key(filename)
													.metadata(metadata)
													.build();
			
			PutObjectResponse response = s3.putObject(putOb,
			RequestBody.fromBytes(getObjectFile(objectPath)));
			//System.out.println(response);
			System.out.println("\n"+filename+" has been Uploaded to "+ bucketName);
			return response.eTag();
		
		} catch (S3Exception e) {
			System.err.println(e.getMessage());
			System.exit(1);
		}
		return "";
	}

	// Return a byte array
	private static byte[] getObjectFile(String filePath) {
	
		FileInputStream fileInputStream = null;
		byte[] bytesArray = null;
		
		try {
			File file = new File(filePath);
			bytesArray = new byte[(int) file.length()];
			fileInputStream = new FileInputStream(file);
			fileInputStream.read(bytesArray);
			
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
			if (fileInputStream != null) {
				try {
					fileInputStream.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return bytesArray;
	}
	
	public static Boolean S3bucketExist(S3Client s3, String bucketName) {
		HeadBucketRequest headBucketRequest = HeadBucketRequest.builder()
	            .bucket(bucketName)
	            .build();

	    try {
	        s3.headBucket(headBucketRequest);
	        return true;
	    } catch (NoSuchBucketException e) {
	        return false;
	    }
	}

}
