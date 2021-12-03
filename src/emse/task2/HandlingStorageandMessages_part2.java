package emse.task2;


import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.DeleteQueueRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.QueueDoesNotExistException;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.SqsException;


public class HandlingStorageandMessages_part2 {
	
	public static void main(String[] args) {
		
		//The default Region in my case, feel free to change it 
	    Region region = Region.EU_CENTRAL_1;
	    
	    final String HOWTOUSE = "\n" +
                "Usage:\n" +
                "   Set in the Code  <queueName>\n" +
                "   Set in the Code  <queueUrl>\n" +
                "Where:\n" +
                "   queueName - Queue name used in the part1 of this program (emse.task2 -> _Part1)\n" +
                "   queueUrl - Get the Url from the console when you Run Part1 of this program (emse.task2 -> _Part1)\n";
	    
	    String queueName = "queue_hamzaAitBaali";
	    String queueUrl = "https://sqs.eu-central-1.amazonaws.com/917047015641/queue_hamzaAitBaali";
	    
	    //Leave them Empty (they are here as an Init)
	    String bucket = "";
	    String filename = "";
	    
	    if (queueName.length() == 0 || queueUrl.length() == 0) {
	        System.out.println(HOWTOUSE);
	        System.exit(1);
   		}
	    
	    final S3Client s3 = S3Client.builder().region(region).build();
	    final SqsClient sqs = SqsClient.builder().region(region).build();
	    
	    //Check Existence of the Queue
	    if(QueueExist(sqs, queueName)){
	    	//Get Messages From the Queue
		    List<Message> messages = receiveMessages(sqs, queueUrl);
		    for (Message msg : messages) {
		    	String[] parts = msg.body().split(";");
		    	bucket = parts[0];
		    	filename = parts[1];
	        	System.out.println("\nbucket is " + bucket + " and FileName is " + filename);
	        	
	        	//Delete Message of the Queue
	        	deleteMessage(sqs, queueUrl, msg);
	        }
		    //Delete SQS Queue
		    deleteSQSQueue(sqs,queueName);
		    
		    //Check Existence of the S3 Bucket
		    if(S3bucketExist(s3, bucket)) {
		    	//Get S3 Object
			    byte[] data = GetS3Object(s3, bucket, filename);
			    
			    //Read Data from the S3 Object
			    ArrayList<Integer> dataInt =  BytesToList(data);
			    
			    //Display MIN, MAX, and SUM
			    findMin(dataInt);
			    findMax(dataInt);
			    CalculateSum(dataInt);
			    
			    //Delete the file and bucket from the Amazon S3
			    emptyAndDeleteBucket(s3, bucket);
		    }else {
		    	System.out.println("\nBucket " + bucket + " Does not Exist " 
		    						+ "\nPlease check again the Name of the bucket"
		    						+ "or create the bucket and upload the File to it");
		    }
	    }else {
	    	System.out.println("\nThe Queue " + queueName + " Does not Exist " 
					+ "\nPlease check again the Name of the Queue"
					+ "or create the queue and send the message in it");
	    }
	}
	

	public static  List<Message> receiveMessages(SqsClient sqsClient, String queueUrl) {
		
        System.out.println("\nReceive messages From :" + queueUrl);

        try {
            ReceiveMessageRequest receiveMessageRequest = ReceiveMessageRequest.builder()
                .queueUrl(queueUrl)
                .maxNumberOfMessages(5)
                .build();
            List<Message> messages = sqsClient.receiveMessage(receiveMessageRequest).messages();
            return messages;
        } catch (SqsException e) {
            System.err.println(e.awsErrorDetails().errorMessage());
            System.exit(1);
        }
        return null;
    }
	
	public static void deleteMessage(SqsClient sqsClient, String queueUrl,  Message message) {
        System.out.println("\nDelete Message "+ message.body());

        try {
        	DeleteMessageRequest deleteMessageRequest = DeleteMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .receiptHandle(message.receiptHandle())
                    .build();
                sqsClient.deleteMessage(deleteMessageRequest);
            System.out.println("\nMessage has been deteled");

        } catch (SqsException e) {
            System.err.println(e.awsErrorDetails().errorMessage());
            System.exit(1);
        }
   
	}
	
