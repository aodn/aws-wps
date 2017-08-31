package au.org.emii.geoserver.client;

import au.org.emii.aggregator.exception.AggregationException;

public interface IndexReader {
    public URIList getUriList(String profile, String timeField, String urlField, SubsetParameters subset) throws AggregationException;
}