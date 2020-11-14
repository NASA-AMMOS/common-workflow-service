package jpl.cws.partner.finding.custom;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class SinglePartnerFinder implements S3PartnerFinder {


    @Override
    public void initialize(Map<String, Object> context) {
        System.out.println("initializing...");
    }

    /**
     * Used for getting no partners
     */
    @Override
    public List<String> getInputPartners(String s3Object, String s3BucketName) {
        return new ArrayList<>();
    }

}
