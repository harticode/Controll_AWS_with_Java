package emse.task1;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.InstanceType;
import software.amazon.awssdk.services.ec2.model.RunInstancesRequest;
import software.amazon.awssdk.services.ec2.model.RunInstancesResponse;
import software.amazon.awssdk.services.ec2.model.Tag;
import software.amazon.awssdk.services.ec2.model.CreateTagsRequest;
import software.amazon.awssdk.services.ec2.model.Ec2Exception;


public class CreateInstanceEC2 {
	public static void main(String[] args) {

        final String HOWTOUSE = "\n" +
                "Usage:\n" +
                "   Set in the Code  <tagName>\n" +
                "   Set in the Code  <amiId>\n" +
                "Where:\n" +
                "   tagName - an instance name value that you can set to Tag the instance (usless in our case). \n" +
                "   amiId - an Amazon Machine Image (AMI) value that you can obtain from the AWS Console. \n";

        String tagName = "cloudcoursesEC2_hamza";
        String amiId = "ami-0bd99ef9eccfee250";
        
        if (tagName.length() == 0 || amiId.length() == 0) {
	        System.out.println(HOWTOUSE);
	        System.exit(1);
   		}
        
      //The default Region in my case, feel free to change it 
        Region region = Region.EU_CENTRAL_1;
        Ec2Client ec2 = Ec2Client.builder()
                .region(region)
                .build();

        String instanceId = createEC2Instance(ec2,tagName, amiId) ;
        System.out.println("The Amazon EC2 Instance ID is "+instanceId);
        ec2.close();
    }

   public static String createEC2Instance(Ec2Client ec2,String tagName, String amiId ) {
        RunInstancesRequest runRequest = RunInstancesRequest.builder()
                .imageId(amiId)
                .instanceType(InstanceType.T2_MICRO)
                .maxCount(1)
                .minCount(1)
                .build();

        RunInstancesResponse response = ec2.runInstances(runRequest);
        String instanceId = response.instances().get(0).instanceId();

        Tag tag = Tag.builder()
                .key("Name")
                .value(tagName)
                .build();

        CreateTagsRequest tagRequest = CreateTagsRequest.builder()
                .resources(instanceId)
                .tags(tag)
                .build();

        try {
            ec2.createTags(tagRequest);
            System.out.printf(
                    "Successfully started EC2 Instance %s based on AMI %s",
                    instanceId, amiId);

          return instanceId;

        } catch (Ec2Exception e) {
            System.err.println(e.awsErrorDetails().errorMessage());
            System.exit(1);
        }

        return "";
    }
   
   
}
