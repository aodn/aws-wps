package au.org.aodn.aws.util;

import com.amazonaws.services.batch.AWSBatch;
import com.amazonaws.services.batch.AWSBatchClientBuilder;
import com.amazonaws.services.batch.model.DescribeJobsRequest;
import com.amazonaws.services.batch.model.DescribeJobsResult;
import com.amazonaws.services.batch.model.JobDetail;
import com.amazonaws.services.batch.model.JobStatus;
import com.amazonaws.services.batch.model.JobSummary;
import com.amazonaws.services.batch.model.ListJobsRequest;
import com.amazonaws.services.batch.model.ListJobsResult;
import au.org.aodn.aws.wps.status.QueuePosition;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static java.util.Comparator.comparing;

public class AWSBatchUtil {

    private static Logger LOGGER = LoggerFactory.getLogger(AWSBatchUtil.class);

    //  The DescribeJobs operation has a limit of the number of Job IDs you can pass in one call
    public static final int DESCRIBE_JOBS_ID_NUMBER_LIMIT = 100;
    public static final JobStatus[] waitingQueueStatuses = {JobStatus.SUBMITTED, JobStatus.PENDING, JobStatus.RUNNABLE, JobStatus.STARTING};
    public static final JobStatus[] completedQueueStatuses = {JobStatus.SUCCEEDED, JobStatus.FAILED};
    public static final JobStatus[] runningQueueStatuses = {JobStatus.RUNNING};

    /**
     *
     * @param jobDetail
     * @return
     */
    public static QueuePosition getQueuePosition(JobDetail jobDetail) {

        AWSBatch batchClient = AWSBatchClientBuilder.defaultClient();
        List<JobSummary> allJobs = listJobs(batchClient, jobDetail.getJobQueue(), waitingQueueStatuses);

        LOGGER.info("TOTAL WAITING JOBS : " + allJobs.size());

        int jobIndex = -1;
        JobSummary[] jobSummaries = new JobSummary[allJobs.size()];
        jobSummaries = allJobs.toArray(jobSummaries);
        for (int index = 0; index <= jobSummaries.length - 1; index++) {
            LOGGER.info("Search queue : jobId [" + jobSummaries[index].getJobId() + "], Match? [" + jobSummaries[index].getJobId().equalsIgnoreCase(jobDetail.getJobId()) + "]");

            if (jobSummaries[index].getJobId().equalsIgnoreCase(jobDetail.getJobId())) {
                jobIndex = index;
                LOGGER.info("Found Job at index [" + jobIndex + "] in queue");
            }
        }

        int jobPosition = 0;
        if (jobIndex >= 0) {
            jobPosition = jobIndex + 1;
        }

        return new QueuePosition(jobPosition, allJobs.size());
    }


    public static JobDetail getJobDetail(String jobId) {

        AWSBatch batchClient = AWSBatchClientBuilder.defaultClient();
        if (batchClient != null && jobId != null) {

            try {
                ArrayList<String> jobList = new ArrayList<>();
                jobList.add(jobId);
                return getJobDetails(batchClient, jobList).get(0);

            } catch (Exception ex) {
                LOGGER.error("Unable to retrieve job details for jobId [" + jobId + "]", ex);
            }
        }

        return null;
    }


    public static List<JobDetail> getJobDetails(AWSBatch batchClient, List<String> jobIds) {

        if (batchClient != null && jobIds != null) {
            List<JobDetail> jobDetails = new ArrayList<>();
            try {

                List<String> jobIdList;
                int startIndex = 0;

                //  Make numerous call the DescribeJobs with chunks of Job IDs up to the
                //  AWS limit (100 job ids per call)
                while(startIndex <= jobIds.size() - 1) {

                    int endIndex = Math.min(jobIds.size(), startIndex + DESCRIBE_JOBS_ID_NUMBER_LIMIT);

                    jobIdList = jobIds.subList(startIndex, endIndex);
                    startIndex = endIndex;

                    //  Call DescribeJobs
                    DescribeJobsRequest describeRequest = new DescribeJobsRequest();
                    describeRequest.setJobs(jobIdList);
                    DescribeJobsResult describeResult = batchClient.describeJobs(describeRequest);

                    if (describeResult != null && describeResult.getJobs().size() > 0) {
                        jobDetails.addAll(describeResult.getJobs());
                    }
                }

                return jobDetails;

            } catch (Exception ex) {
                LOGGER.error("Unable to retrieve job details [" + jobIds.toString() + "]", ex);
            }
        }

        return null;
    }



    public static List<JobSummary> listJobs(AWSBatch batchClient, String queueName, JobStatus[] statusList) {

        ArrayList<JobSummary> allJobs = new ArrayList<>();

        for(JobStatus currentStatus : statusList) {
            ListJobsRequest listJobsRequest = new ListJobsRequest();
            listJobsRequest.setJobStatus(currentStatus);
            listJobsRequest.setJobQueue(queueName);

            ListJobsResult listJobsResult = batchClient.listJobs(listJobsRequest);
            LOGGER.info("# " + currentStatus.name() + " jobs: " + listJobsResult.getJobSummaryList().size());
            allJobs.addAll(listJobsResult.getJobSummaryList());
        }

        return allJobs;
    }


    public static List<JobDetail> getJobDetails(AWSBatch batchClient, String queueName, JobStatus[] statusList) {
        List<JobSummary> jobSummaries = listJobs(batchClient, queueName, statusList);

        List<JobDetail> jobDetails = null;
        LOGGER.info("Job summaries: " + jobSummaries.size());
        if(jobSummaries != null && jobSummaries.size() > 0) {

            ArrayList<String> jobIds = new ArrayList<>();
            for (JobSummary summary : jobSummaries) {
                jobIds.add(summary.getJobId());
            }

            jobDetails = AWSBatchUtil.getJobDetails(batchClient, jobIds);

            return sortByTimestampDescending(jobDetails);
        }

        return null;
    }


    private static List<JobDetail> sortByTimestampDescending(List<JobDetail> jobList) {
        Comparator createdAtDescendingComparator = comparing(JobDetail::getCreatedAt).reversed();
        Collections.sort(jobList, createdAtDescendingComparator);

        return jobList;
    }
}
