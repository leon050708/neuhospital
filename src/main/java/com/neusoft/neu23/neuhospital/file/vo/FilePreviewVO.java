package com.neusoft.neu23.neuhospital.file.vo;

public class FilePreviewVO {

    private Long fileId;
    private String previewUrl;
    private Integer expiresInSeconds;

    public Long getFileId() {
        return fileId;
    }

    public void setFileId(Long fileId) {
        this.fileId = fileId;
    }

    public String getPreviewUrl() {
        return previewUrl;
    }

    public void setPreviewUrl(String previewUrl) {
        this.previewUrl = previewUrl;
    }

    public Integer getExpiresInSeconds() {
        return expiresInSeconds;
    }

    public void setExpiresInSeconds(Integer expiresInSeconds) {
        this.expiresInSeconds = expiresInSeconds;
    }
}
