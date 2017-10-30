package au.org.aodn.aws.util;

import au.org.aodn.aws.wps.status.QueuePosition;
import com.amazonaws.services.batch.AWSBatch;
import com.amazonaws.services.batch.model.DescribeJobsRequest;
import com.amazonaws.services.batch.model.DescribeJobsResult;
import com.amazonaws.services.batch.model.JobDetail;
import com.amazonaws.services.batch.model.JobStatus;
import com.amazonaws.services.batch.model.JobSummary;
import com.amazonaws.services.batch.model.ListJobsRequest;
import com.amazonaws.services.batch.model.ListJobsResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

public class AWSBatchUtil {

    private static Logger LOGGER = LoggerFactory.getLogger(AWSBatchUtil.class);
    /**
     *
     * @param batchClient
     * @param jobDetail
     * @return
     */
    public static QueuePosition getQueuePosition(AWSBatch batchClient, JobDetail jobDetail) {

        ArrayList<JobSummary> allJobs = new ArrayList<>();

        ListJobsRequest submittedJobsRequest = new ListJobsRequest();
        submittedJobsRequest.setJobStatus(JobStatus.SUBMITTED);
        submittedJobsRequest.setJobQueue(jobDetail.getJobQueue());

        ListJobsResult submittedJobsResult = batchClient.listJobs(submittedJobsRequest);
        LOGGER.info("# SUBMITTED jobs: " + submittedJobsResult.getJobSummaryList().size());
        allJobs.addAll(submittedJobsResult.getJobSummaryList());


        ListJobsRequest pendingJobsRequest = new ListJobsRequest();
        pendingJobsRequest.setJobStatus(JobStatus.PENDING);
        pendingJobsRequest.setJobQueue(jobDetail.getJobQueue());

        ListJobsResult pendingJobsResult = batchClient.listJobs(pendingJobsRequest);
        LOGGER.info("# PENDING jobs: " + pendingJobsResult.getJobSummaryList().size());
        allJobs.addAll(pendingJobsResult.getJobSummaryList());


        ListJobsRequest runnableJobsRequest = new ListJobsRequest();
        runnableJobsRequest.setJobStatus(JobStatus.RUNNABLE);
        runnableJobsRequest.setJobQueue(jobDetail.getJobQueue());

        ListJobsResult runnableJobsResult = batchClient.listJobs(runnableJobsRequest);
        LOGGER.info("# RUNNABLE jobs: " + runnableJobsResult.getJobSummaryList().size());
        allJobs.addAll(runnableJobsResult.getJobSummaryList());

        ListJobsRequest startingJobsRequest = new ListJobsRequest();
        startingJobsRequest.setJobStatus(JobStatus.STARTING);
        startingJobsRequest.setJobQueue(jobDetail.getJobQueue());

        ListJobsResult startingJobsResult = batchClient.listJobs(startingJobsRequest);
        LOGGER.info("# STARTING jobs: " + startingJobsResult.getJobSummaryList().size());
        allJobs.addAll(startingJobsResult.getJobSummaryList());

        LOGGER.info("TOTAL JOBS : " + allJobs.size());

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


    public static JobDetail getJobDetail(AWSBatch batchClient, String jobId) {

        if (batchClient != null && jobId != null) {


            try {
                DescribeJobsRequest describeRequest = new DescribeJobsRequest();
                ArrayList<String> jobList = new ArrayList<>();
                jobList.add(jobId);
                describeRequest.setJobs(jobList);

                DescribeJobsResult describeResult = batchClient.describeJobs(describeRequest);

                if (describeResult != null && describeResult.getJobs().size() > 0) {
                    return describeResult.getJobs().get(0);
                }
            } catch (Exception ex) {
                LOGGER.error("Unable to determine the queue for jobId [" + jobId + "]", ex);
            }
        }

        return null;
    }
}
