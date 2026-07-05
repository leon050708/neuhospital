package com.neusoft.neu23.neuhospital.common.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "分页数据包装")
public class PageResult<T> {
    @Schema(description = "当前页数据列表")
    private List<T> records;
    @Schema(description = "当前页码", example = "1")
    private Long pageNo;
    @Schema(description = "每页条数", example = "10")
    private Long pageSize;
    @Schema(description = "总记录数", example = "56")
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
