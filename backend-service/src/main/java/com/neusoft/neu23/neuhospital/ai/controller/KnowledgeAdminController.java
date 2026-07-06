package com.neusoft.neu23.neuhospital.ai.controller;

import com.neusoft.neu23.neuhospital.ai.application.rag.KnowledgeAdminService;
import com.neusoft.neu23.neuhospital.ai.application.rag.KnowledgeDocumentUploadCommand;
import com.neusoft.neu23.neuhospital.ai.dto.KnowledgeDocumentResp;
import com.neusoft.neu23.neuhospital.auth.security.SecurityUtils;
import com.neusoft.neu23.neuhospital.common.response.Result;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/admin/ai/knowledge/documents")
@PreAuthorize("hasAnyRole('ADMIN','MANAGEMENT')")
public class KnowledgeAdminController {

    private final KnowledgeAdminService knowledgeAdminService;

    public KnowledgeAdminController(KnowledgeAdminService knowledgeAdminService) {
        this.knowledgeAdminService = knowledgeAdminService;
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<KnowledgeDocumentResp> uploadDocument(@RequestParam("file") MultipartFile file,
                                                        @RequestParam("title") String title,
                                                        @RequestParam("knowledgeType") String knowledgeType,
                                                        @RequestParam(value = "departmentId", required = false) Long departmentId,
                                                        @RequestParam(value = "tags", required = false) String tags,
                                                        @RequestParam(value = "publishNow", required = false, defaultValue = "false") boolean publishNow) {
        Long operatorId = SecurityUtils.getCurrentUserId();
        return Result.success(KnowledgeDocumentResp.from(
                knowledgeAdminService.createDocumentFromUpload(
                        file,
                        new KnowledgeDocumentUploadCommand(title, knowledgeType, departmentId, tags, publishNow),
                        operatorId
                )
        ));
    }

    @PostMapping("/{documentId}/publish")
    public Result<Void> publishDocument(@PathVariable("documentId") Long documentId) {
        knowledgeAdminService.publishDocument(documentId, SecurityUtils.getCurrentUserId());
        return Result.success();
    }

    @PostMapping("/{documentId}/offline")
    public Result<Void> offlineDocument(@PathVariable("documentId") Long documentId) {
        knowledgeAdminService.offlineDocument(documentId, SecurityUtils.getCurrentUserId());
        return Result.success();
    }

    @PostMapping("/{documentId}/reindex")
    public Result<Integer> reindexDocument(@PathVariable("documentId") Long documentId) {
        return Result.success(knowledgeAdminService.reindexDocument(documentId, SecurityUtils.getCurrentUserId()));
    }
}
