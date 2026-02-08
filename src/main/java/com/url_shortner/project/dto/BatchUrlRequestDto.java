package com.url_shortner.project.dto;


import lombok.Data;

import java.util.List;

@Data
public class BatchUrlRequestDto {

    private List<String> urls;
}
