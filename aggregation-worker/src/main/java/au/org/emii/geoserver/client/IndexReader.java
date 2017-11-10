package au.org.emii.geoserver.client;

import au.org.emii.aggregator.exception.AggregationException;
import au.org.emii.download.DownloadRequest;

import java.util.List;

public interface IndexReader {
    List<DownloadRequest> getDownloadRequestList(String profile, String timeField, String urlField, SubsetParameters subset) throws AggregationException;
}
