package com.neusoft.neu23.neuhospital.common.response;

import java.util.List;

public class PageResult<T> {
    private List<T> records;
    private Long pageNo;
    private Long pageSize;
    private Long total;

    public PageResult() {
    }

    public PageResult(List<T> records, Long pageNo, Long pageSize, Long total) {
        this.records = records;
        this.pageNo = pageNo;
        this.pageSize = pageSize;
        this.total = total;
    }

    public List<T> getRecords() {
        return records;
    }

    public void setRecords(List<T> records) {
        this.records = records;
    }

    public Long getPageNo() {
        return pageNo;
    }

    public void setPageNo(Long pageNo) {
        this.pageNo = pageNo;
    }

    public Long getPageSize() {
        return pageSize;
    }

    public void setPageSize(Long pageSize) {
        this.pageSize = pageSize;
    }

    public Long getTotal() {
        return total;
    }

    public void setTotal(Long total) {
        this.total = total;
    }
}
