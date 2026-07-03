package com.neusoft.neu23.neuhospital.ct.vo;

import java.util.Map;

public class B1B2InferenceResult {

    private String analysisType;
    private String inputPath;
    private String seriesDir;
    private String predictedCategory;
    private Double confidence;
    private Map<String, Double> classProbabilities;
    private String riskLevel;
    private String modelName;
    private String device;
    private Integer numSlices;
    private Integer imageSize;

    public String getAnalysisType() {
        return analysisType;
    }

    public void setAnalysisType(String analysisType) {
        this.analysisType = analysisType;
    }

    public String getInputPath() {
        return inputPath;
    }

    public void setInputPath(String inputPath) {
        this.inputPath = inputPath;
    }

    public String getSeriesDir() {
        return seriesDir;
    }

    public void setSeriesDir(String seriesDir) {
        this.seriesDir = seriesDir;
    }

    public String getPredictedCategory() {
        return predictedCategory;
    }

    public void setPredictedCategory(String predictedCategory) {
        this.predictedCategory = predictedCategory;
    }

    public Double getConfidence() {
        return confidence;
    }

    public void setConfidence(Double confidence) {
        this.confidence = confidence;
    }

    public Map<String, Double> getClassProbabilities() {
        return classProbabilities;
    }

    public void setClassProbabilities(Map<String, Double> classProbabilities) {
        this.classProbabilities = classProbabilities;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public String getDevice() {
        return device;
    }

    public void setDevice(String device) {
        this.device = device;
    }

    public Integer getNumSlices() {
        return numSlices;
    }

    public void setNumSlices(Integer numSlices) {
        this.numSlices = numSlices;
    }

    public Integer getImageSize() {
        return imageSize;
    }

    public void setImageSize(Integer imageSize) {
        this.imageSize = imageSize;
    }
}
