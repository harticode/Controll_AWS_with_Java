package emse.task1;


import java.util.ArrayList;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesResponse;
import software.amazon.awssdk.services.ec2.model.Ec2Exception;
import software.amazon.awssdk.services.ec2.model.Instance;
import software.amazon.awssdk.services.ec2.model.Reservation;
import software.amazon.awssdk.services.ec2.model.StartInstancesRequest;
import software.amazon.awssdk.services.ec2.model.StopInstancesRequest;



public class Task_1_StartStopEC2 {
	
	public static void main(String[] args) {

		final String HOWTOUSE = "\n" +
                "Usage:\n" +
                "   Set in the Code  <instanceId>\n" +
                "Where:\n" +
                "   instanceId - an instance id value that you can obtain from the AWS Console or simply copy it from the console of the CreateInstanceEC2 part. \n" ;
		
		String instanceId = "";//"i-0aa82c781a3e8692b";
        
        if (instanceId.length() == 0) {
	        System.out.println(HOWTOUSE);
	        System.exit(1);
   		}
        
      //The default Region in my case, feel free to change it 
        Region region = Region.EU_CENTRAL_1;
        Ec2Client ec2 = Ec2Client.builder()
                .region(region)
                .build();
        
        String Ec2InstanceState = GetEC2InstanceState(ec2,instanceId);
 
        while(Ec2InstanceState.equals("stopping") 
        		|| Ec2InstanceState.equals("pending")) {
        	System.out.println("The Amazon EC2 Instance state is "+ 
        			Ec2InstanceState +"\n\n"+
        		"Wait a Second or Two\n");
        	Ec2InstanceState = GetEC2InstanceState(ec2,instanceId);
        }
        
        if(Ec2InstanceState.equals("stopped")) {
        	System.out.println("The Amazon EC2 Instance state is "+ Ec2InstanceState);
        	startInstance(ec2, instanceId);
        }else if(Ec2InstanceState.equals("running")) {
        	System.out.println("The Amazon EC2 Instance state is "+ Ec2InstanceState);
        	stopInstance(ec2, instanceId);
        }else {
        	System.out.println("The Amazon EC2 Instance state : "+ Ec2InstanceState);
        }
        
        ec2.close();
    }
	
	public static String GetEC2InstanceState(Ec2Client ec2, String instanceId) {
		
		ArrayList<Instance> listInstances = describeEC2Instances(ec2);
		Instance currentInst = listInstances.stream()
				  .filter(instance -> instanceId.equals(instance.instanceId()))
				  .findAny()
				  .orElse(null);
		if(currentInst != null) {
			return currentInst.state().nameAsString();
		}else {
			return "Does not exist";
		}
	}
	
	public static ArrayList<Instance> describeEC2Instances( Ec2Client ec2){
	       //boolean done = false;
	       String nextToken = null;
	       ArrayList<Instance> instancesInMyAWS = new ArrayList<Instance>(); 

	       try {

	           do {
	               DescribeInstancesRequest request = DescribeInstancesRequest.builder()
	            		   							.maxResults(6)
	            		   							.nextToken(nextToken)
	            		   							.build();
	               DescribeInstancesResponse response = ec2.describeInstances(request);

	               for (Reservation reservation : response.reservations()) {
	                   for (Instance instance : reservation.instances()) {
	                	   instancesInMyAWS.add(instance);
	                       /*System.out.println("Instance Id is " + instance.instanceId());
	                       System.out.println("Image id is "+  instance.imageId());
	                       System.out.println("Instance type is "+  instance.instanceType());
	                       System.out.println("Instance state name is "+  instance.state().name());
	                       System.out.println("monitoring information is "+  instance.monitoring().state());*/
		               }
		           }
	               nextToken = response.nextToken();
	           } while (nextToken != null);
	           
	           
	       } catch (Ec2Exception e) {
	           System.err.println(e.awsErrorDetails().errorMessage());
	           System.exit(1);
	       }
	       return instancesInMyAWS;
	}
	
	public static void startInstance(Ec2Client ec2, String instanceId) {

        StartInstancesRequest request = StartInstancesRequest.builder()
                .instanceIds(instanceId)
                .build();

        ec2.startInstances(request);
        System.out.printf("Successfully started instance %s", instanceId);
    }

    public static void stopInstance(Ec2Client ec2, String instanceId) {

        StopInstancesRequest request = StopInstancesRequest.builder()
                .instanceIds(instanceId)
                .build();

        ec2.stopInstances(request);
        System.out.printf("Successfully stopped instance %s", instanceId);
    }
}