	public static byte[] GetS3Object(S3Client s3, String bucketName, String filename) {
		try {
			GetObjectRequest objectRequest = GetObjectRequest
                    .builder()
                    .key(filename)
                    .bucket(bucketName)
                    .build();
			
			ResponseBytes<GetObjectResponse> objectBytes = s3.getObjectAsBytes(objectRequest);
            byte[] data = objectBytes.asByteArray();
			return data;	    
		
		} catch (S3Exception e) {
			System.err.println(e.getMessage());
			System.exit(1);
		}
		return null;
	}
	
	public static void findMin(ArrayList<Integer> dataInt) {
		Integer min = Collections.min(dataInt);
	    System.out.println("MIN is: " + min);
	}
	
	public static void findMax(ArrayList<Integer> dataInt) {
		Integer max = Collections.max(dataInt);
	    System.out.println("MAX is: " + max);
	}
	
	public static void CalculateSum(ArrayList<Integer> dataInt) {
		Integer sum = 0;
	    for(int i = 0; i < dataInt.size(); i++)
	    {
	        sum += dataInt.get(i);
	    }
	    System.out.println("SUM is: "+ sum);
	}
	
	public static ArrayList<Integer> BytesToList(byte[] data) {
		String[] listcsvString = new String(data, StandardCharsets.UTF_8).split("\n"); 
		ArrayList<Integer> listcsv  = new ArrayList<Integer> ();
		for(String ele : listcsvString) {
			listcsv.add(Integer.parseInt(ele));
		}
	    System.out.println("\nThis List has "+ listcsv.size() + " Element");
	    return listcsv;
	}
	
	public static void deleteSQSQueue(SqsClient sqsClient, String queueName) {
		System.out.println("\nDelete Queue = "+ queueName);
        try {

            GetQueueUrlRequest getQueueRequest = GetQueueUrlRequest.builder()
                    .queueName(queueName)
                    .build();

            String queueUrl = sqsClient.getQueueUrl(getQueueRequest).queueUrl();

            DeleteQueueRequest deleteQueueRequest = DeleteQueueRequest.builder()
                    .queueUrl(queueUrl)
                    .build();

            sqsClient.deleteQueue(deleteQueueRequest);
            System.out.println("\nQueue Deleted");

        } catch (SqsException e) {
            System.err.println(e.awsErrorDetails().errorMessage());
            System.exit(1);
        }
    }
	
	public static void emptyAndDeleteBucket(S3Client s3, String bucket) {
		System.out.println("\nDeleting Bucket and Objects in it");
		try {
            // To delete a bucket, all the objects in the bucket must be deleted first
            ListObjectsV2Request listObjectsV2Request = ListObjectsV2Request.builder().bucket(bucket).build();
            ListObjectsV2Response listObjectsV2Response;

            do {
                listObjectsV2Response = s3.listObjectsV2(listObjectsV2Request);
                for (S3Object s3Object : listObjectsV2Response.contents()) {
                	System.out.println("\nObject is " + s3Object.key() + " has been deleted");
                    s3.deleteObject(DeleteObjectRequest.builder()
                            .bucket(bucket)
                            .key(s3Object.key())
                            .build());
                }

                listObjectsV2Request = ListObjectsV2Request.builder().bucket(bucket)
                        .continuationToken(listObjectsV2Response.nextContinuationToken())
                        .build();

            } while(listObjectsV2Response.isTruncated());

            DeleteBucketRequest deleteBucketRequest = DeleteBucketRequest.builder().bucket(bucket).build();
            s3.deleteBucket(deleteBucketRequest);
            System.out.println("\nBucket Deleted");

        } catch (S3Exception e) {
            System.err.println(e.awsErrorDetails().errorMessage());
            System.exit(1);
        }
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
	
	public static Boolean QueueExist(SqsClient sqsClient, String queueName) {
        try {
                sqsClient.getQueueUrl(GetQueueUrlRequest.builder().queueName(queueName).build());
            return true;
        } catch (QueueDoesNotExistException e){
            return false;
        }
    }
	
}
