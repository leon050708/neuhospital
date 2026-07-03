package com.neusoft.neu23.neuhospital.ct.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "minio")
public class MinioProperties {

    private String endpoint;
    private String accessKey;
    private String secretKey;
    private Bucket bucket = new Bucket();

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getAccessKey() {
        return accessKey;
    }

    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public Bucket getBucket() {
        return bucket;
    }

    public void setBucket(Bucket bucket) {
        this.bucket = bucket;
    }

    public static class Bucket {
        private String ctOriginal;
        private String ctProcessed;
        private String ctHeatmap;
        private String report;
        private String knowledgeDocs;

        public String getCtOriginal() {
            return ctOriginal;
        }

        public void setCtOriginal(String ctOriginal) {
            this.ctOriginal = ctOriginal;
        }

        public String getCtProcessed() {
            return ctProcessed;
        }

        public void setCtProcessed(String ctProcessed) {
            this.ctProcessed = ctProcessed;
        }

        public String getCtHeatmap() {
            return ctHeatmap;
        }

        public void setCtHeatmap(String ctHeatmap) {
            this.ctHeatmap = ctHeatmap;
        }

        public String getReport() {
            return report;
        }

        public void setReport(String report) {
            this.report = report;
        }

        public String getKnowledgeDocs() {
            return knowledgeDocs;
        }

        public void setKnowledgeDocs(String knowledgeDocs) {
            this.knowledgeDocs = knowledgeDocs;
        }
    }
}
