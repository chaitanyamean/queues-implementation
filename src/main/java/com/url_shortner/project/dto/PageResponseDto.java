package com.url_shortner.project.dto;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder

public class PageResponseDto<T> {
    private List<T> content;
    private int pageNo;
    private int pageSize;
    private long totalElements;
    private int totalPages;
    private boolean last;
}
